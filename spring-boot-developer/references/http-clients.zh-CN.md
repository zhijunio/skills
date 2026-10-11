# HTTP 客户端

## 选择哪种客户端

| 客户端 | 用途 |
|---|---|
| **HTTP 接口**（`@HttpExchange`） | **默认选择。** 用接口声明远程 API，由 Spring 生成实现 |
| `RestClient` | 命令式调用；接口不够灵活时使用，例如 URI 需计算、流式处理或条件响应头 |
| `WebClient` | 仅当返回类型确实是 `Mono` / `Flux` 时使用 |
| `RestTemplate` | 仅维护旧代码；已由 `RestClient` 取代 |

Java 21+ 配合虚拟线程时，`RestClient` 不再是“应避免的阻塞客户端”：虚拟线程上的阻塞调用不会固定平台线程。仅为并发而选择 `WebClient` 已没有必要。参见[异步与定时任务](async-and-scheduling.zh-CN.md)。

## 声明式接口

```java
@HttpExchange("/inventory")
public interface InventoryClient {

    @GetExchange("/{productId}")
    StockLevel stockFor(@PathVariable UUID productId);

    @PostExchange("/reservations")
    ReservationResponse reserve(@RequestBody ReservationRequest request);

    @DeleteExchange("/reservations/{id}")
    void release(@PathVariable UUID id);
}
```

不需要 `@Component` 或实现类。**Boot 4** 使用 `@ImportHttpServices` 注册：

```java
@Configuration
@ImportHttpServices(group = "inventory", basePackages = "com.example.app.inventory.client")
class InventoryClientConfig {
}
```

```yaml
spring:
  http:
    clients:                       # 全局传输默认配置
      connect-timeout: 2s
      read-timeout: 5s
    serviceclient:
      inventory:                   # 必须与分组名一致
        base-url: ${INVENTORY_BASE_URL}
        read-timeout: 10s
```

注意两个不同配置前缀：全局默认项位于 `spring.http.clients.*`，分组配置位于 `spring.http.serviceclient.<group>.*`。常见错误是写成 `spring.http.client.*`（单数），它不是分组前缀。

默认客户端是阻塞式 `RestClient`。接口返回 `Mono` / `Flux` 时，选择 `HttpServiceGroup.ClientType.WEB_CLIENT` 并添加 WebClient Starter。分组定制器作用于整个组，需要不同配置的接口应放入单独分组。

主机地址放在配置中，绝不要写进 `@HttpExchange`。注解中只保存路径。

## 超时

**每个客户端都需要连接超时和读取超时。** 不设置时默认值实际上是无限等待；一个慢依赖会耗尽请求线程，最终导致整个应用不响应。这是健康服务被不健康服务拖垮的最常见原因。

在 `spring.http.clients.*` 设置全局值，并按分组覆盖。手动构建客户端时：

```java
@Bean
RestClient inventoryRestClient(RestClient.Builder builder, InventoryProperties properties) {
    var settings = ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(Duration.ofSeconds(2))
            .withReadTimeout(Duration.ofSeconds(5));

    return builder
            .baseUrl(properties.baseUrl())
            .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
            .build();
}
```

读取超时应短于本服务的入站超时。例如负载均衡器 10 秒后就放弃请求，内部调用却设置 30 秒读取超时，只会白白浪费资源。

## 错误

默认情况下，4xx 或 5xx 会抛出 `HttpClientErrorException` 或 `HttpServerErrorException`。应在客户端边界转换为领域异常，使调用方不必了解 HTTP 异常类型：

```java
@Bean
RestClientCustomizer inventoryErrorHandling() {
    return builder -> builder
            .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                    throw new ProductNotStockedException(request.getURI());
                }
                throw new InventoryRequestRejectedException(response.getStatusCode());
            })
            .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                throw new InventoryUnavailableException(response.getStatusCode());
            });
}
```

这会影响重试策略：4xx 重试仍会以相同方式失败；5xx 或超时则可能恢复。区分异常类型后，[弹性模式](resilience.zh-CN.md)才能只重试值得重试的情况。

不要让远程故障以带有上游消息的 500 暴露给调用方，否则会泄漏内部服务拓扑。

## 认证

传播调用方令牌，或使用服务凭据；绝不要硬编码密钥：

```java
@Bean
RestClientCustomizer bearerTokenPropagation() {
    return builder -> builder.requestInterceptor((request, body, execution) -> {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt) {
            request.getHeaders().setBearerAuth(jwt.getToken().getTokenValue());
        }
        return execution.execute(request, body);
    });
}
```

传播用户令牌可以让下游识别真实调用者并自行授权。没有用户身份的服务间调用，应使用 Client Credentials 流程，而不是共享静态秘密。

在 `@Async` 线程或定时任务中，读取 `SecurityContextHolder` 的拦截器会发现上下文为空，因为默认不会传播。应显式传入凭据。

## 调用带版本的 API

```java
RestClient.builder()
        .apiVersionInserter(ApiVersionInserter.fromHeader("API-Version").build())
        .defaultApiVersion("1.2")
        .build();
```

应与服务端采用相同策略（参见[API 版本管理](api-versioning.zh-CN.md)）。

## 测试

```java
@RestClientTest(InventoryClient.class)
class InventoryClientTest {

    @Autowired InventoryClient client;
    @Autowired MockRestServiceServer server;

    @Test
    void readsStockLevel() {
        server.expect(requestTo("/inventory/9f2c8b1e"))
                .andRespond(withSuccess("""
                        {"available": 12}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.stockFor(productId).available()).isEqualTo(12);
    }

    @Test
    void translatesNotFound() {
        server.expect(requestTo("/inventory/9f2c8b1e")).andRespond(withResourceNotFound());
        assertThatThrownBy(() -> client.stockFor(productId))
                .isInstanceOf(ProductNotStockedException.class);
    }
}
```

`MockRestServiceServer` 可在进程内拦截 `RestClient` 和 `RestTemplate`，无需端口或额外依赖，优先使用它。

只有需要真实 Socket 时才使用 WireMock，例如验证真实超时、TLS、故障注入，或测试它无法拦截的客户端。参见[集成测试](testing-integration.zh-CN.md)。

始终测试错误转换，不要只测成功路径。

## 使用 Spring Boot 3.5.x 时

`@HttpExchange` 和 `RestClient` 已存在，但 `@ImportHttpServices` 和 `spring.http.serviceclient.*` **尚不存在**，它们属于 Boot 4。需显式创建代理：

```java
@Bean
InventoryClient inventoryClient(RestClient.Builder builder, InventoryProperties properties) {
    var restClient = builder
            .baseUrl(properties.baseUrl())
            .requestFactory(timeoutFactory())
            .build();
    return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(InventoryClient.class);
}
```

每个外部服务创建一个工厂。其他部分——超时、状态处理器、拦截器和 `@RestClientTest`——都相同。

## 常见问题

- 不设置超时——默认几乎是无限等待，一个慢依赖就可能拖垮应用。
- Boot 4 为每个客户端手写 `HttpServiceProxyFactory` Bean——应使用 `@ImportHttpServices` 分组。
- 用 `spring.http.client.*` 配置分组——全局配置为 `spring.http.clients.*`，分组为 `spring.http.serviceclient.<group>.*`。
- 给 `@HttpExchange` 接口加 `@Component` 或实现类——都不需要。
- 在 `@HttpExchange` 中硬编码主机——注解只写路径，主机来自配置。
- 默认客户端类型下返回 `Mono` / `Flux`——显式选择 `WEB_CLIENT`。
- Java 21+ 为并发而使用 `WebClient`——虚拟线程已消除这个理由。
- 让 `HttpClientErrorException` 直接传到调用方——在客户端边界转成领域异常。
- 重试 4xx——同样会失败；区分客户端错误和服务端错误。
- 把上游错误消息返回给调用方——会泄漏内部拓扑。
- 硬编码 API 密钥——使用配置或秘密管理器。
- 在 `@Async` 或定时任务线程传播令牌——那里没有安全上下文。
- 所有测试都使用 WireMock——`MockRestServiceServer` 不用端口，可在进程内运行。
- 只测成功路径——还要断言错误转换。

## 相关文档

- [弹性模式](resilience.zh-CN.md) · [API 版本管理](api-versioning.zh-CN.md) · [事务](transactions.zh-CN.md) · [异步与定时任务](async-and-scheduling.zh-CN.md) · [集成测试](testing-integration.zh-CN.md) · [可观测性](observability.zh-CN.md)
