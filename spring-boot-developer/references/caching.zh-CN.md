# 缓存

优先使用 Spring 的缓存**抽象**，而不是直接依赖 Redis 客户端。`@Cacheable` 与存储实现无关：测试中可以使用进程内 Map，生产环境使用 Redis；更换存储只需要调整依赖和配置。

添加缓存前，先确认读取确实频繁且确实很慢。缓存会带来失效问题、一致性窗口和新的故障模式。真正的解决办法往往是添加索引。

## 启用缓存

```java
@Configuration
@EnableCaching
class CacheConfig {
}
```

没有 `@EnableCaching` 时，所有 `@Cacheable` 都会静默失效——方法仍会执行，而且不会给出警告。

## 缓存注解

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

| 注解 | 行为 |
|---|---|
| `@Cacheable` | 有缓存时返回缓存值；没有时执行方法并缓存结果 |
| `@CachePut` | 始终执行方法，然后覆盖缓存条目 |
| `@CacheEvict` | 删除缓存条目 |
| `@Caching` | 在一个方法上组合多个上述注解 |

在同一个方法上同时使用 `@Cacheable` 和 `@CachePut` 是矛盾的：前者试图跳过方法调用，后者要求方法必须执行。

**缓存 DTO，绝不缓存实体。** 缓存的实体已经脱离持久化上下文，可能带有访问时会报错的未初始化懒加载代理，还会把一整张对象关系图固定在内存中。上面的示例缓存的都是 `ProductResponse`。

缓存通过代理实现，因此同一个对象内部的自调用不会触发缓存；参见[代理与依赖注入](spring-proxies-and-di.zh-CN.md)。

### 缓存与事务顺序

`@CacheEvict` 会在方法返回时执行，**早于**事务提交。如果之后提交失败，缓存就会因为一个实际未发生的变更而被清除。此问题会自行恢复，因为下一次读取会从数据库重新填充缓存。

更危险的是 `@CachePut`：它可能先发布新值，随后事务回滚，导致缓存保存了数据库中不存在的数据。正确性要求较高时，应在事务提交后再清除缓存：

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
void onProductChanged(ProductChanged event) {
    cacheManager.getCache("products").evict(event.productId());
}
```

参见[事务](transactions.zh-CN.md)。

## 缓存键

默认键由方法的所有参数生成，比较脆弱——添加一个参数就会静默改变所有键，使整个缓存失效。

**始终显式设置 `key`。** 使用 SpEL 引用参数名：

```java
@Cacheable(cacheNames = "orders-by-customer", key = "#customerId.value()")
@Cacheable(cacheNames = "products", key = "#id.value() + ':' + #locale")
```

键应稳定且简短。根据值对象生成键时，应使用稳定的 `toString()` 或访问器，不要使用默认的 record `toString()`，因为它会包含组件名称。

使用 Redis 时，应给缓存命名，以便查看键空间，并避免不同应用相互冲突：

```
{app}:{cache}:{id}          orders:product:9f2c8b1e-…
{app}:{cache}:{filter}      orders:product-list:status:ACTIVE
```

## TTL——必须设置

没有过期时间的缓存条目就是内存泄漏，还会造成数据一致性问题。设置默认 TTL，并按缓存单独覆盖：

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

`cache-null-values: false` 会避免把缓存未命中结果保存为 `null`。不过，缓存负结果也有实际价值：它能保护数据库，避免不存在的键被反复查询。只有在确实需要防护此类攻击或访问模式时，才有意缓存负结果，并使用**较短**的 TTL。

## Redis

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

当 Redis 位于 classpath 且设置了 `spring.cache.type=redis` 时，Spring Boot 会自动配置 `RedisCacheManager`。只有默认值不够用时，才配置每个缓存的 TTL：

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

使用定制器，而不是自行声明 `RedisCacheManager` Bean——替换该 Bean 会丢失 Spring Boot 的配置，这与 JSON 映射器的情况相同（参见 [JSON 与 Jackson](json-and-jackson.zh-CN.md)）。

### 序列化

**绝不要使用 Java 序列化**——它是原始 `RedisTemplate<String, Object>` 的默认行为。序列化结果不可读，跨版本部署时容易损坏，而且会引入反序列化 gadget 攻击面。请使用 JSON。

要反序列化为实际类型，载荷中必须包含类型信息。将允许的类型范围限制在自己的包中，绝不要全局启用：

```java
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

不启用默认类型信息时，缓存对象取出后会变成 `LinkedHashMap`，首次使用就会抛出 `ClassCastException`。启用**不受限**的默认类型信息则会形成远程代码执行风险。类型验证器是关键防线。

Spring Boot 4 使用 `GenericJacksonJsonRedisSerializer`（Jackson 3）；3.5.x 使用 `GenericJackson2JsonRedisSerializer`。

更简单的替代方案是按每个缓存的**已知类型**序列化，例如 `Jackson2JsonRedisSerializer<>(ProductResponse.class)`。配置会更多，但不需要多态类型信息，也不需要担心验证器配置错误。

### 连接设置

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

必须设置超时。如果 Redis 停止响应且没有超时，所有请求都会阻塞在缓存上，最终导致服务中断。

应明确 Redis 不可用时的行为。默认情况下，缓存故障会以异常形式传播，导致请求失败；对缓存来说，这通常不是合适的行为。可以提供 `CacheErrorHandler`，记录错误并回退到数据库。

## 缓存击穿

热门键过期时，所有并发请求会同时未命中，并重复计算相同的值。

```java
@Cacheable(cacheNames = "products", key = "#id.value()", sync = true)
public ProductResponse getById(ProductId id) { … }
```

`sync = true` 会让一个调用方负责计算，其他调用方等待——**范围仅限单个实例**。在多实例集群中，每个实例仍会各自重新计算一次。对于计算开销很大的值，可以在重新计算时加一个短时分布式锁（`SET key value NX EX 30`）。

给 TTL 增加随机抖动，避免同一批写入的键在同一秒过期。

## 本地缓存

对于数据量小、以读取为主且能容忍几秒陈旧的数据，进程内缓存比网络往返更快：

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

每个实例都持有自己的副本，因此实例之间不会共享缓存清除操作——某个实例上的变更，在其他实例的条目过期前不可见。这适用于参考数据，不适用于用户刚刚编辑的数据。

缓存抽象保持不变，因此切换为本地缓存只需修改配置，无需改代码。这正是使用 `@Cacheable` 而非直接调用 Redis 客户端的理由。

## HTTP 缓存

有时最合适的缓存属于客户端。使用 `ETag` 和 304 响应，可以完全避免序列化和传输响应体：

```java
@Bean
FilterRegistrationBean<ShallowEtagHeaderFilter> etagFilter() {
    return new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
}
```

这种方式仍然会执行处理器，因此节省的是带宽，而不是计算。对于真正公开且稳定的资源，使用 `ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))` 可以同时节省带宽和计算。

## 使用 Spring Boot 3.5.x 时

缓存抽象、四种注解、`sync = true`、Caffeine 和 Redis 属性都相同。差异在于使用 Jackson 2：应使用 `GenericJackson2JsonRedisSerializer`，而不是 `GenericJacksonJsonRedisSerializer`；使用 `ObjectMapper`，而不是 `JsonMapper`。Spring Session 的键也有变化：3.5.x 使用 `spring.session.redis.*`，到了 4 则改为 `spring.session.data.redis.*`。

## 常见问题

- 忘记添加 `@EnableCaching`——所有 `@Cacheable` 都会静默失效。
- 缓存实体——实体已脱离持久化上下文，懒加载代理在访问时可能报错；应缓存 DTO。
- 依赖默认缓存键——添加参数会静默使所有缓存失效；应显式设置 `key`。
- 没有设置 TTL——这会造成内存泄漏和永久陈旧的数据。
- Redis 值使用 Java 序列化——结果脆弱且有反序列化 gadget 风险；应使用 JSON。
- 启用 Jackson 默认类型信息却没有配置 `PolymorphicTypeValidator`——形成远程代码执行风险。
- 完全不启用默认类型信息——缓存值会以 `LinkedHashMap` 返回并抛出 `ClassCastException`。
- 在 Spring Boot 4 中使用 `GenericJackson2JsonRedisSerializer`——这是 Jackson 2 的类。
- 自行声明 `RedisCacheManager` Bean——应使用 `RedisCacheManagerBuilderCustomizer`，否则 Spring Boot 默认配置会丢失。
- 通过 `this` 调用 `@Cacheable` 方法——代理被绕过，缓存不会生效。
- 同一方法同时添加 `@Cacheable` 和 `@CachePut`——两者行为矛盾。
- 在对正确性要求较高的事务中直接清除缓存——应在事务提交后清除。
- 没有设置 Redis 超时——无响应的缓存会造成整个服务故障。
- Redis 故障时让请求失败——添加 `CacheErrorHandler`，使请求回退到数据源。
- 热门键没有防击穿措施——使用 `sync = true`；重新计算代价高时还应加分布式锁。
- 所有条目使用相同 TTL——添加随机抖动，避免它们同时过期。
- 对要求跨实例一致的数据使用 Caffeine——本地缓存不会在实例之间共享失效操作。

## 相关文档

- [缓存一致性](cache-consistency.zh-CN.md) · [事务](transactions.zh-CN.md) · [代理与依赖注入](spring-proxies-and-di.zh-CN.md) · [JSON 与 Jackson](json-and-jackson.zh-CN.md) · [可观测性](observability.zh-CN.md) · [配置](configuration.zh-CN.md)
