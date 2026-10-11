# 六边形架构

端口与适配器：领域定义所需能力的接口，基础设施提供实现。依赖方向**向内**，领域层不依赖任何外层。

## 它属于功能内部

六边形架构常被画成三个顶层包：

```text
com.example.app/
├── domain/            ← ❌ 只是换名字的分层结构
├── application/
└── infrastructure/
```

这与 `controller/` / `service/` / `repository/` 有相同缺陷：每项功能都被摊在三个包中，没有类可以设为包级可见，顶层包名描述的是架构风格，而不是应用功能。

六边形描述的是**单个功能内部**的结构。它应与按功能组织包共存，而不是取代该方式（参见[代码组织](code-organization.zh-CN.md)）：

```text
com.example.app/
├── Application.java
├── payment/                        ← 功能
│   ├── PaymentApi.java             ← 其他功能可调用的 API
│   ├── domain/
│   │   ├── Payment.java            ← 纯 Java，不依赖框架
│   │   ├── Money.java
│   │   └── port/
│   │       ├── in/  TakePaymentUseCase.java
│   │       └── out/ PaymentGatewayPort.java, PaymentRepository.java
│   ├── application/
│   │   └── TakePaymentService.java ← @Service，实现入站端口
│   └── infrastructure/
│       ├── StripePaymentAdapter.java     ← 实现 PaymentGatewayPort
│       ├── JpaPaymentRepository.java     ← 实现 PaymentRepository
│       └── PaymentController.java        ← 驱动适配器
├── order/                          ← 普通功能，无需六边形结构
└── config/
```

旁边的 `order/` 保持扁平是正确的，不是不一致。某个功能采用六边形、另一个不采用，是合理做法。

## 何时值得采用

它会带来实际成本：第二套模型、双向 Mapper，以及每个出站调用都要有接口。

| 值得使用 | 不值得使用 |
|---|---|
| 复杂规则需要完全脱离框架测试 | 表格 CRUD |
| 外部提供商确实可替换（支付、消息、存储） | 永远不会更换的单一数据库 |
| 领域核心需要比框架活得更久 | 两年内就会重写的服务 |
| 受监管核心逻辑需要独立审计 | 实体本身就是模型的简单功能 |

“数据库可能更换”不是理由。实际上很少有人更换数据库，而且 Spring Data 已经提供抽象。

**脱离框架测试纯领域是主要收益。** 无 Spring、无 Testcontainers、无上下文，测试只需毫秒。若功能规则足够多，这种收益值得付出成本；否则六边形只是增加间接层。

## 领域层：不依赖框架

```java
public class Payment {

    private final PaymentId id;
    private final OrderId orderId;
    private final Money amount;
    private PaymentStatus status;
    private String gatewayReference;

    private Payment(PaymentId id, OrderId orderId, Money amount) {
        this.id = Objects.requireNonNull(id);
        this.orderId = Objects.requireNonNull(orderId);
        this.amount = Objects.requireNonNull(amount);
        this.status = PaymentStatus.PENDING;
    }

    public static Payment initiate(OrderId orderId, Money amount) {
        return new Payment(PaymentId.newId(), orderId, amount);
    }

    public void authorize(String gatewayReference) {
        if (status != PaymentStatus.PENDING) {
            throw new PaymentNotPendingException(id, status);
        }
        this.status = PaymentStatus.AUTHORIZED;
        this.gatewayReference = Objects.requireNonNull(gatewayReference);
    }

    public PaymentId id() { return id; }
    public PaymentStatus status() { return status; }
}
```

不要添加 `@Entity`、`@Component`、`jakarta.persistence` 或 `org.springframework`。纯领域约束是整个架构的基础，其余设计都从这里推导。应使用 ArchUnit 强制执行（参见[架构测试](archunit.zh-CN.md)），否则几周内就会逐渐侵蚀：

```java
noClasses().that().resideInAPackage("..payment.domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "jakarta.persistence..");
```

## 端口

**入站端口**（驱动端口）表示功能对外提供什么；**出站端口**（被驱动端口）表示它需要外部提供什么。

```java
// domain/port/in - 外部可以请求的操作
public interface TakePaymentUseCase {
    PaymentResult take(TakePaymentCommand command);
}

public record TakePaymentCommand(OrderId orderId, Money amount) {}

// domain/port/out - 功能需要外部提供的能力
public interface PaymentGatewayPort {
    GatewayOutcome charge(PaymentId paymentId, Money amount);
}

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(PaymentId id);
}
```

出站端口使用**领域术语**（`Money`、`PaymentId`），并返回领域类型。签名中一旦出现 `ResponseEntity`、`Page` 或 JPA 实体，就已经让基础设施侵入领域。

这里的 `PaymentRepository` 是领域接口，不是 `JpaRepository`。这是有意为之：领域描述自己的需求，JPA 只是可能的实现方式。

## 应用层：用例

```java
@Service
public class TakePaymentService implements TakePaymentUseCase {

    private final PaymentRepository payments;
    private final PaymentGatewayPort gateway;

    TakePaymentService(PaymentRepository payments, PaymentGatewayPort gateway) {
        this.payments = payments;
        this.gateway = gateway;
    }

    @Override
    @Transactional
    public PaymentResult take(TakePaymentCommand command) {
        var payment = Payment.initiate(command.orderId(), command.amount());
        payments.save(payment);

        var outcome = gateway.charge(payment.id(), command.amount());
        if (outcome.authorized()) {
            payment.authorize(outcome.reference());
        } else {
            payment.decline(outcome.failureCode());
        }
        return PaymentResult.from(payments.save(payment));
    }
}
```

应用层可以使用 Spring，负责编排：加载、调用领域方法、保存；自身不应包含业务规则。把本应属于领域方法的条件判断写在此处，是六边形退化为贫血模型的常见开端。

注意示例在 `@Transactional` 中发起网络调用，这违反[事务](transactions.zh-CN.md)建议；真实代码应拆开事务。

## 基础设施：适配器

```java
@Repository
class JpaPaymentRepository implements PaymentRepository {

    private final SpringDataPaymentRepository jpa;

    JpaPaymentRepository(SpringDataPaymentRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Payment save(Payment payment) {
        return PaymentJpaMapper.toDomain(jpa.save(PaymentJpaMapper.toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(PaymentId id) {
        return jpa.findById(id.value()).map(PaymentJpaMapper::toDomain);
    }
}

interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, UUID> {}
```

简单功能只需一个类型；六边形需要 `Payment`（领域）和 `PaymentJpaEntity`（持久化）两类。**这就是架构成本**，采用前应认真衡量。收益是 Schema 变化不会影响领域，持久化注解也不会扭曲领域模型。

不要在领域类上加 `@Entity` 来省掉 Mapper，这会丢失整个架构的意义。

更新映射会更复杂：对已有行执行 `toEntity` 时，必须合并到已加载实体，而不是创建新实体，否则会丢失 JPA 身份和 `@Version`。应加载实体、应用变更字段，再保存。

Controller 是驱动适配器，应依赖入站端口，而不是具体 Service 类：

```java
@RestController
@RequestMapping("/api/payments")
class PaymentController {

    private final TakePaymentUseCase takePayment;   // 依赖端口，而不是 TakePaymentService

    @PostMapping
    ResponseEntity<PaymentResponse> take(@Valid @RequestBody TakePaymentRequest request) {
        var result = takePayment.take(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(result));
    }
}
```

## 测试

架构的收益可以从领域测试中看出：

```java
class PaymentTest {                                    // 完全不启动 Spring

    @Test
    void authorizesPendingPayment() {
        var payment = Payment.initiate(orderId, Money.of("49.99", "GBP"));
        payment.authorize("ch_123");
        assertThat(payment.status()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    @Test
    void refusesToAuthorizeTwice() {
        var payment = Payment.initiate(orderId, Money.of("49.99", "GBP"));
        payment.authorize("ch_123");
        assertThatThrownBy(() -> payment.authorize("ch_456"))
                .isInstanceOf(PaymentNotPendingException.class);
    }
}
```

用例应针对出站端口的**手写 Fake**测试，而不是到处使用 Mockito：内存版 `PaymentRepository` 只需几行，并且可供所有用例复用。参见[单元测试](testing-unit.zh-CN.md)。

适配器仍需要真实测试：JPA 适配器使用 Testcontainers（参见[持久化切片测试](testing-slices-persistence.zh-CN.md)），网关适配器使用 WireMock（参见[集成测试](testing-integration.zh-CN.md)）。六边形不会省去这些测试，只是隔离了它们。

## 使用 Spring Boot 3.5.x 时

本文没有版本相关内容；它是由纯 Java、`@Service`、`@Repository` 和 `@Transactional` 组成的结构模式。Boot 4 的差异在适配器中：Web 适配器使用 Jackson 3，适配器测试使用 Testcontainers 2。

## 常见问题

- 在根包创建 `domain/`、`application/`、`infrastructure/`——这是换了名称的分层架构；六边形应位于单项功能内部。
- 对所有功能采用六边形——只在业务规则足以证明其价值时采用；CRUD 功能保持扁平。
- 在领域类上使用 `@Entity` 或 `jakarta.persistence`——领域必须不依赖框架。
- 在用例中注入 `JpaRepository`——应依赖领域自己的 Repository 端口。
- 端口签名暴露 `Page`、`ResponseEntity` 或实体——端口应使用领域术语。
- 在领域类上添加 `@Transactional`——事务属于应用 Service。
- Controller 依赖具体 Service——应依赖入站端口。
- 混淆入站与出站端口——`in` 表示功能提供的能力，`out` 表示功能需要的能力。
- 端口背后只有 Getter / Setter——贫血六边形只有成本没有收益。
- 领域模型与实体共用一个类，省略 Mapper——这也消除了采用六边形的理由。
- 更新时重新创建实体——应合并到已加载实体，否则丢失身份和 `@Version`。
- 到处用 Mockito Mock 出站端口——手写内存 Fake 更清晰且可复用。
- 只依靠纪律保持领域纯净——应添加 ArchUnit 规则，否则边界会逐渐侵蚀。

## 相关文档

- [代码组织](code-organization.zh-CN.md) · [领域建模](domain-modelling.zh-CN.md) · [架构测试](archunit.zh-CN.md) · [单元测试](testing-unit.zh-CN.md) · [事务](transactions.zh-CN.md)
