# Caching

Read this when adding or changing application caching. This skill uses Redis
with Spring's cache abstraction. Redis server operations such as clustering
or tuning are outside this reference.

## When to use

- Add `@Cacheable`, `@CachePut`, `@CacheEvict`, or a `CacheManager`
- Choose cache keys, expiration, or invalidation behavior

## When not to use

- A cache is being used as the source of truth or durable storage
- Redis clustering, eviction tuning, or failure diagnosis
- HTTP response caching controlled by HTTP headers

## Rules

1. Add caching only for measured or clear repeated work with an acceptable
   staleness window. State what is cached, its key, expiration policy, and how
   writes invalidate or refresh it.
2. Use Spring's cache abstraction backed by Redis. Add
   `spring-boot-starter-cache` and `spring-boot-starter-data-redis`; let Spring
   Boot manage their versions. Enable caching with `@EnableCaching` on a
   purpose-specific `@Configuration` class in the root `config` package. Keep
   the application bootstrap class limited to `@SpringBootApplication`. Use
   Boot's Redis cache manager. Do not introduce a second cache abstraction.
   Read [docker.md](docker.md) for the local Redis service.
3. Make keys deterministic, bounded, and derived from every input that can
   change the result. Prefer named key components over concatenating
   unescaped values. Include tenant or authorization scope when results are
   scoped; never share user-specific data under a global key.
4. Set an explicit expiration/retention policy for every cache. Do not depend
   on provider defaults. Keep cached values immutable or treat them as
   read-only; do not expose mutable cached collections to callers.
5. Invalidate or update cache entries in the same application operation that
   changes their source data. For transactional writes, defer eviction until
   successful commit. `@CacheEvict`'s default `beforeInvocation = false` means
   after method return, not necessarily after the surrounding database
   transaction commits. Use a transaction-aware cache manager (for example,
   Spring's `TransactionAwareCacheManagerProxy` around Boot's cache manager)
   or register invalidation with transaction synchronization's `afterCommit`.
   Handle bulk changes and delete paths as well as single-record updates.
6. Do not cache failures, authorization decisions, or highly volatile values
   unless the domain explicitly defines their staleness and invalidation
   behavior. Avoid caching `null` by default; negative caching needs a short,
   explicit expiry.
7. Use Redis for cache entries so application instances share the same cache.
   Treat Redis as disposable cache storage, not as the authority for
   persisted business data. Do not enable Redis persistence for cache-only
   deployments unless the application has a separate durability requirement.
8. Configure a `RedisSerializer` for cached DTO values. Spring Data Redis
   defaults to JDK serialization, which couples cache entries to Java class
   serialization. Choose JSON serialization for interoperable DTO caches and
   configure an explicit type policy for the application's value types. Test
   serialization and deserialization against the real Redis container.
9. Test cache behavior that is part of the contract: prove repeated equivalent
   reads use the cache, result-affecting key inputs do not collide, TTL expiry
   or write invalidation works, and a source update is visible after
   invalidation. A test that only reads before and after an update also passes
   when caching is absent, so it does not prove cache behavior by itself. Test
   that rollback does not evict when invalidation is specified to run after
   commit. Keep tests isolated by clearing the relevant cache between cases.
   Use a real Redis service for tests that verify Redis serialization or
   cache-manager integration.

Example cache configuration with an explicit TTL:

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 10m
      cache-null-values: false
      use-key-prefix: true
```

Configure the Redis cache value serializer in the cache manager. Do not
serialize arbitrary polymorphic Java types from untrusted cache contents.

Use per-cache TTLs when caches have different freshness requirements. Keep
Redis connection settings in Spring Boot's `spring.data.redis` configuration;
the Docker Compose integration supplies the local connection details.

The template configures a ten-minute `users` cache with a typed Jackson 3
serializer for `UserResponse`, disables null caching, and uses a
transaction-aware cache manager. Its PostgreSQL/Redis Testcontainers test
proves a direct source update remains hidden behind the cache until the
application service updates the entry and the next read sees the new value:
[cache configuration](../assets/template-maven/src/main/java/com/example/templatemaven/config/CacheConfiguration.java),
[service cache annotations](../assets/template-maven/src/main/java/com/example/templatemaven/user/application/UserService.java),
[PostgreSQL test container](../assets/template-maven/src/test/java/com/example/templatemaven/PostgresTestcontainersConfiguration.java),
[Redis test container](../assets/template-maven/src/test/java/com/example/templatemaven/RedisTestcontainersConfiguration.java),
and [cache integration test](../assets/template-maven/src/test/java/com/example/templatemaven/user/CacheIntegrationTests.java).
The test also rolls back a versioned update and verifies the existing cache
entry remains available, covering the transaction-aware manager's after-commit
invalidation behavior.

## Verify

Inspect the changed cache configuration and call sites. Confirm its key
contains all result-affecting scope, expiration is explicit, and affected
source write paths invalidate after successful commit, including rollback and
delete paths. Confirm tests fail if caching/invalidation is removed. Report
any behavior that depends on a provider default or cannot be verified from
code.
