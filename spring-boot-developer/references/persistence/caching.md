# Caching

Start with Spring's cache **abstraction**, not with a Redis client. `@Cacheable` is store-agnostic: simple tests can use
an in-process map, while integration tests and production can use Redis. Swapping the store is a dependency change.

Before adding a cache, confirm the read is actually hot and actually slow. A cache adds an invalidation problem, a
consistency window and a new failure mode. An index is often the real answer.

## Enable it

```java

@Configuration
@EnableCaching
class CacheConfig {
}
```

Without `@EnableCaching`, every `@Cacheable` silently does nothing - the method just runs. There is no warning.

## The annotations

```java

@Service
@Transactional(readOnly = true)
public class ProductService {

    @Cacheable(cacheNames = "products", key = "#id")
    public ProductResponse getById(ProductId id) {
        return ProductResponse.from(productRepository.getById(id));
    }

    @CachePut(cacheNames = "products", key = "#result.id()")
    @Transactional
    public ProductResponse rename(ProductId id, String name) {
        var product = productRepository.getById(id);
        product.rename(name);
        return ProductResponse.from(product);
    }

    @CacheEvict(cacheNames = "products", key = "#id")
    @Transactional
    public void delete(ProductId id) {
        productRepository.deleteById(id);
    }
}
```

| Annotation    | Behaviour                                                                         |
|---------------|-----------------------------------------------------------------------------------|
| `@Cacheable`  | Return the cached value if present, otherwise run the method and cache the result |
| `@CachePut`   | Always run the method, then overwrite the cache entry                             |
| `@CacheEvict` | Remove the entry                                                                  |
| `@Caching`    | Combine several of the above on one method                                        |

`@Cacheable` and `@CachePut` on the same method is a contradiction - the first tries to skip the call, the second
insists on it.

**Cache DTOs, never entities.** A cached entity is detached, carries uninitialised lazy proxies that will throw when
touched, and pins a graph of objects in memory. Every example above caches a `ProductResponse`.

Caching is proxy-based, so a self-invoked call is never cached -
see [spring-proxies-and-di.md](../architecture/spring-proxies-and-di.md).

### Cache and transaction ordering

`@CacheEvict` may run when the method returns, **before** the transaction commits. If commit fails, the cache is
needlessly empty; the next read usually repopulates it. `@CachePut` is more dangerous: it can publish a value that a
later rollback discards. For ordinary cache-aside data, update the database first and invalidate or refresh only after
commit:

```java

@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void onProductChanged(ProductChanged event) {
    cacheManager.getCache("products").evict(event.productId());
}
```

See [transactions.md](transactions.md).

An in-process `@TransactionalEventListener(AFTER_COMMIT)` avoids invalidating on rollback, but it is not durable: a
process crash after commit can lose the event. If invalidation must survive crashes, write an outbox record in the
database transaction and have a worker invalidate or refresh the cache. The database and Redis do not share a normal
atomic transaction; make repeated invalidations idempotent.

### Consistency promise and invalidation

Treat a cache as a derived copy; the database remains authoritative unless the architecture explicitly makes another
store the source of truth. State the acceptable stale-read window for each data set. TTL provides bounded staleness,
not immediate consistency. Authorization, balances, inventory reservations and other correctness-critical decisions
should read authoritative state or use stronger coordination.

For multiple application instances, invalidation must reach every cache instance. A local cache plus single-instance
eviction is not coherent across nodes; use shared storage or a cross-instance invalidation mechanism. Include every
dimension that changes visibility or representation in the key, such as tenant, locale or authorization scope. Never
let a cache hit bypass object-level authorization. Avoid caching permission decisions or sensitive data unless the
invalidation and revocation window is explicit and tested.

Invalidation has a race: a reader can load old database state, a writer can commit and evict, then the reader can write
the old value back into the cache. If stale resurrection matters, use versioned entries, compare-and-set, a short
acceptable TTL, or serialize refreshes for that key. Choose based on measured impact; distributed locks add their own
failure modes. If an outbox drives invalidation, claim work with a lease or database locking strategy, make events
idempotent, and retain enough version information to reject an older refresh after a newer write. Monitor the age of
the oldest pending invalidation, not only queue depth.

## Keys

The default key is derived from all the method parameters, which is fragile - adding a parameter silently changes every
key and invalidates the whole cache.

**Always set `key` explicitly.** Use SpEL against parameter names:

```java
@Cacheable(cacheNames = "orders-by-customer", key = "#customerId.value()")
@Cacheable(cacheNames = "products", key = "#id.value() + ':' + #locale")
```

Keys must be stable and short. A key derived from a value object needs a stable `toString()`/accessor, not the record's
default `toString()` which includes the component name.

For Redis, name caches so the keyspace is browsable and one application cannot collide with another:

```
{app}:{cache}:{id}          orders:product:9f2c8b1e-…
{app}:{cache}:{filter}      orders:product-list:status:ACTIVE
```

## TTL - always

An entry with no expiry is a memory leak with a consistency bug attached. Set a default and override per cache:

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 10m
      cache-null-values: false
      key-prefix: "orders:"
      use-key-prefix: true
```

`cache-null-values: false` stops a miss being cached as a `null`. The trade-off is real: caching negatives protects the
database from a hammering on a non-existent key. Cache them deliberately, with a *short* TTL, only when that attack or
pattern is a genuine concern.

## Redis

```xml

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

Boot auto-configures a `RedisCacheManager` when Redis is on the classpath and `spring.cache.type=redis`. Configure
per-cache TTLs only when the defaults are not enough:

```java

@Bean
RedisCacheManagerBuilderCustomizer cacheCustomizer() {
    return builder -> builder
            .withCacheConfiguration("products",
                    RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("order-summaries",
                    RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(2)));
}
```

Use the customizer rather than declaring your own `RedisCacheManager` bean - replacing the bean discards Boot's
configuration, exactly as with the JSON mapper ([json-and-jackson.md](../web/json-and-jackson.md)).

### Serialization

**Never use Java serialization** - the default for a raw `RedisTemplate<String, Object>`. It is unreadable, brittle
across deployments, and a deserialisation gadget vector. Use JSON.

Deserialising back to the real type requires type information in the payload. Scope it to your own packages and never
enable it globally:

```java
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

@Bean
RedisCacheConfiguration cacheConfiguration() {
    var validator = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.example.app.")
            .allowIfSubType("java.util.")
            .build();

    var serializer = GenericJacksonJsonRedisSerializer.builder()
            .enableDefaultTyping(validator)
            .build();

    return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .disableCachingNullValues()
            .serializeValuesWith(SerializationPair.fromSerializer(serializer));
}
```

Without default typing, a cached object comes back as a `LinkedHashMap` and throws `ClassCastException` on first use.
With *unrestricted* default typing, you have a remote code execution vector. The validator is the point.

On Boot 4 this is `GenericJacksonJsonRedisSerializer` (Jackson 3).

A simpler alternative that avoids the whole question: serialise each cache to a **known type** with
`JacksonJsonRedisSerializer<>(ProductResponse.class)` per cache. More configuration, no polymorphic typing, no
validator to get wrong. `Jackson2JsonRedisSerializer` is the Jackson 2 class.

### Connection settings

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: 2s
      connect-timeout: 1s
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
```

Set timeouts. A Redis instance that stops responding without a timeout turns a cache into an outage - every request
blocks on it.

Decide what happens when Redis is down. By default a cache failure propagates as an exception and the request fails,
which is usually the wrong answer for a cache. For a non-authoritative cache, a `CacheErrorHandler` can let reads fall
through to the source, ignore failed cache writes, and log failed evictions. Suppressing eviction errors accepts stale
reads until the entry expires, so keep the TTL bounded. If that consistency window is unacceptable, propagate eviction
failures or use a design with explicit after-commit invalidation and repair.

```java

@Bean
CacheErrorHandler cacheErrorHandler() {
    return new CacheErrorHandler() {
        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            logCacheFailure("read", cache, exception);
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            logCacheFailure("write", cache, exception);
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            logCacheFailure("eviction", cache, exception);
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            logCacheFailure("clear", cache, exception);
        }
    };
}

private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);

private static void logCacheFailure(String operation, Cache cache, RuntimeException exception) {
    LOGGER.warn("Cache {} failed for cache '{}'; continuing without the cache.",
            operation, cache.getName(), exception);
}
```

Do not log cache keys or values by default; they can contain identifiers or sensitive data. Alert on sustained cache
failures separately from request failures so a fallback does not hide an unhealthy Redis service.

### Local Redis with Docker Compose

Keep the Redis Compose service and local startup instructions in the [Docker and Compose guide](../deployment/docker.md#redis-cache-with-compose). A cache does not need a named volume: Redis data is derived and can be rebuilt from the source of truth. Use a persistence policy only when Redis stores data the system cannot reconstruct.

For integration tests, use a Redis Testcontainer with `@ServiceConnection(name = "redis")`; disable Docker Compose
startup in the test context so the test owns its container lifecycle. This exercises Redis serialization, TTL, and
invalidation without requiring a developer's local Redis instance. See [Testcontainers](../testing/testcontainers.md).

## Stampede

When a hot key expires, every concurrent request misses simultaneously and all of them recompute the same value.

```java

@Cacheable(cacheNames = "products", key = "#id.value()", sync = true)
public ProductResponse getById(ProductId id) { …}
```

`sync = true` lets one caller compute while the others wait - **per instance**. Across a fleet you still get one
recomputation per instance; for a genuinely expensive value, add a short distributed lock (`SET key value NX EX 30`)
around the recompute. A bounded stale-while-revalidate policy is another option when serving a slightly stale value is
acceptable. Do not let every request hit the database after a cache outage; bound fallback load and define whether to
fail fast or shed load. Negative caching can protect a hot miss, but use a short TTL and consider whether a newly
created resource must become visible immediately.

Add jitter to TTLs so a batch of keys written together does not expire on the same second.

## Testing consistency behavior

Test commit and rollback behavior, multiple cache instances or the chosen invalidation transport, stale-reader races
when relevant, and cache-outage fallback. Use the real cache implementation in at least one integration test when
serialization, TTL or pub/sub behavior is part of the guarantee. Testcontainers can provide Redis; see
[Testcontainers](../testing/testcontainers.md) and [transactions](transactions.md).

## Local caching

For data that is small, read-mostly and tolerant of a few seconds of staleness, an in-process cache beats a network
round trip:

```xml

<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m
```

Each instance holds its own copy, so evictions are not shared - a change on one instance is invisible to the others
until their entries expire. Acceptable for reference data, not for anything a user just edited.

Because the abstraction is the same, this is a configuration change and not a code change. That is the argument for
using `@Cacheable` rather than a Redis client directly.

## HTTP caching

Sometimes the right cache is the client's. An `ETag` plus a 304 avoids serialising and transferring the body entirely:

```java

@Bean
FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
    return new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
}
```

It still executes the handler, so it saves bandwidth rather than work.
`ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))` on genuinely public, stable resources
saves both.

## Gotchas

- Agent omits `@EnableCaching` - every `@Cacheable` silently does nothing
- Agent caches an entity - detached, with lazy proxies that throw when touched; cache a DTO
- Agent relies on the default cache key - adding a parameter silently invalidates everything; set `key` explicitly
- Agent sets no TTL - a memory leak plus permanently stale data
- Agent uses Java serialization for Redis values - brittle and a deserialisation gadget vector; use JSON
- Agent enables Jackson default typing without a `PolymorphicTypeValidator` - remote code execution vector
- Agent omits default typing entirely - cached values return as `LinkedHashMap` and throw `ClassCastException`
- Agent uses `GenericJackson2JsonRedisSerializer` or `Jackson2JsonRedisSerializer` on Boot 4 - those are the Jackson 2 classes; use `GenericJacksonJsonRedisSerializer` or `JacksonJsonRedisSerializer`
- Agent declares its own `RedisCacheManager` bean - use `RedisCacheManagerBuilderCustomizer` or Boot's defaults are lost
- Agent calls a `@Cacheable` method on `this` - proxy bypassed, nothing is cached
- Agent puts `@Cacheable` and `@CachePut` on the same method - they contradict each other
- Agent evicts inside the transaction where correctness matters - evict after commit
- Agent assumes an `AFTER_COMMIT` listener is durable - use an outbox when invalidation must survive a process crash
- Agent ignores a concurrent stale reader repopulating an evicted key - choose a race strategy if stale resurrection violates the requirement
- Agent assumes local cache evictions reach every instance - use shared storage or cross-instance invalidation
- Agent treats TTL as immediate consistency - state and test the actual staleness window
- Agent sets no Redis timeout - an unresponsive cache becomes a full outage
- Agent lets a Redis failure fail the request - add a `CacheErrorHandler` that falls through to the source
- Agent lets a Redis outage send every request to the database - bound fallback load and define a failure policy
- Agent leaves a hot key unprotected - use `sync = true`, plus a distributed lock if the recompute is expensive
- Agent gives every entry an identical TTL - add jitter so they do not expire together
- Agent uses Caffeine for data that must be consistent across instances - local caches do not share evictions

## Related

- [transactions.md](transactions.md) · [messaging.md](../integration/messaging.md) · [resilience.md](../integration/resilience.md) · [spring-proxies-and-di.md](../architecture/spring-proxies-and-di.md) · [json-and-jackson.md](../web/json-and-jackson.md) · [observability.md](../observability/observability.md) · [configuration.md](../configuration/configuration.md)

## References

- [Spring Boot: Caching](https://docs.spring.io/spring-boot/reference/io/caching.html)
- [Spring Data Redis Reference](https://docs.spring.io/spring-data/redis/reference/)
