# REST API 测试

端到端测试层：应用监听真实端口，连接真实数据库，并且只通过 HTTP 驱动。该层应**保持精简**——它是最慢的测试层，每个测试发现的 Bug 也最少。它的职责是证明整个调用栈已连通，而非覆盖业务规则（参见[测试策略](testing-strategy.zh-CN.md)）。

## 基类

所有端到端测试都继承同一个基类，使整个套件共用一个上下文和一个容器：

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfig.class)
@AutoConfigureRestTestClient
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired protected RestTestClient restTestClient;
    @Autowired protected JsonMapper jsonMapper;

    protected String bearerTokenFor(String email) { … }
}
```

每项配置都有其用途：

- **`RANDOM_PORT`**——固定端口在 CI 和并行运行时会冲突。
- **`@Import(TestcontainersConfig.class)`**——导入共享配置（参见[Testcontainers](testcontainers.zh-CN.md)）；第二份配置意味着第二套数据库。
- **`@AutoConfigureRestTestClient`**——Boot 4 必需；`RestTestClient` 不会自动提供。
- **`@ActiveProfiles("test")`**——属性放在 `application-test.yml`，不要散布 `@TestPropertySource`；每种不同属性组合都会拆分上下文缓存。
- **`JsonMapper`**——Boot 4 使用 Jackson 3，不是 `ObjectMapper`（参见[JSON 与 Jackson](json-and-jackson.zh-CN.md)）。

共享 Mock 也放在基类，而不是各测试类中，原因相同：保持缓存键一致。

## `RestTestClient`

Spring Framework 7 的 `RestTestClient` 将 MockMvc 和 `WebTestClient` 统一到同一套流式 API，并按测试层级选择绑定方式：`bindToController(...)`、`bindTo(MockMvc)`、`bindToApplicationContext(...)` 和 `bindToServer()`。同一个客户端可用于切片到端到端的各层测试，无需逐层更换工具。

```java
class OrderApiTest extends BaseIntegrationTest {

    @Test
    void createsOrder() {
        var response = restTestClient.post()
                .uri("/api/orders")
                .headers(h -> h.setBearerAuth(bearerTokenFor(CUSTOMER_EMAIL)))
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                          "customerEmail": "customer@example.com",
                          "items": [{"productId": "%s", "quantity": 2}]
                        }
                        """.formatted(productId))
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .returnResult(OrderResponse.class)
                .getResponseBody();

        assertThat(response.status()).isEqualTo("PLACED");
    }

    @Test
    void rejectsUnauthenticated() {
        restTestClient.post().uri("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void returnsProblemDetailForUnknownOrder() {
        restTestClient.get()
                .uri("/api/orders/{id}", UUID.randomUUID())
                .headers(h -> h.setBearerAuth(bearerTokenFor(CUSTOMER_EMAIL)))
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type").exists()
                .jsonPath("$.title").isEqualTo("Resource Not Found");
    }
}
```

文本块能让请求体清晰易读；但绝不要将不可信内容插值到文本块中。只对已知值使用 `.formatted(...)`，如[Spring 中的 Java](java-in-spring.zh-CN.md)所示。

## 哪些内容属于这一层

有意挑选少量测试：

| 适合端到端测试 | 应在更低层测试 |
|---|---|
| 每种主要资源的成功路径 | 每条校验规则（[Web 切片测试](testing-slices-web.zh-CN.md)） |
| 通过 HTTP 验证认证与授权生效 | 每条业务规则（[单元测试](testing-unit.zh-CN.md)） |
| 在线路上验证错误契约确实是 `ProblemDetail` | 每种查询变体（[持久化切片测试](testing-slices-persistence.zh-CN.md)） |
| 内容协商、响应头和状态码 | 每个字段的序列化（[`@JsonTest`](testing-slices-web.zh-CN.md)） |
| 使用真实数据库完成写入再读取的完整往返 | |

为每种校验情况添加端到端测试，是测试套件变慢的常见原因。它们应放在 Web 切片中，速度快得多，失败原因也更明确。

## 测试数据

`RANDOM_PORT` 测试**不会回滚**——请求运行在另一个线程，不属于测试事务。按优先顺序选择：

1. **每个测试创建自己的数据**，使用唯一 ID；不依赖顺序，可并行运行。
2. 在基类上使用 **`@Sql("/test-data.sql")`**，每次从已知状态开始。
3. 在 `@BeforeEach` 清空数据——可以使用，但新增某个忘记清理的测试后就会出错。

不要依赖其他测试留下的数据，也不要依赖执行顺序。

## 认证

每个测试生成真实 Token 较慢，还会让测试与认证实现耦合。更好的方式：

- 在基类中生成一个 Token 并复用。
- 或使用 JWT 请求后处理器，而非真实 Token（参见[JWT 认证](jwt-authentication.zh-CN.md)）。

在此处断言拒绝场景：未认证为 401，角色错误为 403。`securityMatcher` 变更时，这些路径很容易静默损坏。


## 使用 Spring Boot 3.5.x 时

不存在 `RestTestClient`，因为它属于 Spring Framework 7。使用 `WebTestClient`（流式 API 类似，可轻松迁移）或 `TestRestTemplate`。Boot 4 中使用 `TestRestTemplate` 还需加上 `@AutoConfigureTestRestTemplate`。4.x 中两者位于新模块 `spring-boot-resttestclient`。

其他内容——`RANDOM_PORT`、`@Import`、Testcontainers 和 `@Sql`——都相同，只有 Jackson Mapper 类型和 Testcontainers 坐标不同。

## 常见问题

- 为每条校验规则编写端到端测试——应放在 Web 切片中；端到端层保持精简。
- 使用固定端口而非 `RANDOM_PORT`——CI 和并行运行会冲突。
- Boot 4 忘记 `@AutoConfigureRestTestClient`——客户端不会自动提供。
- Boot 4 使用 `TestRestTemplate` 却未加 `@AutoConfigureTestRestTemplate`。
- Boot 4 注入 `ObjectMapper`——Bean 类型为 `JsonMapper`。
- 新建第二份 `TestcontainersConfig`——会产生第二套数据库和上下文缓存。
- 在各测试类添加 `@TestPropertySource`——应共用 `application-test.yml` 和 `@ActiveProfiles`。
- 期望测试事务回滚请求中的写入——不会回滚。
- 测试依赖运行顺序或其他测试的数据。
- 只断言成功路径——还要断言 401、403 和 `ProblemDetail` 结构。
- 每个测试都生成真实认证 Token——在基类中生成一个，或使用 JWT 后处理器。

## 相关文档

- [测试策略](testing-strategy.zh-CN.md) · [集成测试](testing-integration.zh-CN.md) · [Testcontainers](testcontainers.zh-CN.md) · [错误处理](error-handling.zh-CN.md) · [JWT 认证](jwt-authentication.zh-CN.md) · [JSON 与 Jackson](json-and-jackson.zh-CN.md)
