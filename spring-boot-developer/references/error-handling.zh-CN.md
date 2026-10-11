# 错误处理

错误响应应采用 **RFC 9457 Problem Details**。Spring 内置了 `ProblemDetail` 支持，没有必要发明新的错误结构。

RFC 9457 取代了 RFC 7807；媒体类型 `application/problem+json` 和字段名没有变化，因此旧文档中所谓的“RFC 7807 支持”指的也是同一套结构。

## 不要添加外层包装

不要将响应包装成 `{ success, data, error }`。成功时直接返回有效负载，HTTP 状态表示结果，失败时使用 `ProblemDetail`。外层包装会在响应体中重复状态码，迫使每个客户端再拆一层，并破坏缓存和条件请求。

如果继承的代码已经采用包装结构，**就保持一致并继续使用**——成功响应有包装、错误响应却使用 `ProblemDetail`，会迫使消费者处理两种格式，比任何一种单独使用都更糟。

## 启用支持

```yaml
spring:
  mvc:
    problemdetails:
      enabled: true
```

此设置会把 **Spring 自身**的异常（找不到处理器时的 404、405、415、请求体格式错误等）转换为 `ProblemDetail`。它不会处理领域异常，领域异常仍需单独编写处理器。

多个 `@RestControllerAdvice` 都能处理同一异常时，执行顺序很重要。启用 Boot Problem Details advice 后，另一个处理校验或框架异常的 advice 可能排在 Boot advice 之后，最终返回默认响应。需要定制 MVC 异常时，优先继承 `ResponseEntityExceptionHandler` 并覆盖对应钩子。如果必须使用独立 advice，应显式设置更高优先级（例如 `@Order(Ordered.HIGHEST_PRECEDENCE)`），并在 Web 切片测试中核对状态码、Content-Type 和响应体。不要假定 `@ExceptionHandler` 方法仅因异常类型更具体就一定优先。

## 领域异常层次

```java
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}

public class OrderNotFoundException extends DomainException {
    public OrderNotFoundException(OrderId id) {
        super("ORDER_NOT_FOUND", "No order with id " + id.value());
    }
}

public class OrderNotModifiableException extends DomainException {
    public OrderNotModifiableException(OrderId id, OrderStatus status) {
        super("ORDER_NOT_MODIFIABLE", "Order %s cannot be modified in status %s".formatted(id.value(), status));
    }
}
```

领域异常始终使用非受检异常：受检异常会迫使每个中间层都声明它，而且默认不会触发事务回滚（参见[事务](transactions.zh-CN.md)）。稳定的 `errorCode` 供客户端分支处理；给人看的消息则可以变更。

在 Service 或领域对象中抛出异常，不要在 Controller 中抛出。

## 全局处理器

```java
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI BASE = URI.create("https://errors.example.com/");

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(OrderNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource Not Found", ex);
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail handleDomain(DomainException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business Rule Violation", ex);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail handleConflict(OptimisticLockingFailureException ex) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The resource was modified concurrently. Retry the request.");
        problem.setType(BASE.resolve("concurrent-modification"));
        problem.setTitle("Conflict");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setType(BASE.resolve("internal"));
        problem.setTitle("Internal Server Error");
        return problem;
    }

    private ProblemDetail problem(HttpStatus status, String title, DomainException ex) {
        var problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(BASE.resolve(ex.errorCode().toLowerCase().replace('_', '-')));
        problem.setTitle(title);
        problem.setProperty("errorCode", ex.errorCode());
        return problem;
    }
}
```

需要注意：

- **继承 `ResponseEntityExceptionHandler`**，复用整套 Spring MVC 异常处理。如果不继承，就得手动重新实现 405、415 和请求体格式错误处理，通常容易出错。
- **设置 `type`。** RFC 9457 把它作为错误类型的稳定标识；默认值 `about:blank` 表示“错误信息只有状态码”。
- **直接返回 `ProblemDetail`。** Spring 会根据对象设置状态码和 `application/problem+json` 内容类型。只有需要添加响应头时才返回 `ResponseEntity<ProblemDetail>`。
- **`instance` 会根据请求 URI 自动设置。** 从 `HttpServletRequest` 手动赋值是多余的。
- **500 响应绝不包含 `ex.getMessage()`。** 记录日志，向客户端返回通用消息。堆栈和内部错误信息会泄露实现细节。
- **不要添加 `timestamp`。** HTTP `Date` 响应头已经包含时间。

处理器按异常类型的具体程度匹配，而不按声明顺序；因此 `DomainException` 不会遮蔽 `OrderNotFoundException`。

## 校验失败

应覆盖钩子方法，不要自行处理 `MethodArgumentNotValidException`，这样 `ResponseEntityExceptionHandler` 的其他处理逻辑仍然有效：

```java
@Override
protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers,
        HttpStatusCode status, WebRequest request) {

    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
    problem.setType(URI.create("https://errors.example.com/validation"));
    problem.setTitle("Validation Failed");
    problem.setProperty("violations", ex.getBindingResult().getFieldErrors().stream()
            .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
            .toList());
    return ResponseEntity.badRequest().body(problem);
}
```

```json
{
  "type": "https://errors.example.com/validation",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed.",
  "instance": "/api/orders",
  "violations": [
    { "field": "customerEmail", "message": "must be a well-formed email address" },
    { "field": "items", "message": "must not be empty" }
  ]
}
```

报告**所有**违规项，而不仅是第一项。参见[校验](validation.zh-CN.md)。

## 按功能划分的处理器

功能模块可以自行处理领域异常，无需修改全局处理器：

```java
@RestControllerAdvice(basePackageClasses = OrderService.class)
class OrderExceptionHandler {

    @ExceptionHandler(OrderNotModifiableException.class)
    ProblemDetail handle(OrderNotModifiableException ex) { … }
}
```

限定范围的 Advice 只应用于指定包中的 Controller。建议由全局处理器负责框架异常和通用回退；功能模块负责自己的领域异常。这样可以避免每加一个功能就往全局处理器增加一项（参见[代码组织](code-organization.zh-CN.md)）。

## `@RestControllerAdvice` 无法捕获的异常

**安全过滤器中抛出的任何异常。** Filter 在 `DispatcherServlet` 之前运行，因此认证和授权失败不会到达 Advice。未认证请求默认会收到 Spring Security 返回的空 401，除非显式配置入口点：

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint(problemDetailEntryPoint())    // 401
    .accessDeniedHandler(problemDetailAccessDeniedHandler()) // 403
)
```

需要在此处手动写入 `ProblemDetail` 响应（参见[安全基础](security-fundamentals.zh-CN.md)）。

请求分派前解析失败，以及响应已经发送后异步监听器中抛出的异常，也无法由 Advice 处理（参见[异步与定时任务](async-and-scheduling.zh-CN.md)）。

## 错误日志

| 类型 | 日志级别 |
|---|---|
| 调用方可以修正的错误（400、404、422） | `DEBUG` 或不记录 |
| 冲突（409） | `INFO` |
| 服务端错误（500） | `ERROR`，带堆栈 |

把每个 404 都记录为 `ERROR` 会制造噪音并掩盖真正故障。记录异常对象，不要只记录 `ex.getMessage()`，否则堆栈会丢失。

## 使用 Spring Boot 3.5.x 时

`ProblemDetail`、`spring.mvc.problemdetails.enabled` 和 `ResponseEntityExceptionHandler` 的工作方式相同；从 Boot 3.0 起就支持 `ProblemDetail`。唯一差异是 JSON 由 Jackson 2 生成。

## 常见问题

- 自创 `{ success, data, error }` 包装——应使用 `ProblemDetail`，由状态码表示结果。
- 返回 `Map<String, Object>` 或自定义错误 record——应使用 `ProblemDetail`。
- 处理器不继承 `ResponseEntityExceptionHandler`——会失去 Spring MVC 自身异常处理。
- 认为 `problemdetails.enabled: true` 会处理领域异常——它只转换 Spring 自身异常。
- 新增 advice 与 Boot Problem Details advice 竞争，却假定自己的校验处理器会优先执行——实际由 advice 顺序决定；应继承 `ResponseEntityExceptionHandler`，或显式设置优先级并测试响应。
- 不设置 `type`——它是可供机器识别的稳定错误类型标识。
- 在 500 响应中返回 `ex.getMessage()`——只记录日志，响应返回通用消息。
- 增加 `timestamp` 属性——`Date` 响应头已经包含时间。
- 手动设置 `instance`——Spring 会根据请求 URI 填充。
- 错误响应使用 200——应通过状态码表示结果。
- 把所有失败都映射为 400——解析或绑定失败是 400，业务规则拒绝是 422。
- 在 Controller 捕获异常——从 Service 抛出，在 Advice 中处理。
- 期望 Advice 捕获安全 Filter 异常——配置 `authenticationEntryPoint` 和 `accessDeniedHandler`。
- 只返回第一条校验错误——报告全部违规项。
- 用受检异常表示领域失败——使用非受检异常，它默认会触发事务回滚。
- 将 404 记为 `ERROR`——这是噪音；`ERROR` 应留给服务端故障。

## 相关文档

- [REST Controller](rest-controllers.zh-CN.md) · [校验](validation.zh-CN.md) · [安全基础](security-fundamentals.zh-CN.md) · [事务](transactions.zh-CN.md) · [可观测性](observability.zh-CN.md)
