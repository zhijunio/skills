# REST Controller

Controller 只负责把 HTTP 转换为 Service 调用，再把结果转换回 HTTP。除此之外不做其他事情。

```java
@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        var orderId = orderService.place(request.toCommand());
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(orderId.value()).toUri();
        return ResponseEntity.created(location).body(orderService.findById(orderId));
    }

    @GetMapping("/{id}")
    OrderResponse getById(@PathVariable OrderId id) {
        return orderService.findById(id);
    }

    @GetMapping
    Page<OrderResponse> list(@RequestParam(required = false) OrderStatus status, Pageable pageable) {
        return orderService.find(status, pageable);
    }
}
```

Controller 和构造器都可以是包级可见，因为功能包外没有代码需要调用它们；Spring 也不要求它们是 `public`。

## 规则

- **每个端点只调用一次 Service。** Controller 中调用两次就开始承担编排职责；编排应放在可定义事务的 Service 中（参见[事务](transactions.zh-CN.md)）。
- **绝不接收或返回 `@Entity`。** 入参使用 `*Request` record，出参使用 `*Response` record。返回实体会把持久化模型变成 API 契约，在事务之外触发懒加载，并泄漏不打算公开的列。
- **不使用 `try` / `catch`。** 异常交给 `@RestControllerAdvice`（参见[错误处理](error-handling.zh-CN.md)）。
- **不添加 `@Transactional`。** 事务应放在 Service 上。
- **`HttpServletRequest` 不应越过 Controller。** 如果 Service 需要调用者信息，应传入所需的值，而不是整个请求对象。
- 状态为 200 且无需自定义响应头时，**直接返回响应内容**。需要 201 和 `Location`、自定义响应头或运行时确定状态码时，再使用 `ResponseEntity`。

## URL 约定

- 使用复数名词：`/orders`、`/customers`。
- 多词路径使用 kebab-case：`/order-items`，不要用 `/orderItems`。
- 嵌套最多两层：`/orders/{id}/items` 可以；`/orders/{id}/items/{itemId}/notes` 不行，应提升为 `/order-item-notes/{id}`。
- 路径中使用不透明 ID，例如 UUID 或业务引用。不要暴露自增整数，它会泄漏行数并方便枚举。
- 只有真正的非 CRUD 状态转换才使用动词：`POST /orders/{id}/cancel` 优于用带特殊状态值的 `PATCH`。

```
GET    /api/orders              列表，分页
POST   /api/orders              创建
GET    /api/orders/{id}         查询单项
PUT    /api/orders/{id}         完整替换
PATCH  /api/orders/{id}         部分更新
DELETE /api/orders/{id}         删除
POST   /api/orders/{id}/cancel  状态转换
```

Spring Boot 4 的 API 版本管理不是通过 URL 约定实现的，参见 [API 版本管理](api-versioning.zh-CN.md)。

路径用于标识资源或真正的状态转换子资源。过滤、搜索、分页和白名单排序字段放在查询参数中；创建/更新数据放在请求体中。路由名不要依赖表示格式，也不要将凭据或敏感值放入 URL，因为 URL 常会被记录和缓存。

实现应遵守 HTTP 方法语义：`GET` 不得更改状态；`PUT` 替换资源表示且应具备幂等性；`PATCH` 必须说明 Patch 格式，以及重复执行是否安全；`POST` 本身不保证幂等。客户端需要安全重试非幂等命令时，应定义幂等键契约，不要通过修改资源路径实现。参见[API 幂等性](api-idempotency.zh-CN.md)。

## 状态码

| 情况 | 状态码 |
|---|---|
| 读取成功 | 200 |
| 已创建资源 | 201，并返回 `Location` 头 |
| 已接受异步处理 | 202 |
| 成功且无内容可返回 | 204 |
| 请求格式错误或无效 | 400 |
| 未认证 | 401 |
| 已认证但无权限 | 403 |
| 资源不存在 | 404 |
| 冲突——重复数据或乐观锁失败 | 409 |
| 语法正确但违反业务规则 | 422 |
| 未处理错误 | 500 |

400 与 422 的区别值得正确处理：400 表示“无法解析或绑定”，422 表示“已理解请求，但领域规则拒绝”。校验失败映射为 400，领域规则违规映射为 422。

## 绑定值对象 ID

对于值对象 ID（参见 [Spring Data JPA](spring-data-jpa.zh-CN.md)），为每种 ID 类型注册一个 `Converter`，这样 `@PathVariable OrderId id` 就能直接绑定：

```java
@Component
class StringToOrderIdConverter implements Converter<String, OrderId> {
    @Override
    public OrderId convert(String source) {
        return OrderId.of(source);
    }
}
```

格式错误的 ID 会由转换器统一抛出并集中映射，无需在每个处理器里重复解析。请求**体**则由 Jackson 通过 record 上的 `@JsonValue` / `@JsonCreator` 处理（参见 [JSON 与 Jackson](json-and-jackson.zh-CN.md)）。

## 分页

接收 `Pageable`，返回由**响应 record** 构成的 `Page<T>`，绝不要返回实体分页：

```java
@GetMapping
Page<OrderResponse> list(Pageable pageable) {
    return orderService.findAll(pageable);   // Service 将实体映射为响应
}
```

**限制页面大小。** 直接使用 `Pageable` 会允许任何调用方发送 `?size=100000`，单个请求就可能把整张表载入内存。Spring Boot 默认上限也可能造成负担：

```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100      # 超出部分会被截断，而不是拒绝
```

排序格式是 `?sort=placedAt,desc`。如果客户端可以指定任意属性，Service 应限制可排序字段；否则客户端可以通过未建索引的字段强制全表扫描。

深分页应改用 Keyset 分页，参见 [Spring Data JPA](spring-data-jpa.zh-CN.md)。

## 内容协商与响应头

- 只有端点确实提供 JSON 以外的格式，或需要返回明确 415 而不是令人困惑的绑定错误时，才设置 `produces` / `consumes`。
- 长时间运行的工作应返回 **202**，并在 `Location` 中提供状态查询资源。不要让请求一直等待。
- CORS 应集中配置在 `config/` 中的 `WebMvcConfigurer`，不要在 Controller 上散布 `@CrossOrigin`。

## 使用 Spring Boot 3.5.x 时

Controller 代码不变。两个区别：

- JSON 使用 Jackson 2（`com.fasterxml.jackson`），参见 [JSON 与 Jackson](json-and-jackson.zh-CN.md)。
- 不支持原生 API 版本管理，3.5.x 的方案见 [API 版本管理](api-versioning.zh-CN.md)。

## 常见问题

- 处理器返回 `@Entity` 或 `Page<Entity>`——应映射为响应 record；实体序列化会泄漏数据库结构，并在事务外触发懒加载。
- 请求体直接绑定到实体——使用 `*Request` record，再转换为命令。
- Controller 注入 Repository——应通过 Service 访问。
- 在 Controller 上使用 `@Transactional`——事务属于 Service。
- 在处理器里编写 `try` / `catch`——使用 `@RestControllerAdvice`。
- 使用没有上限的 `Pageable`——设置 `spring.data.web.pageable.max-page-size`。
- URL 暴露 `Long` 自增 ID——应使用 UUID 或业务引用。
- 创建操作返回 200——应返回带 `Location` 头的 201。
- 返回 200 并在响应体中放错误对象——应使用 HTTP 状态码（参见[错误处理](error-handling.zh-CN.md)）。
- 所有响应都包装为 `ResponseEntity`——除非需要非默认状态或响应头，否则直接返回内容。
- 资源嵌套三层——提升为顶层资源。
- 在 Controller 上散布 `@CrossOrigin`——通过 `WebMvcConfigurer` 集中配置 CORS。
- 在处理方法中自行从路径字符串解析 ID——注册 `Converter` 并直接绑定值对象。

## 相关文档

- [错误处理](error-handling.zh-CN.md) · [校验](validation.zh-CN.md) · [JSON 与 Jackson](json-and-jackson.zh-CN.md) · [API 版本管理](api-versioning.zh-CN.md) · [API 幂等性](api-idempotency.zh-CN.md) · [限流与资源保护](rate-limiting.zh-CN.md) · [Spring Data JPA](spring-data-jpa.zh-CN.md) · [Web 切片测试](testing-slices-web.zh-CN.md)
