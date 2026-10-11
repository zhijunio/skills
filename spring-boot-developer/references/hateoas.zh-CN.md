# HATEOAS

超媒体的成本只有在一种情况下值得付出：**客户端可执行的操作取决于服务器状态，且客户端无法自行计算该状态。** 例如，订单处于 `PENDING` 时可取消，发货后不可取消。如果服务器只在允许取消时提供 `cancel` 链接，客户端就无需重新实现这条规则。

如果客户端只是一个已经了解状态机的单团队 SPA，链接就只是仪式。返回普通 DTO（参见 [REST Controller](rest-controllers.zh-CN.md)），等有第二个消费者时再考虑超媒体。

要么在整个 API 中采用 HATEOAS，要么完全不采用。只有一半端点返回 `_links`，比完全没有更糟。

## 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

添加依赖后，`RepresentationModel` 类型的默认响应内容类型会变为 HAL（`application/hal+json`），普通 DTO 不受影响。

## 使用 Assembler，而不是 Model 工厂方法

将链接构建放进 `RepresentationModelAssembler`，不要放在响应 record 的静态方法或 Controller 中。Assembler 是 Bean，可以注入依赖，也能在返回同类资源的多个 Controller 之间复用。

```java
@Component
class OrderModelAssembler
        implements RepresentationModelAssembler<Order, EntityModel<OrderResponse>> {

    @Override
    public EntityModel<OrderResponse> toModel(Order order) {
        var model = EntityModel.of(
                OrderResponse.from(order),
                linkTo(methodOn(OrderController.class).getById(order.id())).withSelfRel(),
                linkTo(methodOn(OrderController.class).list(null)).withRel("orders"));

        // 只有当前允许该操作时才添加操作链接
        if (order.status() == OrderStatus.PENDING) {
            model.add(linkTo(methodOn(OrderController.class).cancel(order.id())).withRel("cancel"));
        }
        if (order.status() == OrderStatus.PROCESSING) {
            model.add(linkTo(methodOn(OrderController.class).ship(order.id())).withRel("ship"));
        }
        return model;
    }
}
```

`EntityModel.of(dto, links…)` 可以包装已有响应 record，优于继承 `RepresentationModel`：后者会强迫 DTO 变成可变类，无法继续使用 record。

条件链接是 HATEOAS 的核心。无论当前状态如何都添加所有链接，只是多绕几步重新实现静态 URL 列表。

## Controller

```java
@GetMapping("/{id}")
EntityModel<OrderResponse> getById(@PathVariable OrderId id) {
    return assembler.toModel(orderService.getById(id));
}

@GetMapping
PagedModel<EntityModel<OrderResponse>> list(
        Pageable pageable,
        PagedResourcesAssembler<Order> pagedAssembler) {

    return pagedAssembler.toModel(orderService.findAll(pageable), assembler);
}
```

`PagedResourcesAssembler` 由 Spring 注入，**不要自行声明 Bean**。它会生成 `first`、`prev`、`next` 和 `last` 链接以及 `page` 元数据。手动构造分页链接总会漏掉边界情况，尤其是首页和末页。

## 构建链接

`linkTo(methodOn(Controller.class).method(args))` 会根据映射解析 URL，因此更改 `@RequestMapping` 后所有链接都会更新。不要拼接 URL 字符串，那会重新引入 HATEOAS 本来要避免的问题。

`methodOn` 会记录一次代理调用，因此处理方法不能是 `final`；不影响路径的参数可以传 `null`。

适合时使用 IANA 注册的关系名，并通过 `IanaLinkRelations` 获取：`self`、`next`、`prev`、`first`、`last`、`collection`、`item`、`edit`。只有领域操作（如 `cancel`、`ship`、`refund`）才应自定义名称。

## 响应结构

```json
{
  "id": "9f2c8b1e-…",
  "status": "PENDING",
  "total": "49.99",
  "_links": {
    "self":   { "href": "https://api.example.com/api/orders/9f2c8b1e-…" },
    "orders": { "href": "https://api.example.com/api/orders" },
    "cancel": { "href": "https://api.example.com/api/orders/9f2c8b1e-…/cancel" }
  }
}
```

每个资源都应带有 `self`。收到资源的客户端无需自行构造 URL，就能重新获取该资源。

应用位于代理或网关之后时，如果没有正确处理转发头，生成链接中的主机名会错误：

```yaml
server:
  forward-headers-strategy: framework
```

未配置时，`linkTo` 会根据内部主机和端口生成链接，客户端收到的链接将无法访问。

## 测试

测试时要断言链接，而不只是字段。例如，Assembler 停止生成 `cancel` 链接属于契约破坏，只有字段断言无法发现：

```java
assertThat(mockMvcTester.get().uri("/api/orders/{id}", id))
        .hasStatusOk()
        .bodyJson()
        .hasPathSatisfying("$._links.self.href", href -> assertThat(href).asString().endsWith(id.toString()))
        .hasPathSatisfying("$._links.cancel.href", Assert::isNotNull);
```

也要覆盖反面情况：已发货订单**不能**包含 `cancel` 链接。

## 使用 Spring Boot 3.5.x 时

Spring HATEOAS 没有变化：`EntityModel`、`CollectionModel`、`PagedModel`、`RepresentationModelAssembler`、`PagedResourcesAssembler` 以及 `linkTo` / `methodOn` 的行为相同。只有 JSON 由 Jackson 2 生成。

## 常见问题

- 只有部分端点使用超媒体，其他端点返回普通 DTO——应统一采用或不采用。
- 不论资源状态如何都输出全部链接——按状态输出操作链接才是 HATEOAS 的价值。
- 硬编码或拼接 URL——使用 `linkTo(methodOn(...))`。
- 手工构造分页链接——使用 `PagedResourcesAssembler`。
- 将 `PagedResourcesAssembler` 声明为 `@Bean`——Spring 会把它作为处理方法参数提供。
- 缺少 `self` 链接——每个资源都需要它。
- 在 Controller 或 DTO 静态工厂中放链接逻辑——使用 Assembler Bean。
- record 继承 `RepresentationModel`——不可行；使用 `EntityModel.of(...)` 包装 record。
- 代理之后忘记配置 `forward-headers-strategy`——生成链接会指向内部主机。
- 只断言响应字段——还应断言存在和必须缺席的链接。

## 相关文档

- [REST Controller](rest-controllers.zh-CN.md) · [OpenAPI](openapi.zh-CN.md) · [JSON 与 Jackson](json-and-jackson.zh-CN.md) · [Web 切片测试](testing-slices-web.zh-CN.md)
