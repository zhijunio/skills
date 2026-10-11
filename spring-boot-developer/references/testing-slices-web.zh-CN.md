# Web 切片测试

用于非持久化层的切片测试。每种测试只加载自身层的 Bean，比完整 `@SpringBootTest` 快得多（参见[测试策略](testing-strategy.zh-CN.md)）。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
```

这是 Boot 4 的名称；3.5.x 使用 `spring-boot-starter-test`。不要同时添加两者——Boot 4 的 `-test` Starter 自成一体（参见[版本与升级](boot-versions.zh-CN.md)）。

## `@WebMvcTest`——隔离测试 Controller

Boot 4 的导入包是 `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`。Boot 3.5.x 使用 `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`；不要在 Boot 4 项目中沿用旧包名。

只加载 Web 层：Controller、`@ControllerAdvice`、Filter、Converter 和 `MockMvcTester`。**不会**加载 `@Service`、`@Repository` 或 `@Component`，因此 Controller 需要的协作者都必须通过 `@MockitoBean` 提供，否则上下文启动失败。

```java
@WebMvcTest(OrderController.class)
@Import({ SecurityConfig.class, StringToOrderIdConverter.class })
class OrderControllerTest {

    @Autowired MockMvcTester mockMvc;

    @MockitoBean OrderService orderService;

    @Test
    @WithMockUser
    void returnsOrder() {
        given(orderService.getById(orderId)).willReturn(orderResponse);

        assertThat(mockMvc.get().uri("/api/orders/{id}", orderId.value()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status").isEqualTo("PLACED");
    }

    @Test
    @WithMockUser
    void rejectsInvalidPayload() {
        assertThat(mockMvc.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.violations").asArray().isNotEmpty();
    }
}
```

两项配置必须显式导入，否则测试结果会产生误导：

- **自己的 `SecurityConfig`。** Web 切片不会自动加载它。不加 `@Import` 时，测试使用 Boot 默认过滤器链，通常会意外得到 401；更糟的是，测试变绿却完全没验证真实规则。参见[安全基础](security-fundamentals.zh-CN.md)。
- **Controller 使用的 Converter**，例如[REST Controller](rest-controllers.zh-CN.md)中的值对象 ID 转换器。

应写成 `@WebMvcTest(OrderController.class)` 并指定 Controller，不要只写注解而加载**所有** Controller，那会要求 Mock 应用中每个协作者。

### `MockMvcTester`

优先使用 `MockMvcTester`（Boot 3.4+），而非原始 `MockMvc`。它原生支持 AssertJ，断言可以组合，失败信息也更易读：

```java
assertThat(mockMvc.get().uri("/api/orders")).hasStatusOk();

// 渲染视图的 Controller
assertThat(mockMvc.get().uri("/orders/{id}", id).exchange())
        .hasStatusOk()
        .hasViewName("order-detail")
        .model().containsKey("order");

// 重定向和 Flash 属性
assertThat(mockMvc.post().uri("/orders")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .param("customerEmail", "a@example.com")
        .exchange())
        .hasStatus(HttpStatus.FOUND)
        .hasRedirectedUrl("/orders")
        .flash().containsKey("successMessage");

// 绑定错误
assertThat(result).model().extractingBindingResult("order")
        .hasErrorsCount(2)
        .hasFieldErrors("customerEmail", "items");
```

可以逐步迁移：`MockMvcTester.create(mockMvc)` 能包装已有 `MockMvc`。

## 应断言什么

测试 **Web 层** 的行为，而不是 Service 行为——此处 Service 是 Mock。

| 应断言 | 不应断言 |
|---|---|
| 每种结果对应的状态码 | 业务规则（应单独测试 Service） |
| JSON 契约——字段名、结构、缺省字段 | Repository 行为 |
| 校验是否拒绝错误输入，并返回所有违规项 | |
| 认证和授权：401、403 以及允许访问场景 | |
| 错误响应是否为 `ProblemDetail` | |

断言**安全拒绝**场景。只验证成功路径的测试，在安全规则被移除时也能通过。

## `@JsonTest`——锁定线上 JSON 格式

只加载 JSON 配置并提供 `JacksonTester`。可用它锁定 DTO 序列化结构，避免字段重命名静默破坏客户端：

```java
@JsonTest
class OrderResponseJsonTest {

    @Autowired JacksonTester<OrderResponse> json;

    @Test
    void serialisesOrder() throws Exception {
        var response = new OrderResponse(orderId, "PLACED", new BigDecimal("49.99"), placedAt);

        assertThat(json.write(response))
                .hasJsonPathStringValue("$.id")
                .extractingJsonPathStringValue("$.status").isEqualTo("PLACED");
    }

    @Test
    void serialisesValueObjectAsScalar() throws Exception {
        assertThat(json.write(response))
                .extractingJsonPathStringValue("$.id")
                .isEqualTo(orderId.value().toString());   // 不是嵌套对象
    }
}
```

第二个测试尤其值得编写：它能捕捉值对象缺少 `@JsonValue` 的问题，否则 API 契约会意外输出包装对象（参见[JSON 与 Jackson](json-and-jackson.zh-CN.md)）。

## `@RestClientTest`——Stub 出站 HTTP

测试通过 `RestClient` 或 `RestTemplate` **发起请求**的代码。该切片自动配置客户端和 `MockRestServiceServer`，在进程内运行，无需端口、额外依赖或 Socket。

```java
@RestClientTest(InventoryClient.class)
class InventoryClientTest {

    @Autowired InventoryClient client;
    @Autowired MockRestServiceServer server;

    @Test
    void readsStockLevel() {
        server.expect(requestTo("/inventory/" + productId))
                .andRespond(withSuccess("""
                        {"available": 12}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.stockFor(productId).available()).isEqualTo(12);
    }

    @Test
    void translatesServerError() {
        server.expect(requestTo("/inventory/" + productId))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.stockFor(productId))
                .isInstanceOf(InventoryUnavailableException.class);
    }
}
```

优先使用该方式。`MockRestServiceServer` 只能拦截 Spring `RestClient` / `RestTemplate`。若使用 `WebClient`、HTTP 接口、第三方 SDK，或需要通过真实端口测试超时和 TLS，则改用 WireMock（参见[集成测试](testing-integration.zh-CN.md)）。

应测试**错误转换**，而不只是成功路径；Bug 往往出在那里。

## 使用 Spring Boot 3.5.x 时

`@WebMvcTest`、`@JsonTest`、`@RestClientTest`、`MockRestServiceServer`、`JacksonTester`、`MockMvcTester`（3.4+）和 `@WithMockUser` 都相同。两个区别：

- 测试 Starter 是 `spring-boot-starter-test`，而非 `spring-boot-starter-webmvc-test`。
- `@JsonTest` 使用 Jackson 2（`com.fasterxml.jackson`），而非 Jackson 3（`tools.jackson`）。DTO 断言不变，只是底层 Mapper 不同。

## 常见问题

- `@WebMvcTest` 未导入 `SecurityConfig`——测试运行于 Boot 默认链，不能验证自定义规则。
- 只写 `@WebMvcTest`——会加载所有 Controller 并要求 Mock 所有协作者，应指定 Controller。
- 忘记给协作者添加 `@MockitoBean`——切片不会加载 `@Service`，上下文会启动失败。
- 忘记导入 Converter——路径参数绑定会失效。
- Boot 4 同时添加 `spring-boot-starter-test` 和 `spring-boot-starter-webmvc-test`——`-test` Starter 已自成一体。
- 使用 `@MockBean`——Boot 4 已移除，应使用 `@MockitoBean`。
- 用原始 `MockMvc` 和 `andExpect(jsonPath(...))`——`MockMvcTester` 会提供更清晰的 AssertJ 失败信息。
- 忘记 `@WithMockUser`，然后遇到不明原因的 401。
- 只测试已授权请求——还应断言 401 和 403。
- 在 Web 切片断言业务规则——Service 是 Mock，应另行测试。
- `@RestClientTest` 已足够却使用 WireMock——进程内 Stub 无需端口。
- 出站客户端只测成功路径——还要断言错误转换。

## 相关文档

- [测试策略](testing-strategy.zh-CN.md) · [REST Controller](rest-controllers.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [JSON 与 Jackson](json-and-jackson.zh-CN.md) · [安全基础](security-fundamentals.zh-CN.md) · [HTTP 客户端](http-clients.zh-CN.md)

---

*切片测试模式参考 **Philip Riecks** 的 Testing Spring Boot Applications Demystified。*
