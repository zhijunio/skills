# 异步与定时任务

## 优先启用虚拟线程

Java 21 及以上版本可以启用：

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

**所有支持虚拟线程的 Spring Boot 版本默认都不启用。** 此设置会将 Tomcat 和 `applicationTaskExecutor` 背后的线程池 `ExecutorService` 替换为**每个任务一个虚拟线程**的执行器，请求因此在虚拟线程上处理。JDBC、HTTP 客户端、消息发送等阻塞调用会挂起虚拟线程并释放底层平台线程，所以吞吐量不再受固定的 200 个 Tomcat 线程限制。

影响如下：

- **“如何处理更多并发请求”不再是选择响应式编程的理由。** 许多团队使用 WebFlux 的主要原因由此消失；虚拟线程上的阻塞代码更简单，扩展能力也相近。
- **连接池会成为真正的限制。** 虚拟线程近乎无限，数据库连接并不是。1 万个并发请求访问只有 10 个连接的连接池，意味着 9,990 个线程等待连接。应调整并监控连接池，而非线程池。
- **线程池不再是有效的隔离舱。** 过去可以用有界线程池限制依赖项的并发工作量；现在应使用 `@ConcurrencyLimit`（参见[弹性模式](resilience.zh-CN.md)）。

虚拟线程**不会解决单个慢请求**。它让多个并发请求的开销变小，却不会加速同一个处理器中依次进行的三个 REST 调用。那是另一个问题，需要单独处理。

三个注意事项：

- **JDK 24 之前，在阻塞调用外使用 `synchronized` 会固定载体线程。** 虚拟线程在 `synchronized` 内阻塞时无法卸载，因此并发量受载体线程数限制，而不是连接池限制。在 JDK 21 与 25 上相同工作的测量值分别为 13.1 秒和 1.0 秒；java-developer 的 `structured-concurrency.md` 中有数据。热点路径优先使用 `ReentrantLock`。该行为取决于**运行时使用的 JDK**，而非编译版本或 Spring Boot 版本。
- **`ThreadLocal` 仍然有效，但按任务创建。** 虚拟线程按请求创建，而不是从池中复用，因此原先可跨请求复用的 `ThreadLocal` 缓存现在会为每个请求分配。
- **线程名不再适合作为标识。** 虚拟线程默认没有名称，依赖线程名的日志、指标和转储会失去关联手段。应关联 Trace 或请求 ID（参见[可观测性](observability.zh-CN.md)）。

## 结构化并发：在一个请求内并行调用

依次调用三个服务的端点需要 1.5 秒，理论上应为 500 毫秒；启用虚拟线程不会改变这一点。`StructuredTaskScope` 可以在各自的虚拟线程上并行执行调用并汇总结果。与 WebFlux 不同，它无需更改被调用的 `RestClient`、JDBC 或服务方法。

**状态与 Boot 服务的建议：** `StructuredTaskScope` 在 Java 25 中仍是**预览 API**。在 Boot 应用中使用它，需要编译器和启动器（包括 Dockerfile 及每个测试任务）都带上 `--enable-preview`。预览版 class 文件不能由不同特性版本的 JDK 加载，升级时必须重新构建。**不要只为此在整个生产服务中启用预览功能。** 仅当单个端点的并行调用延迟经测量确认是问题时才考虑使用，并将其限制在服务边界内；正式定稿前 API 可能再次改变。`ScopedValue` 在 Java 25 已正式发布，无此限制。

作用域应放在 Service 中，不能放在 Controller 中——在 Controller 中打开作用域意味着它在承担编排职责；汇总逻辑应放在 Joiner 中：

```java
@Service
public class SpeakerProfileService {

    private final ProfileSourceClient client;

    SpeakerProfileService(ProfileSourceClient client) {
        this.client = client;
    }

    public Profile bestProfile(SpeakerId id) throws InterruptedException {
        try (var scope = StructuredTaskScope.open(new BestProfileJoiner(),
                cfg -> cfg.withName("profile").withTimeout(Duration.ofSeconds(2)))) {
            scope.fork(() -> client.fromDirectory(id));
            scope.fork(() -> client.fromCrm(id));
            scope.fork(() -> client.fromLegacy(id));
            return scope.join();
        }
    }
}
```

Boot 应用中有三处容易出错：

1. **异常不会原样传出。** `Joiner.result()` 抛出的领域异常会被包装；`join()` 抛出 `StructuredTaskScope.FailedException`，原异常是其 cause。为领域异常编写的 `@RestControllerAdvice` 处理器不会触发，客户端会收到 500，而不是预期的 `ProblemDetail`。在 Service 边界解包，不要让 Advice 了解 `FailedException`（参见[错误处理](error-handling.zh-CN.md)）：

```java
try {
    return scope.join();
} catch (StructuredTaskScope.FailedException e) {
    throw switch (e.getCause()) {
        case ProfileNotFoundException notFound -> notFound;
        case RuntimeException other -> other;
        case Throwable other -> new IllegalStateException(other);
    };
}
```

2. **不要在 `@Transactional` 方法中 fork 仓储调用。** Spring 通过 `TransactionSynchronizationManager` 将事务、连接、`EntityManager` 和同步器绑定到当前线程；它使用 `ThreadLocal`。fork 出的子任务在另一个线程上运行，因此看不到原事务，会从连接池获取自己的连接，并独立提交。应在开启事务前并行调用，或只并行执行非数据库工作。
3. **必须调用 `scope.join()`。** 作用域关闭时如果尚未 join，会抛出 `IllegalStateException: Owner did not join after forking`。在最后一次 `fork` 和 `join` 之间提前 `return`，会让问题在生产环境中出现。

聚合逻辑位于普通 Java 类 `Joiner` 中，可以直接构造实例，给 `onComplete` 传入自行创建的子任务，再断言 `result()`，无需 `@SpringBootTest` 或 `MockMvc`（参见[单元测试](testing-unit.zh-CN.md)）。

### `fork()` 不会传播线程绑定上下文

与下文 `@Async` 原因相同：fork 会创建新线程。

| | 在 `scope.fork(...)` 中 |
|---|---|
| `SecurityContextHolder.getContext()` | 默认 `ThreadLocal` 策略下为空。`MODE_INHERITABLETHREADLOCAL` 会跨 fork 继承，但它是有额外代价的全局开关，不应为单个作用域而修改 |
| `RequestContextHolder`、`@RequestScope` Bean | 不存在；可能抛异常或返回空值 |
| MDC 关联 ID | 丢失，无法关联子任务日志 |
| `TransactionSynchronizationManager` | 为空，见上文 |

将子任务需要的值作为参数传入。如果深层调用链不便传参，可以在 Filter 中绑定 `ScopedValue`：它能跨 fork 读取，并且在 Java 25 已正式发布。添加 JVM 参数 `-Djdk.traceVirtualThreadLocals=true`，每次虚拟线程访问 `ThreadLocal` 时都会打印堆栈，便于排查迁移点。java-developer 的 `structured-concurrency.md` 详细介绍该 API。

不要假定框架已经处理了这些上下文。依赖自行创建的 fork 传播任何上下文前，都应按构建中使用的 Spring 版本验证。

## `@Async`

```java
@Configuration
@EnableAsync
class AsyncConfig {
}
```

```java
@Component
public class OrderNotifier {                     // 必须与调用方分属不同 Bean

    @Async
    public void sendConfirmation(OrderId orderId) {
        emailClient.send(…);
    }

    @Async
    public CompletableFuture<Receipt> generateReceipt(OrderId orderId) {
        return CompletableFuture.completedFuture(receiptService.build(orderId));
    }
}
```

几乎所有 `@Async` 问题都可以归结为三条规则：

1. **同类自调用不会异步。** `this.sendConfirmation(id)` 会在调用方线程同步执行，没有错误或警告。带注解的方法必须位于另一个 Bean 中。参见[代理与依赖注入](spring-proxies-and-di.zh-CN.md)。
2. **返回 `void` 的 `@Async` 方法会吞掉异常。** 调用方没有返回值可观察。应返回 `CompletableFuture<T>`，或注册 `AsyncUncaughtExceptionHandler`，否则失败不可见。
3. **调用方需要结果时应返回 `CompletableFuture<T>`**，不能直接返回原始值。

```java
@Bean
AsyncUncaughtExceptionHandler asyncExceptionHandler() {
    return (throwable, method, params) ->
            log.error("Async {} failed with {}", method.getName(), Arrays.toString(params), throwable);
}
```

### 不会自动传播的上下文

安全上下文、请求作用域和 MDC 日志上下文都绑定在线程上，在异步线程中**不存在**：

- `SecurityContextHolder.getContext()` 为空，`@Async` 方法无法获知调用者身份。将身份作为参数传入；也可以设置 `SecurityContextHolder.setStrategyName(MODE_INHERITABLETHREADLOCAL)`，但它在池化执行器中不起作用。启用虚拟线程后，每个任务创建一个线程，`InheritableThreadLocal` 会在线程创建时继承，因此此限制解除。不要悄悄依赖这一行为：只要方法改用具名池化执行器，就会再次失效。
- `@RequestScope` Bean 和 `RequestContextHolder` 可能抛异常或返回空值。
- MDC 中的关联 ID 会丢失，异步日志无法关联。Micrometer 的上下文传播会处理 Trace 上下文；MDC 需要任务装饰器。

### 执行器

Spring Boot 自动配置一个 `applicationTaskExecutor`。启用虚拟线程后使用虚拟线程，池大小不再重要。未启用时，默认执行器是 `ThreadPoolTaskExecutor`，其队列**无界**：持续负载下任务会不断占用内存，最终耗尽堆，而不是施加背压。

```java
@Bean(name = "reportExecutor")
Executor reportExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);                 // 有界；拒绝任务，避免耗尽堆
    executor.setThreadNamePrefix("report-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    return executor;
}
```

```java
@Async("reportExecutor")
public CompletableFuture<Report> generate(ReportRequest request) { … }
```

为线程设置可读名称。堆栈中的 `pool-2-thread-7` 无法提供有用信息。

## `@Scheduled`

```java
@Configuration
@EnableScheduling
class SchedulingConfig {
}
```

```java
@Component
class ReservationExpiryJob {

    @Scheduled(fixedDelayString = "${app.orders.expiry-scan-interval:PT1M}")
    void expireStaleReservations() {
        reservationService.expireOlderThan(Instant.now().minus(timeout));
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "Europe/London")
    void nightlyReconciliation() {
        reconciliationService.run();
    }
}
```

| 属性 | 行为 |
|---|---|
| `fixedDelay` | 上一次执行**结束**后等待 N；任务不会重叠 |
| `fixedRate` | 每隔 N 开始一次，不考虑执行时长；任务**可能重叠**，执行慢于间隔时也可能排队 |
| `cron` | 日历计划；始终设置 `zone`，默认使用服务器时区，而本机与生产环境的时区可能不同 |

优先使用 `fixedDelay`。使用 `fixedRate` 时，如果任务偶尔执行过久，就会并发执行同一任务，而这通常不安全。

将间隔绑定到属性，以便调整或禁用而无需重新构建。Spring cron 有**六个**字段（秒在最前），不是 Unix cron 的五个字段；五字段表达式会导致启动失败。

### 默认单线程调度器

**调度器默认只有一个线程。** 应用中所有 `@Scheduled` 方法共享该线程，因此一个慢任务会悄悄拖延其他任务，看起来像是“偶尔不运行”。

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 4
```

### 多实例会重复执行任务

部署三个副本，夜间任务就会运行三次。这是本文件提到的最常见生产意外；`@Scheduled` 和 Spring Boot 都不会自动处理。

从最简单的方案开始选择：

- **让任务幂等**，使并发执行无害。可行时这是最佳方案。
- **原子认领工作项。** 使用 `UPDATE … SET claimed_by = ? WHERE status = 'PENDING' AND claimed_by IS NULL` 并限制数量，每个实例只处理自己认领的任务；无需额外基础设施。
- **使用分布式锁**，例如 `SELECT … FOR UPDATE SKIP LOCKED` 行锁或锁库；使用锁库会增加依赖。
- **将调度移出应用。** 例如让 Kubernetes `CronJob` 调用端点，或使用独立调度服务，适用于确实必须只运行一次的任务。

无论选哪种方案，都要明确写出。只因当前部署恰好只有一个副本才正确的任务，会在扩容时出错。

### 定时方法与事务

在同一方法上使用 `@Scheduled` 和 `@Transactional` 是可行的：调度触发会经过代理，两个注解都会生效。但定时方法应保持简短；长事务会在整个执行期间占用连接和锁（参见[事务](transactions.zh-CN.md)）。应由定时方法获取有限数量的批次，再委托给事务 Bean 逐项处理。

与其他代理功能一样，`@Scheduled` 需要 Spring Bean 上的 `public`、非 `final` 方法，而且方法不能有参数。

## 优雅关闭

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

正在处理的请求会完成，而不是在写入过程中被终止。自定义执行器也应设置 `waitForTasksToCompleteOnShutdown`，否则部署时排队的异步任务会被丢弃。该超时应短于编排平台的终止宽限期，否则 Pod 仍会被强制终止。

## 测试

不要使用 `Thread.sleep`，应使用 Awaitility：

```java
@Test
void sendsConfirmationAsynchronously() {
    orderService.place(cmd);

    await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> verify(emailClient).send(any()));
}
```

测试中禁用调度任务，避免任务不可预测地触发：在 `application-test.yml` 设置 `spring.task.scheduling.enabled=false`，并直接调用方法进行测试。Spring Boot 4 会在缓存的测试上下文之间暂停调度任务（参见[测试策略](testing-strategy.zh-CN.md)）。

## 使用 Spring Boot 3.5.x 时

`@Async`、`@Scheduled`、执行器和调度器属性，以及优雅关闭设置都相同。Java 21 及以上版本从 Boot 3.2 起支持 `spring.threads.virtual.enabled`，因此也可以在 3.5 中使用虚拟线程。载体线程固定问题以及 `StructuredTaskScope`、`ScopedValue` 是否存在，主要取决于 JDK 版本。结构化并发属于 Java 25 能力，与 Boot 4 无关：Boot 3.5 搭配 Java 25 可以使用，Boot 4 搭配 Java 21 则不行。

## 常见问题

- 在 `this` 上调用 `@Async` 方法——方法会同步运行，没有警告。
- `void` 异步方法没有异常处理器——异常会静默消失。
- 忘记 `@EnableAsync` 或 `@EnableScheduling`——对应注解不起作用。
- 预期异步线程中有 `SecurityContextHolder`——上下文为空，应显式传入身份。
- 预期 MDC 或关联 ID 自动传播——没有任务装饰器时不会传播。
- 高负载时依赖默认执行器——队列无界，应设置上限和拒绝策略。
- 调度器保持默认单线程——一个慢任务会悄悄延迟其他任务。
- 对可能执行很久的任务使用 `fixedRate`——任务可能重叠。
- 使用五字段 cron——Spring cron 有六字段，秒在最前。
- cron 未设置 `zone`——服务器时区可能因环境而异。
- 多副本部署 `@Scheduled` 任务——每个副本都会执行一次。
- 定时方法中运行长事务——应获取有限批次，再逐项委托处理。
- 给 `@Scheduled` 方法添加参数——该方法不能有参数。
- 在 Java 21 及以上版本仅为并发而选响应式编程——虚拟线程已消除这项理由。
- 启用虚拟线程却保留默认连接池大小——连接池会成为瓶颈。
- 假定虚拟线程默认启用——所有 Boot 版本都默认关闭，需设置 `spring.threads.virtual.enabled`。
- 虚拟线程热点路径使用 `synchronized`——JDK 24 之前会固定载体线程；优先考虑 `ReentrantLock`，检查运行时 JDK。
- 启用虚拟线程来解决请求内部顺序调用过慢——它不会加速单个请求中的顺序调用。
- 使用 `StructuredTaskScope` 却未在编译器、启动器和容器添加 `--enable-preview`——Java 25 中该 API 仍是预览版。
- 为使用一个作用域就在整个生产服务启用预览功能——只在测量确认的热点端点使用，否则不要启用。
- 在 Controller 中打开结构化作用域——应放在 Service 中，并把聚合逻辑放进 `Joiner`。
- 在 `@Transactional` 内 fork 仓储调用——事务绑定在线程上，子任务会使用自己的连接并独立提交。
- 为 `Joiner.result()` 抛出的领域异常直接编写 `@RestControllerAdvice`——异常会包装为 `FailedException`，应在 Service 边界解包。
- 期望 `scope.fork(...)` 中能读取 `SecurityContextHolder` 或 MDC——两者都绑定在线程上，子任务中为空。
- 在 fork 后、join 前提前返回——关闭作用域时会抛出 `IllegalStateException: Owner did not join after forking`。
- 异步测试使用 `Thread.sleep`——应使用 Awaitility。
- 测试中保持调度启用——任务可能在测试中途触发，导致不可复现的失败。

## 相关文档

- [代理与依赖注入](spring-proxies-and-di.zh-CN.md) · [事务](transactions.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [弹性模式](resilience.zh-CN.md) · [可观测性](observability.zh-CN.md) · [Spring Batch](spring-batch.zh-CN.md) · [配置](configuration.zh-CN.md)
- java-developer 的 `structured-concurrency.md` 介绍语言层面的 API：`StructuredTaskScope`、`Joiner`、`ScopedValue` 及载体线程固定的测量数据。
