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
   Boot manage their versions. Enable caching with `@EnableCaching` and use
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
   changes their source data. For transactional writes, run eviction after a
   successful commit when supported by the chosen integration. Handle bulk
   changes and delete paths as well as single-record updates.
6. Do not cache failures, authorization decisions, or highly volatile values
   unless the domain explicitly defines their staleness and invalidation
   behavior. Avoid caching `null` by default; negative caching needs a short,
   explicit expiry.
7. Use Redis for cache entries so application instances share the same cache.
   Treat Redis as disposable cache storage, not as the authority for
   persisted business data. Do not enable Redis persistence for cache-only
   deployments unless the application has a separate durability requirement.
8. Test cache behavior that is part of the contract: key separation,
   expiration or invalidation, and source updates. Keep tests isolated by
   clearing the relevant cache between cases. Use a real Redis service for
   tests that verify Redis serialization or cache-manager integration.

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

Use per-cache TTLs when caches have different freshness requirements. Keep
Redis connection settings in Spring Boot's `spring.data.redis` configuration;
the Docker Compose integration supplies the local connection details.

## Verify

For every cache, inspect its configuration and call sites. Confirm the key
contains all result-affecting scope, expiration is explicit, and each source
write path has the intended invalidation/update behavior. Report any behavior
that depends on a provider default or cannot be verified from the code.
