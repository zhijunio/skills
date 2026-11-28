# Caching

These fragments assume an authorized single-scope read model. For tenant- or caller-dependent data, authorize outside the cache interceptor and scope keys by trusted identity and all result dimensions. Do not copy an ID-only key into a multi-tenant or permission-filtered cache.

## Writer semantics and illustrative key names

This imperative example explicitly selects Redis 4 `immediateWrites()` so a successful synchronous writer operation has completed before it returns. This is not a guarantee of replica read-after-write or of immediate transaction-deferred callbacks.
That costs write latency; asynchronous cache writes are a separate, intentional consistency
choice. Transaction-aware callbacks still run after the database commit, not atomically with it.
`SCAN`-based clearing avoids a single `KEYS` traversal; this configuration assumes Lettuce.
See the [Redis 4 writer API](https://docs.spring.io/spring-data/redis/reference/api/java/org/springframework/data/redis/cache/RedisCacheWriter.RedisCacheWriterConfigurer.html).

```
{app}:{domain}:{id}          → orders:order:uuid-here
{app}:{domain}:list:{filter} → orders:order:list:status:PENDING
{app}:session:{sessionId}    → orders:session:opaque-id
{app}:ratelimit:{ip}         → orders:ratelimit:192.168.1.1
```

## @Cacheable — Declarative Caching

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(UUID id) {
        return productRepository.findById(id)
            .map(ProductResponse::from)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    @CachePut(value = "products", key = "#result.id()")  // use a transaction-aware manager for post-commit update
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id).orElseThrow();
        product.update(request);
        return ProductResponse.from(productRepository.save(product));
    }

    @CacheEvict(value = "products", key = "#id")  // invalidate on delete
    @Transactional
    public void delete(UUID id) {
        productRepository.deleteById(id);
    }

    @CacheEvict(value = "products", allEntries = true)  // only an explicitly authorized, scoped administrative action
    public void clearCache() {}
}
```

## Manual Cache-Aside Pattern

```java
@Service
@RequiredArgsConstructor
public class OrderCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JsonMapper jsonMapper; // Jackson 3 — Boot auto-configures a JsonMapper bean
    private static final Duration TTL = Duration.ofMinutes(5);

    public Optional<OrderResponse> get(UUID orderId) {
        String key = "orders:order:" + orderId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached == null) return Optional.empty();
        return Optional.of(jsonMapper.convertValue(cached, OrderResponse.class));
    }

    public void put(OrderResponse order) {
        String key = "orders:order:" + order.id();
        redisTemplate.opsForValue().set(key, order, TTL);
    }

    public void evict(UUID orderId) {
        redisTemplate.delete("orders:order:" + orderId);
    }
}
```

## Cache Stampede

`sync = true` delegates loading coordination to the cache provider. The default Redis writer
in the configuration above is non-locking: do not claim that this annotation alone prevents
duplicate loads, even inside one application instance.

For expensive read-only loads, the optional
[CoalescingRedisCacheManager](../templates/CoalescingRedisCacheManager.java) uses a locking
writer with a finite lock TTL. Register it under a distinct bean name, keeping your normal
cache manager primary, and pass a JSON-serialized `RedisCacheConfiguration`:

```java
@Cacheable(cacheManager = "coalescingCacheManager", value = "products", key = "#id", sync = true)
public ProductResponse findById(UUID id) { ... }
```

Add this bean method to the shown `RedisConfig`, reusing its `jsonSerializer()` helper,
and mark the normal `cacheManager` bean `@Primary` so existing annotations stay unchanged:

```java
@Bean("coalescingCacheManager")
RedisCacheManager coalescingCacheManager(RedisConnectionFactory factory) {
    var config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(5))
        .disableCachingNullValues()
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer()));
    return CoalescingRedisCacheManager.create(factory, config, Duration.ofSeconds(10));
}
```

The writer lock is cache-wide, not per key: unrelated misses may block too. Configure finite
loader/transport timeouts and a lock TTL longer than the expected load, and test contention.
Lock expiry during a stalled load can still permit duplicates; this is load reduction, not
an exactly-once guarantee or protection for payment/order mutations. Redis command timeouts
do not bound the writer's entire lock-wait loop. Use a bounded caller deadline or a different
coalescing strategy when strict waiting bounds are required.

Use `sync` loading only for committed, read-only data; transaction-aware put/evict does not make
the loader-based cache get transactional. Prefer TTL jitter or local coalescing when the
cache-wide lock would reduce throughput excessively. Do not implement a bare `SETNX`/delete
lock without ownership tokens, atomic owner-checked release and a lease-expiry design.

See [Spring Redis cache behavior](https://docs.spring.io/spring-data/redis/reference/redis/redis-cache.html).

Enable caching through the existing configuration when needed; verify proxy interception and self-invocation behavior. Use TTL and negative caching only where the contract permits stale/missing results. Consider TTL jitter for synchronized expirations, not as a universal requirement. Avoid unbounded cache-wide clearing; SCAN is not an atomic snapshot and driver/topology compatibility must be checked. See [consistency and failure policy](consistency-failure-policy.md).
