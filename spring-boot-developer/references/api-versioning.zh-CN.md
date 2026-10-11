# API 版本管理

Spring Framework 7 在映射层按 API 版本路由。Boot 4 项目不要用重复的 `V1`/`V2` Controller、自定义 `RequestCondition` 或过滤器中的 header 检查来手工实现版本管理。

## 给映射加版本，而不是给 Controller 分版本

```java
@RestController
@RequestMapping("/api/orders")
class OrderController {

    @GetMapping("/{id}")                              // 无版本限制：匹配任意版本
    OrderResponse getById(@PathVariable OrderId id) { … }

    @GetMapping(path = "/{id}", version = "1.1")      // 仅匹配 1.1
    OrderResponseV1_1 getByIdV1_1(@PathVariable OrderId id) { … }

    @GetMapping(path = "/{id}", version = "1.2+")     // 匹配 1.2 及之后的支持版本
    OrderResponseV2 getByIdV2(@PathVariable OrderId id) { … }
}
```

- 版本值是**语义化版本字符串**，例如 `"1.0"`，不是数字 `1`。
- 后缀 `+` 表示基线：此映射处理该版本及更新版本。
- 最具体的匹配优先。
- 同一个端点每个版本对应一个 handler。只有实际发生变化的端点需要第二个 handler；其余保持无版本映射。

这也是它优于复制 Controller 的原因：为了修改一个端点而建立 `V2` Controller，会迫使你复制另外二十个端点。

## 只选择一种版本解析策略

```java
@Configuration
class ApiVersionConfig implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
                .useRequestHeader("API-Version")
                .setDefaultVersion("1.0")
                .addSupportedVersions("1.0", "1.1", "1.2");
    }
}
```

也可以使用配置：

```yaml
spring:
  mvc:                          # WebFlux 使用 spring.webflux.apiversion.* 下的相同配置键
    apiversion:
      use:
        header: API-Version
        # path-segment: 1       # /api/v1.1/orders：版本所在路径段的索引
        # query-parameter: version
        # media-type-parameter: version
      supported: [1.0, 1.1, 1.2]
      default: 1.0
```

版本可以来自请求头、路径段、查询参数或媒体类型参数。**选一种并始终使用它。**同时采用两种来源时，请求可能携带两个版本，优先级就会变得含糊。

| 策略 | 适用情况 |
|---|---|
| 请求头 | 默认选择。URL 保持稳定，版本不属于资源标识 |
| 路径段 | 版本需要出现在可复制、记录或收藏的 URL 中 |
| 查询参数 | 在浏览器里最容易尝试，但版本混入资源标识，会影响缓存 |
| 媒体类型参数 | REST 约束严格；语义正确，但多数客户端使用不便 |

如果行为需要显式且易于审查，使用 Java 配置；简单场景可以使用属性配置。

## 默认版本与必需版本

除非配置默认版本或调用 `setVersionRequired(false)`，否则版本是**必需的**。两者都未配置时，无版本请求会返回 400。

| 情况 | 异常 | 状态码 |
|---|---|---|
| 版本不在支持列表中 | `InvalidApiVersionException` | 400 |
| 请求没有版本且未配置默认值 | `MissingApiVersionException` | 400 |

这两类异常在 handler mapping 阶段抛出。如果 API 契约要求这类错误返回框架的 Problem Details 响应，应启用 `spring.mvc.problemdetails.enabled=true`；未启用时，不要承诺原生版本解析错误响应一定是 `ProblemDetail`。应针对当前应用配置测试不支持版本和缺失版本的响应。定制响应时的 advice 顺序见[错误处理](error-handling.zh-CN.md)。

给已有客户端的 API 增加版本管理时，应设置默认版本。否则部署当天所有旧客户端都会出错。

## 弃用某个版本

```java
@Override
public void configureApiVersioning(ApiVersionConfigurer configurer) {
    var deprecation = new StandardApiVersionDeprecationHandler();
    deprecation.configureVersion("1.0")
            .setDeprecationDate(ZonedDateTime.parse("2026-09-01T00:00:00Z"))
            .setSunsetDate(ZonedDateTime.parse("2027-01-01T00:00:00Z"))
            .setDeprecationLink(URI.create("https://docs.example.com/api/v1-sunset"));

    configurer.useRequestHeader("API-Version")
            .setDeprecationHandler(deprecation);
}
```

它会发送 [RFC 9745](https://www.rfc-editor.org/rfc/rfc9745.html) 定义的 `Deprecation`、[RFC 8594](https://www.rfc-editor.org/rfc/rfc8594.html) 定义的 `Sunset`，以及指向迁移说明的 `Link` header。不要自创 sunset header，客户端和网关已经能识别标准格式。

弃用是迁移流程，不只是响应头。梳理受影响的客户端，发布迁移指南和可用替代方案，通过客户端实际使用的渠道公布日期，并在 sunset 前监控版本使用情况。指定负责人并跟踪移除任务。Sunset 日期代表真实的关闭承诺；替代方案和运营计划准备好后再发送。对于无法从请求解析版本的 API，应在 OpenAPI 和发行说明中清晰记录弃用；框架或网关能安全支持时使用标准响应头。

## 客户端也要发送版本

调用有版本管理的 API 时，客户端应采用与服务端一致的策略：

```java
RestClient.builder()
        .apiVersionInserter(ApiVersionInserter.fromHeader("API-Version").build())
        .defaultApiVersion("1.2")
        .build();
```

`RestClient`、`WebClient` 和 HTTP interface client 都支持；见 [http-clients.zh-CN.md](http-clients.zh-CN.md)。

## 什么时候需要增加版本

增加版本有成本：每个版本都是需要持续支持到 sunset 的契约。多数变化不需要新版本。

| 变化 | 是否需要新版本 |
|---|---|
| 添加可选响应字段 | 不需要，能容忍额外字段的客户端会忽略它 |
| 添加可选请求字段 | 不需要 |
| 添加端点 | 不需要 |
| 重命名或删除响应字段 | 需要 |
| 将可选请求字段改为必需 | 需要 |
| 修改字段类型或含义 | 需要 |
| 修改错误契约 | 需要 |

优先扩展现有版本，而不是创建新版本。确实需要版本化时，对**表示形式**版本化，而不是复制整个 API：一个发生变化的端点对应一个新映射。

公共 API 应在 CI 中运行 OpenAPI 兼容性差异检查，并审查有意的破坏性变更。关键集成还要有消费者契约测试；Schema 差异无法证明业务语义没有变化。保留宽容读取行为：客户端忽略未知响应字段；服务端也应明确未知请求字段的处理方式，不要依赖不一致的默认值。将可空性、枚举演进、分页和错误契约变化都纳入 API 文档。

## Boot 3.5.x 说明

Framework 6 **没有** `version` 映射属性和 `ApiVersionConfigurer`。可选方案按推荐顺序排列：

1. **暂不版本化。**尽可能通过兼容性扩展现有 API。
2. **使用路径前缀和独立 Controller：**`/api/v1/orders`、`/api/v2/orders`。显式且易测。Service 层不感知版本；各 Controller 将自己的请求模型映射到共享命令和结果。代价是每个版本要复制 Controller。
3. **手工按 header 路由**，使用自定义 `RequestCondition`。可以工作，但这是 Framework 7 已替代的机制，不建议用于新代码。

无论采用哪种方案，都要把版本控制留在 Web 边界。版本一旦进入 Service 或 Repository 层，就越界了。

## 常见误区

- 在 Boot 4 中手工创建 `/v1`、`/v2` Controller：应使用 `version` 映射属性。
- 写成 `version = 1`：版本是语义化版本 `String`，例如 `"1.0"`。
- 同时配置多个版本来源：只选一种。
- 开启版本管理却不配置默认值，也不调用 `setVersionRequired(false)`：所有旧客户端都会收到 400。
- 自创 `X-Sunset` 等响应头：使用标准 `Deprecation` 和 `Sunset` 字段（受支持的 Framework 版本可用 `StandardApiVersionDeprecationHandler`）。
- 为了改一个表示形式而复制全部端点：只版本化发生变化的映射。
- 让版本信息泄漏到 Service 层：只在 Web 边界处理版本。
- 在 3.5.x 项目使用 Boot 4 的 `version` 属性：Framework 6 不支持。
- 为纯新增的可选字段创建新版本：通常不需要。
- 忘记为出站客户端配置版本：服务端会因为请求未带版本而拒绝调用。

## 相关文档

- [rest-controllers.zh-CN.md](rest-controllers.zh-CN.md) · [error-handling.zh-CN.md](error-handling.zh-CN.md) · [http-clients.zh-CN.md](http-clients.zh-CN.md) · [openapi.zh-CN.md](openapi.zh-CN.md)
