# 弹性处理

Spring Framework 7 在核心框架中提供声明式重试和并发限制。**这不是重新打包的 Spring Retry**：注解位于不同包，属性名称不同，也没有恢复回调。为 Spring Retry 编写的代码无法直接用于它。

## 使用重试之前

重试应该是第三步，而非第一步：

1. **先设置超时。** 没有超时的调用无法有效重试，而超时缺失是大多数级联故障的实际原因。参见[HTTP 客户端](http-clients.zh-CN.md)。
2. **确保幂等。** 重试非幂等操作会造成重复。先解决幂等问题，否则就不要重试。
3. **最后才重试**，而且只重试暂时性故障。

依赖已经损坏时还持续重试，会在它最无力承受时用流量攻击它。限制尝试次数，使用指数退避并添加随机抖动。

## 启用

```java
@Configuration
@EnableResilientMethods
class ResilienceConfig {
}
```

没有该配置时，注解不会生效，方法只运行一次，且没有警告。

## 重试

```java
import org.springframework.resilience.annotation.Retryable;

@Service
public class InventoryGateway {

    @Retryable(
            includes = { ConnectException.class, InventoryUnavailableException.class },
            maxRetries = 3,
            delay = 200,
            multiplier = 2.0,
            maxDelay = 2000,
            jitter = 100)
    public StockLevel stockFor(ProductId productId) {
        return inventoryClient.stockFor(productId.value());
    }
}
```

| 属性 | 含义 |
|---|---|
| `includes` | 要重试的异常类型。**始终设置**；默认会重试所有异常，包括程序 Bug |
| `excludes` | 绝不重试的类型 |
| `maxRetries` | 首次调用**之后**的重试次数；`3` 表示最多调用 4 次 |
| `delay` | 初始延迟，单位毫秒 |
| `multiplier` | 指数退避倍数；`1.0` 表示固定延迟 |
| `maxDelay` | 退避上限 |
| `jitter` | 随机延迟范围，避免整个集群同步重试 |

有一定规模时，抖动不可省略。否则同一时刻失败的实例会在同一时刻重试，惊群效应会让依赖一直无法恢复。

### 与 Spring Retry 的区别

| Spring Retry | Framework 7 核心功能 |
|---|---|
| `org.springframework.retry.annotation` | `org.springframework.resilience.annotation` |
| `@EnableRetry` | `@EnableResilientMethods` |
| `maxAttempts`（总调用次数） | `maxRetries`（首次之后的重试次数） |
| `retryFor` / `noRetryFor` | `includes` / `excludes` |
| 嵌套 `@Backoff(delay=…, multiplier=…)` | `@Retryable` 上的平铺属性 `delay`、`multiplier`、`maxDelay`、`jitter` |
| `@Recover` 备用方法 | **无**——最终异常传给调用方 |

`maxAttempts = 3` 与 `maxRetries = 3` 不相等：前者总共调用三次，后者总共调用四次。

**没有 `@Recover`。** 重试耗尽后，最后一个异常会向外传播。应在编排边界处理：

```java
public StockLevel stockOrUnknown(ProductId productId) {
    try {
        return inventoryGateway.stockFor(productId);
    } catch (InventoryUnavailableException e) {
        log.warn("Inventory unavailable for {}, degrading", productId, e);
        return StockLevel.unknown();
    }
}
```

有意降级通常优于使整个请求失败。应在每个调用位置决定能否接受过期或缺失的数据。

## 重试与事务

**绝不要在同一个方法上同时使用 `@Retryable` 和 `@Transactional`。** 第一次失败就会将事务标记为只能回滚；此后的重试仍在该事务中，最后提交时全部失败。

将重试放在调用事务方法的另一个 Bean 上，让每次尝试都启动新事务：

```java
@Service
public class OrderStatusUpdater {

    private final OrderService orderService;      // 独立 Bean

    @Retryable(includes = ObjectOptimisticLockingFailureException.class,
               maxRetries = 3, delay = 50, jitter = 25)
    public void changeStatus(OrderId id, OrderStatus status) {
        orderService.changeStatus(id, status);    // @Transactional 放在此处
    }
}
```

乐观锁冲突是典型的可重试场景：冲突确实是暂时性的，重试会重新读取最新版本。参见[事务](transactions.zh-CN.md)。

## 并发限制

```java
import org.springframework.resilience.annotation.ConcurrencyLimit;

@ConcurrencyLimit(8)
public Report generate(ReportRequest request) { … }
```

这是一种 Bulkhead：最多同时运行八次调用，其余调用等待。可以防止某个昂贵操作耗尽所有线程，或确保遵循下游服务的并发配额。

在争用激烈的写路径上，限制并发有时优于重试——它从源头避免冲突，而不是事后应对。

## 代理规则

两个注解都基于代理。同类自调用会静默绕过它们；`private`、`static` 和 `final` 方法也不生效。参见[代理与依赖注入](spring-proxies-and-di.zh-CN.md)。

## 哪些情况值得重试

| 可以重试 | 不要重试 |
|---|---|
| 拒绝连接、连接重置 | 400、401、403、404、422 |
| **幂等**调用发生读取超时 | 校验失败 |
| 502、503、504 | 业务规则违规 |
| 乐观锁冲突 | 没有幂等键的非幂等操作 |
| 检测到死锁 | 程序 Bug，如 `NullPointerException`、`IllegalArgumentException` |

对 **`POST`** 的读取超时最棘手：请求可能已经成功，重试会产生重复。应通过客户端提供的幂等键让端点幂等，否则不要重试写操作。

这就是必须设置 `includes` 的原因。默认情况下，`NullPointerException` 也会被重试四次，然后仍得到同一份堆栈。

## 熔断器

Spring 核心框架**没有**熔断器。仅有重试不能阻止你在依赖全面故障时不断调用它。

标准选择是加入 Spring Cloud Circuit Breaker 和 Resilience4j——这是需要权衡的第三方依赖。很多场景下，成本更低的方案就已足够：

- **严格超时加 `@ConcurrencyLimit`** 限制故障影响：失效依赖最多占用 N 个线程、持续 T 秒。
- 在调用处提供**明确的降级结果**，如上例。
- 为依赖配置 Actuator 健康指示器，使故障可见（参见[可观测性](observability.zh-CN.md)）。

只有在某个真正可选的依赖频繁故障、快速失败明显优于缓慢失败时，才添加熔断器。不要默认添加。

## 使用 Spring Boot 3.5.x 时

核心弹性功能属于 Framework 7，因此 **3.5.x 不提供这些功能**。应使用 Spring Retry：

```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

```java
@EnableRetry     // 不是 @EnableResilientMethods

@Retryable(retryFor = ConnectException.class,
           maxAttempts = 4,                                   // 总次数，而非重试次数
           backoff = @Backoff(delay = 200, multiplier = 2.0, maxDelay = 2000, random = true))
public StockLevel stockFor(ProductId productId) { … }

@Recover
public StockLevel fallback(ConnectException e, ProductId productId) {
    return StockLevel.unknown();
}
```

Spring Retry 还需要 `spring-boot-starter-aspectj`（3.5.x 中为 `-aop`）。升级时将 `maxAttempts` 转换为 `maxRetries = maxAttempts - 1`，展平 `@Backoff` 属性，并在调用方使用 `try` / `catch` 取代 `@Recover`。

## 常见问题

- Boot 4 为基本重试添加 `spring-retry`——重试在核心框架中。
- Boot 4 引入 `org.springframework.retry.annotation`——核心注解位于 `org.springframework.resilience.annotation`。
- 忘记 `@EnableResilientMethods`——注解会静默失效。
- 使用 `maxAttempts`——核心功能使用不含首次调用的 `maxRetries`。
- 使用 `retryFor` / `noRetryFor`——核心属性是 `includes` / `excludes`。
- 将 `@Backoff` 嵌套进 `@Retryable`——核心版本的属性是平铺的。
- 添加 `@Recover`——核心没有恢复回调，应在调用方捕获异常。
- 不设置 `includes`——默认重试所有异常，包括 `NullPointerException`。
- 重试 4xx——每次都会以相同方式失败。
- 超时后重试非幂等 `POST`——会重复执行操作。
- 不添加抖动——整个集群会同步重试。
- 同一方法叠加 `@Retryable` 和 `@Transactional`——所有重试都在只能回滚的事务中运行。
- 重试自调用方法——代理已被绕过。
- 重试调用却不设置超时——一直不返回的调用也不会自动重试。
- 默认添加熔断器——先用超时和并发限制约束故障影响。

## 相关文档

- [HTTP 客户端](http-clients.zh-CN.md) · [事务](transactions.zh-CN.md) · [代理与依赖注入](spring-proxies-and-di.zh-CN.md) · [消息](messaging.zh-CN.md) · [可观测性](observability.zh-CN.md)
