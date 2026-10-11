# JSON 与 Jackson

Spring Boot 4 使用 **Jackson 3**。这是大量“看起来正确却无法编译”的代码的来源。

## 哪些内容迁移，哪些保持不变

| 项目 | Jackson 2（Boot 3.5） | Jackson 3（Boot 4） |
|---|---|---|
| Core、databind | `com.fasterxml.jackson.core` / `.databind` | **`tools.jackson.core` / `.databind`** |
| Mapper 类型 | `ObjectMapper` | **`JsonMapper`**（`tools.jackson.databind.json.JsonMapper`） |
| Boot 注解 | `@JsonComponent` | **`@JacksonComponent`** |
| Boot 定制器 | `Jackson2ObjectMapperBuilderCustomizer` | **`JsonMapperBuilderCustomizer`** |
| Databind 注解——`@JsonSerialize`、`@JsonDeserialize` | `com.fasterxml.jackson.databind.annotation` | **`tools.jackson.databind.annotation`** |
| **Core 注解——`@JsonValue`、`@JsonCreator`、`@JsonProperty`、`@JsonIgnore`、`@JsonInclude`、`@JsonFormat`** | `com.fasterxml.jackson.annotation` | **不变** |
| 配置属性 | `spring.jackson.*` | `spring.jackson.*` 配置 Jackson 3；Jackson 2 的属性迁移至 `spring.jackson2.*` |

最后两行是最容易写错的地方。`jackson-annotations` 刻意保留了 group id 和包名，因此 DTO 上的注解**不需要**更改 import；但属于 `databind` 的两个注解确实迁移了。把所有 `com.fasterxml.jackson` import 都改为 `tools.jackson` 会导致构建失败。

## 不要自行声明 ObjectMapper Bean

Spring Boot 会自动配置 Mapper。自行声明 Bean 会替换默认配置并静默丢失所有 Boot 默认行为，包括 `spring.jackson.*` 属性和 Boot 注册的模块。

```java
// ✅ 定制
@Bean
JsonMapperBuilderCustomizer jsonCustomizer() {
    return builder -> builder.changeDefaultPropertyInclusion(
            incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL));
}
```

```java
// ❌ 完全替换 Boot 的 Mapper
@Bean
JsonMapper jsonMapper() {
    return JsonMapper.builder().build();
}
```

很多需要定制器的场景，其实只需配置属性：

```yaml
spring:
  jackson:
    default-property-inclusion: non_null
    deserialization:
      fail-on-unknown-properties: false
```

只有确实需要手动序列化时才注入 `JsonMapper`。Boot 4 中不要注入 `ObjectMapper`。

## Record 可以直接序列化

无需注解，也无需编写构造器：

```java
public record OrderResponse(
        UUID id,
        String status,
        BigDecimal total,
        Instant placedAt,
        List<OrderLineResponse> lines
) {}
```

反序列化会使用规范构造器。DTO 使用 record 的主要实际理由就是这一点（参见[Spring 中的 Java](java-in-spring.zh-CN.md)）。

只有线上的 JSON 字段名与组件名称不同时才需要 `@JsonProperty`：

```java
public record CustomerResponse(UUID id, @JsonProperty("full_name") String fullName) {}
```

如果整个 API 都使用 snake_case，优先配置全局命名策略，而不是逐字段注解：

```yaml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

## 在线上传输值对象

值对象 ID 在 JSON 中应表现为底层标量，而不是嵌套对象。下面两个注解都来自未迁移的 `com.fasterxml.jackson.annotation` 包：

```java
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record OrderId(@JsonValue UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value");
    }

    @JsonCreator
    public static OrderId of(String value) {
        return new OrderId(UUID.fromString(value));
    }

    public static OrderId of(UUID value) {
        return new OrderId(value);
    }
}
```

序列化结果是 `"9f2c…"`，而非 `{"value":"9f2c…"}`。没有 `@JsonValue` 时，API 中每种值对象都会泄漏一个包装对象。

`@JsonValue` / `@JsonCreator` 负责处理**请求体**。路径变量和查询参数则通过 Spring `Converter` 处理（参见[REST Controller](rest-controllers.zh-CN.md)）。两者缺一不可，因为机制不同。

## 日期和时间

Boot 4 中，Jackson 3 的 Java 时间类型序列化器默认使用 ISO-8601 字符串。不要把 Jackson 2 时代的 `write-dates-as-timestamps` 复制到 `spring.jackson.serialization`：Jackson 3 已不再提供该 `SerializationFeature`，Boot 4 可能在绑定该配置时启动失败。如果确实需要非默认日期格式，应显式配置 Jackson 3 序列化器，并用 JSON 测试覆盖。Boot 3.5.x 仍可在需要时使用该属性。

- **表示一个时间点时使用 `Instant`。** 序列化为 `2026-08-10T09:15:30Z`，适用于 `createdAt`、`placedAt` 等事件时间。
- **没有时间的日期使用 `LocalDate`**，例如生日和开票日期。
- **避免在线上传输 `LocalDateTime`。** 它没有时区，客户端无法确定对应的时间点，是跨时区出现一小时偏差问题的最常见原因。
- **不要注册 `JavaTimeModule`。** Jackson 3 原生支持 `java.time`；Jackson 2 也由 Spring Boot 自动注册该模块，因此一直无需手动添加。

## Null 与字段缺失

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(UUID id, String status, String cancellationReason) {}
```

也可以通过 `default-property-inclusion` 全局配置。应在整个 API 中统一选择：有些响应省略 null 字段、有些却显式返回 `null`，会增加客户端处理难度。

对 `PATCH` 来说，字段缺失和设置为 `null` 含义不同，`NON_NULL` 无法区分。使用 `JsonNullable`（依赖 `org.openapitools:jackson-databind-nullable`）或显式建模补丁请求，不要假装两者没有区别。

## 未知属性

Spring Boot 默认关闭 `FAIL_ON_UNKNOWN_PROPERTIES`，遇到意外字段会忽略。对于需要宽容地读取第三方 API 的场景，这很合适。但对于**自己的入站请求**，可以考虑启用：静默忽略拼错字段会让调用方误以为已成功设置该值。

```yaml
spring:
  jackson:
    deserialization:
      fail-on-unknown-properties: true
```

## 多态类型

没有配置时，Jackson 无法实用地序列化 sealed 层次结构。可以在边界处映射为扁平响应 record（推荐，参见[Spring 中的 Java](java-in-spring.zh-CN.md)），也可以显式配置：

```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CardPayment.class, name = "card"),
        @JsonSubTypes.Type(value = BankTransfer.class, name = "transfer")
})
public sealed interface PaymentMethod permits CardPayment, BankTransfer {}
```

**绝不要在入站 Mapper 上启用默认类型信息。** 它会反序列化载荷中任意命名的类，形成远程代码执行风险。只有两端都由自己控制的缓存才可能合理启用默认类型信息，并且必须限制到自己的包（参见[缓存](caching.zh-CN.md)）。

## 自定义序列化器

```java
@JacksonComponent
public class MoneySerializer extends ValueSerializer<Money> { … }
```

`@JacksonComponent`（Boot 4；3.5.x 使用 `@JsonComponent`）会自动注册。编写自定义序列化器之前，先检查类型上的 `@JsonValue` 是否足够——通常足够。

## 使用 Spring Boot 3.5.x 时

全程使用 Jackson 2：`com.fasterxml.jackson.databind.ObjectMapper`、`@JsonComponent`、`Jackson2ObjectMapperBuilderCustomizer`，以及配置 Jackson 2 的 `spring.jackson.*`。本文其他内容——record、`@JsonValue`、日期处理、字段包含规则、未知属性——都相同。Boot 会注册 `JavaTimeModule`，仍然不需要手动添加。

## 常见问题

- 为定制 JSON 声明 `ObjectMapper` 或 `JsonMapper` Bean——这会替换 Boot Mapper 并丢失默认配置；应使用 `JsonMapperBuilderCustomizer` 或属性。
- Boot 4 中注入 `ObjectMapper`——Bean 类型是 `JsonMapper`。
- 把所有 `com.fasterxml.jackson` import 改成 `tools.jackson`——`jackson-annotations` 未迁移，`@JsonValue`、`@JsonCreator`、`@JsonProperty`、`@JsonIgnore`、`@JsonInclude` 包名保持不变。
- `@JsonSerialize` / `@JsonDeserialize` 仍使用旧包——它们属于 `databind`，已迁移到 `tools.jackson.databind.annotation`。
- Boot 4 使用 `@JsonComponent`——应使用 `@JacksonComponent`。
- 注册 `JavaTimeModule`——两个版本线都不需要；Jackson 3 也没有同样形式的模块。
- 序列化值对象时不加 `@JsonValue`——API 会得到 `{"value":"…"}`，而不是标量。
- 添加 `@JsonValue` 后期望路径变量自动绑定——仍需注册 `Converter`。
- 用 `LocalDateTime` 表示时间戳——它没有时区，应使用 `Instant`。
- 将 `write-dates-as-timestamps` 复制到 Boot 4 的 `spring.jackson.serialization`——Jackson 3 不支持该特性；默认已使用 ISO-8601。
- 在请求 Mapper 上启用默认类型信息——这会引入反序列化 gadget 风险。
- Controller 返回 sealed interface——应配置 `@JsonTypeInfo`，或映射为扁平 record。
- 为 `@JsonValue` 已能处理的类型编写自定义序列化器。
- 在 Boot 4 查找 Jackson 2 的配置属性——它们已迁移至 `spring.jackson2.*`。

## 相关文档

- [REST Controller](rest-controllers.zh-CN.md) · [Spring 中的 Java](java-in-spring.zh-CN.md) · [缓存](caching.zh-CN.md) · [版本与升级](boot-versions.zh-CN.md) · [Web 切片测试](testing-slices-web.zh-CN.md)
