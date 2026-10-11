# 领域建模

## 何时值得建模

只是在表格上进行 CRUD 操作的页面不需要聚合。如果功能只是读取一行、改一个字段再写回，Service、Repository 和 DTO 已足够；添加领域对象反而会让结构更复杂。

当功能包含**业务规则**时，再考虑下文的做法：对象可能处于不同状态、状态转换只在特定条件下合法、多个字段之间存在不变量，或多个计算结果必须始终一致。这些正是贫血模型导致六个 Service 重复编写同一 `if`、随后行为逐渐不一致的场景。

按功能逐个采用。一个应用可以有三个 CRUD 功能和一个领域模型功能，这是合理的。

## 丰富模型，而非贫血模型

贫血模型只有 Getter / Setter，规则却散落在其他地方：

```java
// ❌ 不变量由每个调用方自行记得检查
if (order.getStatus() == OrderStatus.DRAFT) {
    order.getItems().add(new OrderItem(productId, quantity));
    order.setTotal(recalculate(order));
}
```

下一个调用方可能忘记检查状态，或只添加商品却忘记重新计算。应把规则放在持有状态的对象中：

```java
// ✅ 外部调用方无法绕过规则
void addItem(ProductId productId, int quantity, Money unitPrice) {
    if (status != OrderStatus.DRAFT) {
        throw new OrderNotModifiableException(id, status);
    }
    items.add(OrderItem.create(this, productId, quantity, unitPrice));
    recalculateTotal();
}
```

检验方式：**调用方能否将对象变成无效状态？** 如果可以，规则的位置就不对。

## 聚合

聚合是一组以一个**根实体**为中心、作为一致性整体处理的对象。

- **每个聚合根一个 Repository。** `OrderItem` 没有自己的 Repository，应通过 `Order` 访问（参见[Spring Data JPA](spring-data-jpa.zh-CN.md)）。
- **所有访问都通过聚合根。** 不要暴露可变的子对象集合；返回 `List.copyOf(items)`，避免 `order.items().add(…)` 绕过不变量。
- **对其他聚合只引用其 ID，不引用对象。**

```java
class Order {
    private CustomerId customerId;   // ✅ 其他聚合，通过 ID 引用
    private List<OrderItem> items;   // ✅ 当前聚合内部，直接引用
}
```

```java
class Order {
    @ManyToOne Customer customer;    // ❌ 一个对象图中混入两个聚合
}
```

按 ID 引用有助于保持事务边界真实，避免一次懒加载牵出半个数据库结构，也便于之后将两个聚合拆到不同模块或服务。

**保持聚合小巧。** 聚合就是加锁单位：内部所有对象会一起加载和检查版本，因此大型聚合容易成为并发热点。如果两个部分从不在同一事务中改变，它们就应该是两个聚合。

## 值对象

使用在紧凑构造器中校验的 record。创建后，下游代码不必重复校验：

```java
public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Money supports at most 2 decimal places");
        }
        if (currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
    }

    public static Money of(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new CurrencyMismatchException(currency, other.currency);
        }
    }
}
```

- 金额使用 **`BigDecimal`，不要用 `double`**。`0.1 + 0.2` 并不等于精确的 `0.3`。
- 操作应**返回新实例**。值对象不可变，因此 `add` 返回新的 `Money`。
- 拒绝相加不同币种的金额，是原始 `BigDecimal` 类型无法帮你捕获的一类错误。

标识符也应建模为值对象，参见[Spring Data JPA](spring-data-jpa.zh-CN.md)。

## 使用 Sealed Type 表示状态

状态承载不同数据时，应把状态建模为类型选择，而非“状态枚举加一堆可空字段”：

```java
public sealed interface Fulfilment {
    record Pending() implements Fulfilment {}
    record Shipped(TrackingNumber tracking, Instant shippedAt) implements Fulfilment {}
    record Cancelled(String reason, Instant cancelledAt) implements Fulfilment {}
}
```

只有发货时才有意义的 `trackingNumber`，不再是半数代码都忘记检查的可空列。编译器可以验证每个分支都得到处理（参见 java-developer 的面向数据设计建议和[Spring 中的 Java](java-in-spring.zh-CN.md)）。

代价在于持久化：sealed 层次结构不能直接映射为 JPA 实体。可以让实体以扁平形式存储，在访问器中重建 sealed 类型；也可以在底层使用带可空列的 `@Embedded`。

## 领域事件

领域事件是已经发生的事实，使用过去时命名并保持不可变：

```java
public record OrderPlaced(OrderId orderId, CustomerId customerId, Money total, Instant occurredAt) {

    public static OrderPlaced from(Order order) {
        return new OrderPlaced(order.id(), order.customerId(), order.total(), Instant.now());
    }
}
```

事件携带 **ID 和值，不携带实体**。监听器运行时，实体可能已经脱离持久化上下文。

### 让 Spring Data 发布事件

可以在聚合上收集事件，再由 Repository 在 `save()` 时取出发布，而不是由 Service 手工发布：

```java
@Entity
class Order extends BaseEntity {

    @Transient
    private final List<Object> domainEvents = new ArrayList<>();

    void place() {
        if (items.isEmpty()) {
            throw new EmptyOrderException(id);
        }
        this.status = OrderStatus.PLACED;
        this.placedAt = Instant.now();
        domainEvents.add(OrderPlaced.from(this));
    }

    @DomainEvents
    Collection<Object> domainEvents() {
        return List.copyOf(domainEvents);
    }

    @AfterDomainEventPublication
    void clearDomainEvents() {
        domainEvents.clear();
    }
}
```

Spring Data 会在每次 `save()` 时调用 `@DomainEvents`，发布每个事件，再调用 `@AfterDomainEventPublication`。Service 无需处理事件发布细节；事件也不会因为调用方走了另一条 `save` 路径而遗漏。

注意两个故障场景：聚合被修改却**没有保存**时不会发布事件；同一事务中保存两次会重复发布，除非清空事件列表。`@AfterDomainEventPublication` 可处理第二种情况。

继承 `AbstractAggregateRoot<Order>` 并通过 `registerEvent(…)` 也能获得相同行为，代码更少，但会继承 Spring Data 类型。

### 消费事件

```java
@Component
class ReserveInventoryOnOrderPlaced {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void on(OrderPlaced event) {
        inventoryService.reserve(event.orderId());
    }
}
```

`AFTER_COMMIT` 很重要：普通 `@EventListener` 在发布事务中运行，如果之后回滚，副作用仍已发生。使用 Spring Modulith 时，`@ApplicationModuleListener` 会正确组合这些语义并持久化发布记录（参见[Spring Modulith](spring-modulith.zh-CN.md)和[事务](transactions.zh-CN.md)）。

## 命令、查询与结果

Service 使用专用 record 作为输入和输出，不使用松散参数，也不直接传入 HTTP 请求体：

```java
public record PlaceOrderCmd(CustomerId customerId, List<OrderLine> lines, String notes) {}
public record OrderLine(ProductId productId, int quantity, Money unitPrice) {}
public record FindOrdersQuery(CustomerId customerId, OrderStatus status, Pageable pageable) {}
```

| 后缀 | 含义 |
|---|---|
| `*Cmd` | 改变状态的意图 |
| `*Query` | 读取请求 |
| `*Result` | 命令返回值（不仅仅是 ID 时） |

不要直接把 Controller 的 `*Request` record 传给 Service：请求属于 HTTP 层，可能随着 API 版本更新而改变，领域逻辑不应受影响。Controller 把 `CreateOrderRequest` 映射为 `PlaceOrderCmd`，两层就能独立变化。参见[REST Controller](rest-controllers.zh-CN.md)。

也不要使用松散参数：`place(customerId, lines, notes)` 迟早会增加第四、第五个参数，调用点上的 `place(a, b, null, null, true)` 无法表达意图。

如果命令中的组件已经是校验过的值对象，就没有剩余内容需要校验（参见[校验](validation.zh-CN.md)）。

## Specification

对于由多个可选条件组成的查询，组合 `Specification` 优于为每种组合写一个 Repository 方法：

```java
final class OrderSpecifications {

    private OrderSpecifications() {
    }

    static Specification<Order> forCustomer(CustomerId customerId) {
        return (root, query, cb) -> cb.equal(root.get("customerId"), customerId);
    }

    static Specification<Order> withStatus(OrderStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Order> placedAfter(Instant instant) {
        return (root, query, cb) -> cb.greaterThan(root.get("placedAt"), instant);
    }
}
```

```java
var spec = Specification.allOf(
        OrderSpecifications.forCustomer(customerId),
        status != null ? OrderSpecifications.withStatus(status) : null,
        since != null ? OrderSpecifications.placedAfter(since) : null);

return orderRepository.findAll(spec, pageable);
```

Repository 需要实现 `JpaSpecificationExecutor<Order>`。代价是 Specification 以字符串引用字段名，字段重命名仍可编译，却会在运行时失败。大量使用时应启用 JPA Metamodel；条件组合较少时，优先使用普通 `@Query`。

## 防腐层

外部系统的数据模型不应变成本领域模型的一部分，应在边界进行转换：

```java
@Component
class StripePaymentAdapter implements PaymentPort {

    private final StripeClient stripe;

    @Override
    public PaymentOutcome charge(OrderId orderId, Money amount) {
        var request = new StripeChargeRequest(
                amount.amount().movePointRight(2).longValueExact(),   // Stripe 使用最小货币单位
                amount.currency().toLowerCase(),
                orderId.value().toString());

        var response = stripe.charge(request);

        return switch (response.status()) {
            case "succeeded" -> new PaymentOutcome.Authorized(response.id(), amount);
            case "requires_action" -> new PaymentOutcome.RequiresAction(URI.create(response.nextActionUrl()));
            default -> new PaymentOutcome.Declined(response.failureCode());
        };
    }
}
```

只有 Adapter 知道最小货币单位、小写币种代码和提供商状态字符串。没有它们，这些约定就会扩散到领域代码中，更换提供商也会变成重写。Adapter 属于基础设施层；`PaymentPort` 接口属于领域层（参见[六边形架构](hexagonal-architecture.zh-CN.md)）。

## 使用 Spring Boot 3.5.x 时

与版本无关。聚合、值对象、`@DomainEvents` / `@AfterDomainEventPublication`、`AbstractAggregateRoot`、Specification 和 `@TransactionalEventListener` 行为都相同。版本相关的内容在别处：实体标识策略见[Spring Data JPA](spring-data-jpa.zh-CN.md)，Modulith 版本见[Spring Modulith](spring-modulith.zh-CN.md)。

## 常见问题

- 为简单 CRUD 功能创建聚合——没有业务规则时，Service、Repository 和 DTO 已足够。
- 生成 Getter / Setter，并把规则放进 Service——应把行为放在持有状态的对象中。
- 用 `@ManyToOne` 引用另一个聚合——应只引用 ID。
- 返回实时可变的子对象集合——返回 `List.copyOf(...)`，否则可以绕过不变量。
- 为子实体创建 Repository——每个聚合根一个 Repository。
- 创建包含十几个实体的大聚合——聚合是锁定单位，应拆分。
- 用 `double` 表示金额——使用 `Money` 值对象和 `BigDecimal`。
- 让值对象可变——操作应返回新实例。
- 在领域事件中保存实体——只携带 ID 和值；监听器执行时实体可能已脱离上下文。
- 用现在时命名事件——事件是已发生的事实，应叫 `OrderPlaced`，而非 `PlaceOrder`。
- `save` 前手动发布事件——聚合负责收集，让 `@DomainEvents` 在保存时取出。
- 忘记 `@AfterDomainEventPublication`——下次保存会重复发布事件。
- 使用普通 `@EventListener` 执行副作用——它在事务内运行，应使用 `AFTER_COMMIT`。
- 把 HTTP `*Request` record 传入 Service——转换为 `*Cmd`，使 API 可以独立变化。
- 让外部提供商的数据模型渗入领域层——在 Adapter 中转换。
- 只有两个条件的查询也使用 Specification——`@Query` 更清楚且更容易安全重构。

## 相关文档

- [Spring Data JPA](spring-data-jpa.zh-CN.md) · [六边形架构](hexagonal-architecture.zh-CN.md) · [Spring Modulith](spring-modulith.zh-CN.md) · [事务](transactions.zh-CN.md) · [Spring 中的 Java](java-in-spring.zh-CN.md) · [校验](validation.zh-CN.md)
