# 单元测试

使用纯 JUnit、Mockito 和 AssertJ，**不启动 Spring 上下文**。这类测试在毫秒内完成，验证的是*你的代码*——Spring 自己有自己的测试。

构造器注入使这一点成为可能：用测试替身直接创建 Bean，无需容器。这是遵循[代理与依赖注入](spring-proxies-and-di.zh-CN.md)建议的实际收益。

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock PaymentGateway paymentGateway;

    @InjectMocks OrderService orderService;

    @Test
    void placesOrderAndCharges() {
        var cmd = new PlaceOrderCmd(customerId, List.of(line), null);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var orderId = orderService.place(cmd);

        assertThat(orderId).isNotNull();
        verify(paymentGateway).charge(any(), eq(Money.of("49.99", "GBP")));
    }
}
```

## 哪些对象应使用 Mock

在**架构边界**使用 Mock：外部服务、Repository、时钟、随机源。值对象、领域实体和纯逻辑应使用真实对象；Mock 这些对象会让测试依赖本不该知道的内部实现。

```java
// ❌ Mock 值对象
@Mock Money total;
when(total.amount()).thenReturn(new BigDecimal("49.99"));

// ✅ 直接构造
var total = Money.of("49.99", "GBP");
```

如果测试需要五个 Mock，被测类很可能有五项职责。Mock 数量是设计信号。

## Repository 优先使用 Fake，而非 Mock

手写内存 Fake 通常比逐次 Stub Repository 更清楚，也可以复用：

```java
class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.id(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return Optional.ofNullable(store.get(id));
    }
}
```

Fake 只需编写一次约二十行代码，而不是在每个测试里写 `when(...)`；测试读起来是业务场景，而不是交互脚本。这与[六边形架构](hexagonal-architecture.zh-CN.md)中的出站端口很契合。

对于继承了数十个方法的 Spring Data 接口，Fake 不太适用；这种情况下 Mock 实际用到的少数方法即可。

## 断言行为，而非实现

```java
// ❌ 将测试绑定到内部计算方式
verify(orderService).recalculateTotal();

// ✅ 断言实际结果
assertThat(order.total()).isEqualTo(Money.of("99.98", "GBP"));
```

谨慎使用 `verify()`，主要用于证明**副作用**发生（例如完成扣款）或未发生，例如购物车为空时 `verifyNoInteractions(paymentGateway)`。充满 `verify()` 的测试会在每次重构时失败，却抓不到实际问题。

## AssertJ

```java
assertThat(orders)
        .hasSize(2)
        .extracting(Order::status)
        .containsExactly(OrderStatus.PLACED, OrderStatus.SHIPPED);

assertThat(order).satisfies(o -> {
    assertThat(o.total()).isEqualTo(expected);
    assertThat(o.items()).isNotEmpty();
});

assertThatThrownBy(() -> order.addItem(productId, 1, price))
        .isInstanceOf(OrderNotModifiableException.class)
        .hasMessageContaining("PLACED");
```

使用 AssertJ，而非 JUnit 的 `assertEquals`：失败消息更有用；`extracting`、`satisfies` 和 `containsExactlyInAnyOrder` 能直接表达意图，避免手写循环。

## 测试领域逻辑

富领域对象（参见[领域建模](domain-modelling.zh-CN.md)）是代码库中最容易测试的部分，无需任何 Mock：

```java
@Test
void refusesToAddItemToPlacedOrder() {
    var order = Order.create(customerId);
    order.addItem(productId, 1, Money.of("10.00", "GBP"));
    order.place();

    assertThatThrownBy(() -> order.addItem(otherProductId, 1, Money.of("5.00", "GBP")))
            .isInstanceOf(OrderNotModifiableException.class);
}
```

如果把规则放在领域对象中能让测试如此简单，而放到 Service 中就需要 Mock，这正是贫血模型带来的测试摩擦。

## 时间与随机数

```java
@Test
void expiresReservationAfterTimeout() {
    var clock = Clock.fixed(Instant.parse("2026-08-10T12:00:00Z"), ZoneOffset.UTC);
    var service = new ReservationService(repository, clock);
    …
}
```

注入 `Clock`；需要测试的代码中不要调用 `Instant.now()`。只在午夜、月末或 CI 时区下失败的测试，本身就有问题。

## 参数化测试

```java
@ParameterizedTest
@CsvSource({
        "DRAFT,     true",
        "PLACED,    false",
        "SHIPPED,   false",
        "CANCELLED, false",
})
void allowsItemsOnlyWhileDraft(OrderStatus status, boolean allowed) { … }
```

将几乎相同的用例合并，不要复制测试方法。`@EnumSource(OrderStatus.class)` 会覆盖所有枚举值，**新增枚举值时测试也会失败**；很多时候这正是预期行为。

## 不该进行单元测试的内容

- Getter、Setter 和 record 访问器。
- 框架装配——`@Autowired` 能否工作是 Spring 的测试，不是你的测试。
- 通过反射测试私有方法。如果私有方法需要单独测试，它可能应该成为独立类。
- 简单派生的 Spring Data 查询——这是 Spring Data 的职责（参见[持久化切片测试](testing-slices-persistence.zh-CN.md)）。

## 使用 Spring Boot 3.5.x 时

与版本无关。只有 JUnit 版本不同：3.5.x 使用 JUnit 5，4.x 使用 JUnit 6，但上面的代码不受影响。两个版本的测试 Starter 都会传递引入 Mockito 和 AssertJ。

> **Native Image：** GraalVM Native Image 不支持 Mockito，因此基于 `@Mock` 的测试无法在 `nativeTest` 中运行。参见[测试策略](testing-strategy.zh-CN.md)。

## 常见问题

- 不需要 Spring 上下文的测试也使用 `@SpringBootTest`——应使用纯 JUnit。
- Mock 值对象和实体——直接构造，只在边界处使用 Mock。
- 一个类需要五个以上 Mock——这是设计信号，不是 Mock 不足。
- 逐次 Stub Repository 调用——手写内存 Fake 更清楚且可复用。
- 验证内部方法调用——应断言实际结果。
- 对每个调用都使用 `verify()`——只用于副作用。
- 内联调用 `Mockito.mock()`，而不使用 `@ExtendWith(MockitoExtension.class)` 和 `@Mock`。
- 使用 JUnit 的 `assertEquals`——应使用 AssertJ。
- 生产代码调用 `Instant.now()`，然后断言时间——注入 `Clock`。
- 复制近似相同的测试方法——应使用 `@ParameterizedTest`。
- 对 Getter、框架装配或私有方法做单元测试。
- 测试派生 Spring Data 查询——那是 Spring Data 的代码。

## 相关文档

- [测试策略](testing-strategy.zh-CN.md) · [领域建模](domain-modelling.zh-CN.md) · [六边形架构](hexagonal-architecture.zh-CN.md) · [代理与依赖注入](spring-proxies-and-di.zh-CN.md) · [Spring 中的 Java](java-in-spring.zh-CN.md)

---

*单元测试和 Mock 指南参考 **Philip Riecks** 的 Testing Spring Boot Applications Demystified。*
