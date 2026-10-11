# 事务

## `@Transactional` 应该放在哪里

**放在 Service 上。** 不在 Controller，也不在 Repository 上。

Service 方法代表一个工作单元，是必须整体成功或失败的范围。Repository 方法范围太小——一个业务操作中的两次 Repository 调用会变成两个事务。Controller 范围太大，还会把 HTTP 关注点带进事务边界。

```java
@Service
@Transactional(readOnly = true)          // 此类所有方法的默认设置
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    OrderService(OrderRepository orderRepository, InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional                        // 写操作覆盖类级设置
    public OrderId place(PlaceOrderCmd cmd) {
        var order = Order.create(cmd.customerId());
        cmd.lines().forEach(line -> order.addItem(line.productId(), line.quantity(), line.unitPrice()));
        order.place();
        inventoryService.reserve(order.id(), cmd.lines());   // 加入当前事务
        return orderRepository.save(order).id();
    }

    public OrderResponse getById(OrderId id) {               // 继承 readOnly = true
        return OrderResponse.from(orderRepository.getById(id));
    }
}
```

类级 `readOnly = true`、写方法级覆盖是合理的默认方式：读取通常多于写入；忘记给读方法标记只读较隐蔽，忘记给写方法标记事务则影响明显。

`readOnly = true` 并非装饰。Hibernate 会将 flush 模式设为手动，从而跳过每个已加载实体的脏检查；数据库驱动也可以把请求路由到只读副本。

## 传播行为

| 传播行为 | 行为 |
|---|---|
| `REQUIRED`（默认） | 加入调用方事务；没有时则启动事务 |
| `REQUIRES_NEW` | 暂停调用方事务，开启新事务，结束后恢复原事务 |
| `MANDATORY` | 必须已有事务，否则抛异常 |
| `SUPPORTS` | 有事务则加入，否则无事务执行 |
| `NOT_SUPPORTED` | 暂停已有事务，无事务执行 |
| `NEVER` | 若存在事务则抛异常 |
| `NESTED` | 在调用方事务中创建保存点 |

通常只会用到 `REQUIRED`，偶尔使用 `REQUIRES_NEW`。其他选项只在遇到对应场景时使用。

`REQUIRES_NEW` 适合必须在调用方回滚后仍保留的工作，例如审计记录或失败日志：

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void recordAttempt(AuditEvent event) {
    auditRepository.save(event);          // 独立提交
}
```

它会在第一个连接挂起期间从连接池再取一个**连接**。在大型集合循环中使用 `REQUIRES_NEW` 会耗尽连接池并死锁：外部事务持有连接，等待内部事务获取已经没有空闲的连接。

## 回滚规则

`@Transactional` 遇到 `RuntimeException` 和 `Error` 时回滚；遇到受检异常时会**提交**。

```java
@Transactional(rollbackFor = InventoryUnavailableException.class)   // 受检异常
public OrderId place(PlaceOrderCmd cmd) throws InventoryUnavailableException { … }
```

更简单的方式是让领域异常继承非受检异常（参见[错误处理](error-handling.zh-CN.md)）。这样自动回滚，也不要求调用方声明异常。

**捕获异常不会撤销回滚标记。** 参与事务的方法一旦抛出异常，事务就会被标记为只能回滚；捕获后继续执行，提交时仍会得到 `UnexpectedRollbackException`。如果某一步确实可选，应使用 `REQUIRES_NEW`，让它的失败不会影响调用方事务。

## 同类自调用

`@Transactional` 基于代理，因此 `this.otherMethod()` 会完全绕过代理，事务不会生效，而且没有提示。这是最常见的 Spring 错误之一，完整原因和解决方法见[代理与依赖注入](spring-proxies-and-di.zh-CN.md)。

同理，`private`、`static` 或 `final` 方法上的 `@Transactional` 都不起作用。

## 缩短事务时间

事务会占用数据库连接；写事务还会持有行锁。事务中的所有操作都会增加竞争。

**绝不要在事务内发起网络调用。** 在 `@Transactional` 中调用支付服务的 HTTP 接口，会在第三方故障期间一直占用连接和锁。应调整流程，把远程工作放到事务之外，让事务只处理数据库写入。

```java
// ❌ 整个远程网络往返期间都保持事务打开
@Transactional
public void pay(OrderId id) {
    var order = orderRepository.getById(id);
    var result = paymentGateway.charge(order.total());   // 可能等待数秒或超时
    order.markPaid(result.reference());
}
```

```java
// ✅ 远程调用放在外部，前后使用两个短事务
public void pay(OrderId id) {
    var total = orderService.totalOf(id);                // 事务 1
    var result = paymentGateway.charge(total);           // 不占用事务
    orderService.markPaid(id, result.reference());       // 事务 2
}
```

这会产生一个真实窗口：扣款已成功，但本地状态尚未写入。这属于分布式系统问题，应该通过幂等和对账解决；延长事务也无法解决。

### 事务绑定在线程上，不会跨越 fork

Spring 通过 `ThreadLocal` 的 `TransactionSynchronizationManager` 保存连接、`EntityManager` 和同步器。任何切换线程的工作都会离开原事务，包括 `@Async`、`parallelStream()` 和 `StructuredTaskScope.fork(...)`。子任务不会加入原事务，也不会随原事务一起失败：它会从连接池取得自己的连接，并独立提交。

```java
@Transactional                                       // ❌ fork 的任务不属于当前事务
public void reprice(OrderId id) {
    try (var scope = StructuredTaskScope.open()) {
        scope.fork(() -> orderRepository.save(...));  // 使用自己的连接、独立提交
        scope.fork(() -> auditRepository.save(...));  // 即使前者失败，它也会提交
        scope.join();
    }
}
```

应在事务开启前并行执行非数据库工作，等待所有任务完成后再写入。参见[异步与定时任务](async-and-scheduling.zh-CN.md)。

## 乐观锁

实体上的 `@Version`（参见[Spring Data JPA](spring-data-jpa.zh-CN.md)）会让并发更新失败，而不是静默覆盖：

```java
@Transactional
public void changeStatus(OrderId id, OrderStatus status) {
    orderRepository.getById(id).changeStatus(status);
    // 若另一个事务先提交，则在提交时抛出 ObjectOptimisticLockingFailureException
}
```

将其映射为 **409** 并让客户端重试，或在服务器端重试（参见[弹性模式](resilience.zh-CN.md)）。悲观锁（`@Lock(LockModeType.PESSIMISTIC_WRITE)`）适用于竞争确实激烈、重试会造成冲击的行；它会持有数据库锁，因此事务必须极短。

## 副作用应发生在提交之后

在事务中发送邮件，即使之后回滚，邮件也已经发出。应将副作用绑定到事务提交：

```java
@Transactional
public OrderId place(PlaceOrderCmd cmd) {
    var order = Order.create(cmd.customerId());
    orderRepository.save(order);
    events.publishEvent(new OrderPlaced(order.id(), order.total()));  // 还未分发
    return order.id();
}
```

```java
@Component
class OrderPlacedNotifier {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(OrderPlaced event) {
        emailService.sendConfirmation(event.orderId());   // 数据持久化后再发送
    }
}
```

- 普通 `@EventListener` 会**在发布事务内部同步执行**，这正是应该避免的行为。
- `AFTER_COMMIT` 在**原事务之外**运行，因此监听器需要写数据库时，应使用自己的 `@Transactional(propagation = REQUIRES_NEW)`。
- 默认情况下监听器仍在发布线程执行，因此调用方会等待。添加 `@Async` 可以解耦；但提交后异步失败时，如果没有持久化待处理意图，失败就会丢失。

Spring Modulith 的 `@ApplicationModuleListener` 组合了 `AFTER_COMMIT`、`@Async` 和 `REQUIRES_NEW`，还能持久化事件发布记录以应对进程崩溃。可用时优先考虑（参见[Spring Modulith](spring-modulith.zh-CN.md)）。跨进程边界则使用事务 Outbox（参见[消息](messaging.zh-CN.md)）。

## 重试与事务

绝不要在同一个方法上同时添加 `@Retryable` 和 `@Transactional`。失败后事务已被标记为回滚，重试会在同一个事务中运行，最后仍会提交失败。把重试放在**调用方 Bean** 上，使每次尝试都获得新事务。参见[弹性模式](resilience.zh-CN.md)。

## 跨服务操作

HTTP 调用之间没有分布式事务。应使用 Saga：一系列本地事务，每步都定义补偿操作。

```java
@Service
public class PlaceOrderSaga {

    public OrderId execute(PlaceOrderCmd cmd) {
        var orderId = orderService.createDraft(cmd);            // 本地事务
        try {
            inventoryClient.reserve(orderId, cmd.lines());
            paymentClient.charge(orderId, cmd.total());
            orderService.confirm(orderId);                      // 本地事务
            return orderId;
        } catch (PaymentFailedException e) {
            inventoryClient.release(orderId);                   // 补偿
            orderService.fail(orderId, e.getMessage());
            throw e;
        }
    }
}
```

Saga 编排方法本身**不能**标记为 `@Transactional`，因为它包含网络调用。每个补偿操作都必须幂等，因为它可能在部分失败后执行，也可能被重试。

## 使用 Spring Boot 3.5.x 时

本文内容都相同。只有重试存在差异：核心 `@Retryable` 和 `@EnableResilientMethods` 属于 Framework 7；3.5.x 使用独立的 Spring Retry 项目，属性名称不同（参见[弹性模式](resilience.zh-CN.md)）。

## 常见问题

- 在 Controller 上使用 `@Transactional`——事务应放在 Service 上。
- 通过 `this` 调用事务方法——代理被绕过，没有事务。
- 在 `private` 方法上添加 `@Transactional`——代理无法增强它。
- 读取方法未指定 `readOnly = true`——会失去脏检查和只读副本路由优化。
- 期望受检异常触发回滚——受检异常会提交；使用非受检异常或指定 `rollbackFor`。
- 在事务内捕获异常后继续——事务已标为只能回滚，最终可能抛 `UnexpectedRollbackException`。
- 在事务内进行 HTTP 调用——远程超时期间一直占用连接和锁。
- 在循环中使用 `REQUIRES_NEW`——每次需要第二个连接，连接池可能耗尽并死锁。
- 在事务中发送邮件或发布消息——改用 `@TransactionalEventListener(AFTER_COMMIT)`。
- 用普通 `@EventListener` 处理提交后副作用——它会在事务内执行。
- `AFTER_COMMIT` 监听器写数据库时没有 `REQUIRES_NEW`——此时已没有活动事务。
- 在一个方法上叠加 `@Retryable` 和 `@Transactional`——每次重试都运行在已注定回滚的事务中。
- Saga 编排器标记为 `@Transactional`——编排包含网络调用，每个步骤应使用自己的本地事务。
- 补偿操作不幂等——失败重试时可能执行多次。
- 在 `@Transactional` 中 fork 仓储调用，或使用 `parallelStream()` 并行访问数据库——事务绑定在线程上，每个任务独立提交，原事务回滚不会影响它们。

## 相关文档

- [代理与依赖注入](spring-proxies-and-di.zh-CN.md) · [Spring Data JPA](spring-data-jpa.zh-CN.md) · [弹性模式](resilience.zh-CN.md) · [消息](messaging.zh-CN.md) · [Spring Modulith](spring-modulith.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [异步与定时任务](async-and-scheduling.zh-CN.md)
