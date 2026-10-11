# 校验

校验规则分为三类，放错位置会导致重复校验，或让无效对象进入系统。

| 规则类型 | 所在位置 | 执行者 |
|---|---|---|
| **请求结构**——必填、长度、格式、范围 | `*Request` record 上的 Jakarta Validation 注解，Controller 参数上的 `@Valid` | Spring，在业务代码运行前 |
| **值对象不变量**——金额不能为负、订单 ID 不能为空 | record 的紧凑构造器 | 类型本身；无效实例无法创建 |
| **聚合不变量**——已提交订单不能再添加商品 | 实体上的行为方法 | 领域对象，在变更发生时检查 |

区别在于状态：前两类独立检查单个值，可以在加载任何数据前执行；第三类需要聚合的当前状态，因此应放在拥有该状态的对象中。

这是把 java-developer 提倡的“在边界校验，信任内部”应用于 Spring。前两项属于边界校验；第三项不是重复校验，而是此前无法执行的另一条规则。

## 请求校验

```java
public record CreateOrderRequest(

        @NotBlank
        @Email
        String customerEmail,

        @NotEmpty
        @Size(max = 100, message = "an order may contain at most 100 lines")
        @Valid
        List<OrderLineRequest> items,

        @Size(max = 500)
        String notes
) {
    public PlaceOrderCmd toCommand() {
        return new PlaceOrderCmd(
                EmailAddress.of(customerEmail),
                items.stream().map(OrderLineRequest::toCommand).toList(),
                notes);
    }
}

public record OrderLineRequest(
        @NotNull UUID productId,
        @Positive int quantity
) { … }
```

```java
@PostMapping
ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) { … }
```

- **参数上的 `@Valid` 会触发校验。** 没有它，所有约束都不会执行，这是最常见的校验错误。
- **嵌套集合上的 `@Valid` 会级联到元素。** 单独的 `@NotEmpty` 只检查列表本身，不检查列表内容。
- **校验请求 record，而不是实体。** 创建实体前，请求绑定已经完成。
- 默认错误消息对 API 用户不清楚时，应设置 `message`。`@Size(max = 100)` 默认会说“大小必须在 0 到 100 之间”，这不是调用方真正需要的信息。

失败会表现为 `MethodArgumentNotValidException`，并转成 RFC 9457 `violations` 数组（参见[错误处理](error-handling.zh-CN.md)）。

### 选择校验注解

| 意图 | 注解 |
|---|---|
| 必须存在且不能只有空白字符（`String`） | `@NotBlank` |
| 必须存在且非空（集合、Map、数组、`String`） | `@NotEmpty` |
| 必须存在（任何类型，包括 `false` 和 `0`） | `@NotNull` |
| 数值范围 | `@Positive`、`@PositiveOrZero`、`@Min`、`@Max`、`@DecimalMin`、`@DecimalMax` |
| 金额和精度 | `@Digits(integer = 10, fraction = 2)` |
| 日期 | `@Past`、`@PastOrPresent`、`@Future`、`@FutureOrPresent` |
| 正则格式 | `@Pattern(regexp = …)` |

`String` 上的 `@NotNull` 接受 `""` 和 `"   "`。文本应使用 `@NotBlank`。

## 值对象不变量

```java
public record EmailAddress(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    public EmailAddress {
        Objects.requireNonNull(value, "value");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Not a valid email address: " + value);
        }
    }

    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }
}
```

有了这个类型，任何接收 `EmailAddress` 的方法都不必重复检查。这就是收益，也是 java-developer 建议让类型承载校验规则的原因。

两条规则：

- 紧凑构造器中应抛出 `IllegalArgumentException`，而不是领域异常。它表示创建对象时出现编程错误；如果调用方可能触发，应在边界处转换异常。
- **不要以为值对象上的 Jakarta Validation 注解会自动执行。** 只有调用校验器时才会触发，例如标有 `@Valid` 的参数或 `@Validated` Bean。直接构造的值对象不会自动校验。紧凑构造器才能保证不变量。

## 聚合不变量

```java
void addItem(ProductId productId, int quantity, Money unitPrice) {
    if (status != OrderStatus.DRAFT) {
        throw new OrderNotModifiableException(id, status);
    }
    items.add(OrderItem.create(this, productId, quantity, unitPrice));
    recalculateTotal();
}
```

该规则依赖当前状态，只能在这里检查。应抛出 `DomainException` 子类；此例映射为 422 而不是 400，因为请求格式正确，只是领域规则拒绝了操作。

不要在实体上使用 Jakarta Validation 注解执行此规则。实体约束会在 flush 时触发，时机不对，而且会从持久化层深处抛出 `ConstraintViolationException`，而不是清晰的领域错误。

## 自定义约束

同一规则在多个请求中重复出现时，可以创建自定义约束：

```java
@Documented
@Constraint(validatedBy = SupportedCurrencyValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedCurrency {
    String message() default "unsupported currency";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

class SupportedCurrencyValidator implements ConstraintValidator<SupportedCurrency, String> {

    private final CurrencyRegistry registry;   // 可注入依赖；校验器是 Spring Bean

    SupportedCurrencyValidator(CurrencyRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || registry.isSupported(value);   // null 由 @NotNull 负责
    }
}
```

- `@Target` 中必须包含 `ElementType.RECORD_COMPONENT`，否则无法用于 record。
- **对 `null` 返回 `true`。** 是否组合 `@NotNull` 由调用方决定。校验器拒绝 null 会重复该职责，并产生两条错误消息。
- 校验器是 Spring Bean，可以注入依赖；不要在热点路径上执行数据库查询。

### 跨字段规则

约束应标在类型上，而不是某个组件上：

```java
@Target(ElementType.TYPE)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange { … }

@ValidDateRange
public record ReportRequest(@NotNull LocalDate from, @NotNull LocalDate to) {}
```

使用 `context.buildConstraintViolationWithTemplate(…).addPropertyNode("to").addConstraintViolation()` 将消息关联到具体字段，使 `violations` 数组更有用。

## 校验方法参数

`@RequestBody` 上的 `@Valid` 由参数解析器处理。要校验 **Service** 方法参数，Bean 必须添加 `@Validated`：

```java
@Service
@Validated
public class OrderService {

    public OrderId place(@Valid PlaceOrderCmd cmd) { … }
}
```

此功能基于代理，同一对象内部自调用时不会执行校验（参见[代理与依赖注入](spring-proxies-and-di.zh-CN.md)）。失败会抛出 `ConstraintViolationException`，而不是 `MethodArgumentNotValidException`，因此需要单独的异常处理器。

优先使用值对象，而不是在 Service 上添加 `@Validated`。如果命令中的组件已经是校验过的类型，就没有剩余规则需要检查。

## 校验配置

配置错误应在启动时暴露，而非首次使用时才发现：

```java
@Validated
@ConfigurationProperties("app.orders")
public record OrderProperties(
        @NotNull @Positive Integer maxLinesPerOrder,
        @NotBlank String defaultCurrency,
        @NotNull Duration reservationTimeout
) {}
```

参见[配置](configuration.zh-CN.md)。

## 校验组

字段在更新时必填、创建时可选时，可以使用校验组：

```java
public interface OnCreate {}
public interface OnUpdate {}

public record SaveCustomerRequest(
        @Null(groups = OnCreate.class) @NotNull(groups = OnUpdate.class) UUID id,
        @NotBlank String name
) {}

@PutMapping("/{id}")
ResponseEntity<?> update(@Validated(OnUpdate.class) @RequestBody SaveCustomerRequest request) { … }
```

此处使用 `@Validated(Group.class)`，不能用不接受组参数的 `@Valid`。只有创建和更新确实不对称时，校验组才有价值；通常定义两个独立请求 record 更清晰。

## 使用 Spring Boot 3.5.x 时

行为相同。两个版本线都使用 Jakarta Validation（`jakarta.validation.*`）；Boot 4 管理的库版本更新，但注解、`@Validated` 和自定义约束 API 均未改变。两个版本都需要 `spring-boot-starter-validation` 提供 `jakarta.validation`；Web Starter 不会传递引入它。

## 常见问题

- 添加约束注解，却忘记在参数上添加 `@Valid`——校验不会执行。
- 嵌套集合或对象缺少 `@Valid`——`@NotEmpty` 只检查列表，不检查元素。
- 对 `String` 使用 `@NotNull`——空字符串和空白字符仍然通过；应使用 `@NotBlank`。
- 给值对象 record 添加约束并假设会执行——应在紧凑构造器校验。
- 在实体上放业务规则约束——它们会在 flush 时触发；应在行为方法中检查。
- 自定义校验器拒绝 `null`——应返回 `true`，由 `@NotNull` 负责。
- 自定义约束的 `@Target` 忘记 `ElementType.RECORD_COMPONENT`——无法用于 record。
- 在 Controller 中编写 `if` 校验——应注解请求 record。
- 在紧凑构造器中抛出 `DomainException`——这里应抛出 `IllegalArgumentException`。
- 认为 Service 上的 `@Validated` 对自调用有效——它基于代理，自调用不会生效。
- 只返回第一条违反规则——应报告全部违规项。
- 忘记 `spring-boot-starter-validation`——约束 API 不会进入 classpath。
- 对校验组使用 `@Valid`——`@Valid` 不接受组参数，应使用 `@Validated(Group.class)`。

## 相关文档

- [REST Controller](rest-controllers.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [领域建模](domain-modelling.zh-CN.md) · [配置](configuration.zh-CN.md) · [Spring 中的 Java](java-in-spring.zh-CN.md)
