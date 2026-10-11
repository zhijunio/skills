# Spring 中的 Java

本文说明 java-developer 的语言规则如何应用到 Spring 代码库。**java-developer 负责语言本身**：不可变性、record、sealed type、模式匹配、`Optional`、`var` 和文本块。本文只处理框架对这些规则的限制，以及 java-developer 建议按系统决定的 Spring 特定做法。

## record 与 Bean 的取舍

java-developer 的 `beans-vs-records.md` 建议明确选择 record 或 Bean，并保持一致。Spring Boot 应用的划分并非随意，框架已经决定了主要边界：

| 类型 | 形式 | 原因 |
|---|---|---|
| HTTP 请求 / 响应载荷 | **record** | 表示穿过边界的不可变快照；Jackson 原生支持绑定 record |
| 命令 / 查询 / 结果 | **record** | 传入或返回 Service 的值 |
| 领域事件 | **record** | 已发生的事实，不能被修改 |
| 值对象（含 ID） | **record** | 在紧凑构造器中校验 |
| `@ConfigurationProperties` | **record** | 构造器绑定，配置不可变 |
| Projection / 读取模型 | **record** 或接口 | Spring Data 均支持 |
| `@Embeddable` | **record** | Hibernate 6+ 支持；Jakarta Persistence 3.2 中注解可选 |
| **`@Entity`** | **class** | 可变、基于身份，需要无参构造器和非 final 字段以支持代理；record 不能用作实体 |
| **Spring Bean**（`@Service`、`@Component`、`@Configuration`） | **class** | 持有协作者，并且必须可代理 |

这是 java-developer 所说的“明确划分”方案，不是半套采用：凡是跨越边界的数据都使用 record，框架负责的两类对象则使用 class。

实体的规则还要与 java-developer 的数据导向建议一并理解：record 组件应是简单类型，不能持有协作者；Spring Bean 只持有协作者，这正是它应使用 class 的原因。

## 不使用 Lombok

直接编写代码，每种注解都有直接替代方式：

| Lombok | 替代方式 |
|---|---|
| `@RequiredArgsConstructor` | 显式构造器，参见[代理与依赖注入](spring-proxies-and-di.zh-CN.md) |
| DTO 上的 `@Getter` | record，自带访问器 |
| 实体上的 `@Getter` | 只为调用方确实需要的字段显式编写访问器 |
| `@Setter` | 不需要 Setter；定义有含义的操作，例如 `order.cancel()`，而非 `order.setStatus(CANCELLED)` |
| `@Data` | 不使用；它会生成 Setter 和基于可变字段的 `equals`，实体上尤其危险，参见[Spring Data JPA](spring-data-jpa.zh-CN.md) |
| Lombok 的 `@Value` | record |
| `@Builder` | 为合理的创建路径定义静态工厂；如果类型确实需要 Builder，可按 java-developer 的建议考虑生成式不可变 Bean |
| `@Slf4j` | `private static final Logger log = LoggerFactory.getLogger(OrderService.class);` |
| `@SneakyThrows` | 处理异常，或包装为领域异常 |
| `@EqualsAndHashCode` | 使用 record 或不写；实体规则见[Spring Data JPA](spring-data-jpa.zh-CN.md) |

```java
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

多写三行，少用两个注解，也不需要注解处理器修改 AST。

如果项目已经使用 Lombok，**应遵循周边代码并提出迁移建议**；同一个文件只转换一半，比统一采用任一种风格都差。移除 Lombok 应单独作为一次变更提交。

## Spring 边界处的 Optional

java-developer 的规则是：`Optional` 用于公共返回类型，不用于参数和字段。Spring 还有三条具体规则。

**Repository 返回 `Optional`。** Spring Data 原生支持：

```java
Optional<Order> findByReference(String reference);

default Order getByReference(String reference) {
    return findByReference(reference)
            .orElseThrow(() -> new OrderNotFoundException(reference));
}
```

将 `getBy…` 默认方法放在 `findBy…` 旁边是值得采用的惯用法：需要记录的调用方得到异常，并由[错误处理](error-handling.zh-CN.md)映射为 404；能处理不存在情况的调用方直接使用 `Optional`。

**构造器参数是例外。** `Optional<T>` 构造器参数是 Spring 表示“可选依赖”的协议，这是获准的唯一例外（参见[代理与依赖注入](spring-proxies-and-di.zh-CN.md)）。

**载荷 record 组件绝不使用 Optional。** Jackson 对 `Optional` 的序列化不理想，最终仍会产生可空字段。应使用可空组件并标注，或用 sealed type 表达缺失状态。

## Spring 代码中的 var

遵循 java-developer 的建议使用 `var`，只有以下情况应显式保留类型：

```java
var order = orderRepository.getById(id);              // 明显
var response = OrderResponse.from(order);             // 明显

// 框架类型的结构需要清楚时保留类型
ResponseEntity<OrderResponse> entity = ResponseEntity.created(uri).body(response);
```

绝不要在 `@Bean` 方法的返回类型上使用 `var`：声明的返回类型就是注册的 Bean 类型，推断结果会改变容器发布的类型。

## 用 Sealed Type 表示领域状态

领域存在有限状态集合且不同状态携带不同数据时，按 java-developer 的方式建模，让编译器验证所有情况都得到处理：

```java
public sealed interface PaymentOutcome {
    record Authorized(String reference, Money amount) implements PaymentOutcome {}
    record Declined(String reasonCode) implements PaymentOutcome {}
    record RequiresAction(URI redirect) implements PaymentOutcome {}
}
```

```java
return switch (paymentGateway.charge(cmd)) {
    case Authorized(var reference, var amount) -> ResponseEntity.ok(new PaidResponse(reference, amount));
    case Declined(var reasonCode)              -> throw new PaymentDeclinedException(reasonCode);
    case RequiresAction(var redirect)          -> ResponseEntity.status(HttpStatus.SEE_OTHER).location(redirect).build();
};
```

注意：**Jackson 没有额外配置时无法序列化 sealed 层次结构。** 在 Service 和 Controller 间使用 sealed 类型表达内部结果，在边界处映射为扁平响应 record；或有意配置多态处理。参见[JSON 与 Jackson](json-and-jackson.zh-CN.md)。

不要将 sealed 层次结构用于 JPA 实体继承；Hibernate 需要代理并派生子类。

## 使用文本块编写 JPQL 和 SQL

```java
@Query("""
        select o
        from Order o
        where o.status = :status
        order by o.placedAt desc, o.id desc
        """)
List<Order> findRecentByStatus(OrderStatus status, Limit limit);
```

java-developer 的警告在此完全适用：**绝不要将值拼接到查询中。** `"... where o.status = '" + status + "'"` 会造成 JPQL 注入。始终绑定参数，原生 SQL 和 `JdbcClient` 同样如此。

## 不可变性与 final

java-developer 倾向使用 `final` class。Spring 有两个例外：

| 类型 | 是否 `final` |
|---|---|
| DTO、值对象、事件、命令（record） | 天生 final |
| `@Entity` | **否**——Hibernate 需要继承它以创建代理 |
| 被增强的 Spring Bean | **否**——CGLIB 需要继承它；参见[代理与依赖注入](spring-proxies-and-di.zh-CN.md) |
| 其他所有类型 | 是 |

Bean 的*字段*仍应是 `final`；构造器注入可以轻松做到这一点。

## 使用 Spring Boot 3.5.x 时

record / Bean 划分、Lombok 替代方式、`Optional` 用法、`var`、sealed type 和 `final` 例外都相同。两个差异在其他文档中说明：使用 Jackson 2 而非 Jackson 3（参见[JSON 与 Jackson](json-and-jackson.zh-CN.md)）；3.5.x 中 record 作为 `@Embeddable` **必须**标注该注解，因为还没有 Jakarta Persistence 3.2 的隐式支持（参见[Spring Data JPA](spring-data-jpa.zh-CN.md)）。

## 常见问题

- 把 JPA 实体写成 record——不可能；实体需要无参构造器以及可变、非 final 字段。
- DTO 使用带 Getter 和 Setter 的 class——应使用 record。
- 生成 Lombok 注解——参见上面的替代表。
- DTO 手写 `equals` / `hashCode`——使用 record；实体规则见[Spring Data JPA](spring-data-jpa.zh-CN.md)。
- 将 `@Entity` 或被增强的 `@Service` 标记为 `final`——代理会失效。
- `@Bean` 方法返回类型使用 `var`——声明类型就是注册的 Bean 类型。
- 在请求或响应 record 中放 `Optional`——Jackson 处理不理想，应使用可空组件。
- Repository 查询缺失时返回 `null`——返回 `Optional`，并提供抛异常的 `getBy…` 默认方法。
- 在 `@Query` 文本块中拼接值——应绑定参数。
- Controller 直接返回 sealed interface——Jackson 无法有效序列化；应映射为扁平响应 record。

## 相关文档

- [代理与依赖注入](spring-proxies-and-di.zh-CN.md) · [Spring Data JPA](spring-data-jpa.zh-CN.md) · [JSON 与 Jackson](json-and-jackson.zh-CN.md) · [校验](validation.zh-CN.md) · [领域建模](domain-modelling.zh-CN.md)
