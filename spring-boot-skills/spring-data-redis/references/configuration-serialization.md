# Configuration and serialization

Inspect actual managed dependencies; the examples below target Spring Data Redis 4 with Jackson 3. Add only starters required by the selected role.

## Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

## Configuration

Use [RedisConfig](../templates/RedisConfig.java) as an illustrative imperative cache configuration, not a mandatory replacement of the project's beans. It uses Jackson 3 JSON serialization and explicit Redis 4 writer settings. Compare the
[TTL-aware cache service](../examples/cache-service.java) with the
[cache service without a TTL](../examples/cache-service-no-ttl.java) before adding a new cache.

Default typing is disabled by default. If a generic cache stores heterogeneous DTOs, the template
shows how to enable it with a validator; keep that validator restricted to the concrete cache DTO
types used by the application. Do not allow entire JDK packages such as `java.util` or broad
application packages. Prefer type-specific serializers when a cache has one stable value type.
If the payload uses polymorphic JDK values such as `BigDecimal`, allow only those concrete types
after reviewing the serialized shape; do not solve round-trip errors with a package-wide rule.

## application.yml

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 5
          min-idle: 2
  cache:
    type: redis
```

The YAML assumes a password-authenticated environment. Use the approved deployment secret mechanism, TLS/ACL and explicit connection/command timeout settings; passwordless local disposable Redis requires intentional separate configuration. Lettuce pool settings require the relevant pooling dependency; verify effective settings. Preserve existing reactive/imperative choices and avoid blocking event-loop threads.

Use namespaced, bounded keys including environment/application, tenant where relevant, operation and schema version where needed. Use unambiguous encoding for composite keys; avoid raw credentials or private data in keys. Keep Redis Cluster hash-slot constraints in mind for multi-key atomic operations. Choose data types, serialization, expiry and durability from the role, not a universal cache template.

For sessions use the existing Spring Session integration and the actual Boot version's property names. Boot 4 documents the Redis session starter and `spring.session.data.redis.namespace`; see [Spring Session guide](https://docs.spring.io/spring-session/reference/guides/boot-redis.html). Review cookie/security policy, session expiry, namespace isolation, session serialization and rolling-deployment compatibility. Do not reuse a disposable cache manager for sessions.

Spring Data Redis 4 uses Jackson 3; see [migration guidance](https://docs.spring.io/spring-data/redis/reference/upgrading.html). Generic Jackson 3 default typing is opt-in; a typed serializer avoids unnecessary runtime type metadata. Jackson 3 supports java.time without the old JavaTimeModule pattern. Verify exact DTO/nested value types and reject unexpected polymorphic types. Class-based subtype matching can include subclasses; use final DTO types or a validator with explicit allowed concrete names when an exact allowlist is required. Do not assume a class-based allow rule proves exact-type restriction. Avoid broadening the allowlist just to fix round-trip errors. Preserve existing persisted encodings or design a scoped migration.
