# 集成测试与切片上下文测试

`@SpringBootTest` 覆盖多种范围：从启动全部内容的完整上下文冒烟测试，到只启动指定组件的快速切片上下文测试。它们与单注解切片测试和单元测试**互为补充**，都需要编写（参见[测试策略](testing-strategy.zh-CN.md)）。

## 冒烟测试：应用能启动吗？

这是测试套件中成本低、收益高的一项：

```java
@SpringBootTest
@Import(TestcontainersConfig.class)
class ApplicationSmokeTest {

    @Test
    void contextLoads() {
        // 任意依赖注入、配置或映射装配错误都会使测试失败
    }
}
```

每个应用至少编写一个。单元测试即使全部通过，也可能存在 Bean 定义错误；一行测试就能发现这类问题。

但能启动不代表能工作——缺少注解的处理器仍然可以启动，只是什么都不响应。还应至少添加一个**请求级**冒烟测试：

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfig.class)
@AutoConfigureRestTestClient
class TopLevelRequestSmokeTest {

    @Autowired RestTestClient restTestClient;

    @Test
    void servesOrders() {
        restTestClient.get().uri("/api/orders")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk();
    }
}
```

## 切片上下文测试：主力测试

大多数涉及框架的测试都应该采用这种方式。两个构件一起使用：

1. **`@SpringBootTest(classes = { … })`** 只注册被测组件，并额外加入一个用于替代缓慢或昂贵外部依赖的 `@TestConfiguration`，而不是启动整个应用。
2. **切片注解**装配该层需要的自动配置。只列出 `classes` 会得到 Bean，但不会得到框架装配，例如 JPA Repository 和 Web 层；切片注解负责补齐。

内置切片（`@WebMvcTest`、`@DataJpaTest`）就是预先选好切片的上述组合。需要同时装配多个真实层时，再使用显式形式。

**Mock 外部依赖，其余保持真实：**

```java
@TestConfiguration(proxyBeanMethods = false)
class StubExternals {

    @Bean
    PaymentGateway paymentGateway() {
        var gateway = mock(PaymentGateway.class);
        when(gateway.charge(any(), any()))
                .thenReturn(new PaymentOutcome.Authorized("ch_test", Money.of("49.99", "GBP")));
        return gateway;
    }
}
```

**变体 A——Mock Repository。** 速度接近单元测试，但 Service 装配是真实的：

```java
@SpringBootTest(classes = { OrderService.class, StubExternals.class })
class OrderServiceSlicedTest {

    @Autowired OrderService orderService;
    @MockitoBean OrderRepository orderRepository;

    @Test
    void placesOrder() {
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(orderService.place(cmd)).isNotNull();
    }
}
```

**变体 B——使用可复用切片连接真实数据库。** 删除 Repository Mock，列出真实 Repository，添加装配 JPA 和真实 PostgreSQL 的组合注解。它需要启动容器，因此不如单元测试快，但会执行真实 SQL：

```java
@SpringBootTest(classes = { OrderService.class, OrderRepository.class, StubExternals.class })
@EnableDatabaseTest
class OrderServiceDatabaseTest {

    @Autowired OrderService orderService;

    @Test
    void persistsPlacedOrder() {
        var orderId = orderService.place(cmd);
        assertThat(orderService.getById(orderId).status()).isEqualTo("PLACED");
    }
}
```

组合注解在套件中定义并复用：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableJpaRepositories(basePackages = "com.example.app.order")
@EntityScan("com.example.app.order")
@AutoConfigureDataJpa
@AutoConfigureTestEntityManager
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfig.class)
public @interface EnableDatabaseTest {}
```

需要注意：

- 通过本层自己的入口进行断言——用 `@WebMvcTest` 测 Controller，直接调用 Service 测 Service。除非有意测试 HTTP，此层不需要 `@AutoConfigureMockMvc`。
- 每种不同的 `@MockitoBean` 组合和 `@TestConfiguration` 都会改变上下文缓存键。应把共享项集中到基类（参见[测试策略](testing-strategy.zh-CN.md)）。
- **复用套件现有的 `TestcontainersConfig`**，不要再创建第二个；两种容器配置意味着两套数据库和两组缓存。

## 外部 HTTP：优先进程内 Stub，需要端口时再用 WireMock

完整测试和切片测试仍会调用代码依赖的外部服务，应对其 Stub：

1. **默认使用 `@RestClientTest` 和 `MockRestServiceServer`** 测试 `RestClient` / `RestTemplate` 客户端。它在进程内工作，无需端口（参见[Web 切片测试](testing-slices-web.zh-CN.md)）。大型 `@SpringBootTest` 中的昂贵外部依赖通常只需由 `@TestConfiguration` 替换客户端 Bean。
2. 只有确实需要真实 HTTP 端口时才升级到 WireMock：客户端为 `WebClient`、HTTP 接口或第三方 SDK；需要验证真实超时、TLS 或重试；需要注入故障或延迟；或者同一个 Stub 需要服务整个上下文。

**切片测试使用进程内 Stub，需要端口时使用 WireMock。**

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfig.class)
@AutoConfigureRestTestClient
class CheckoutIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void paymentProperties(DynamicPropertyRegistry registry) {
        registry.add("app.payments.base-url", wireMock::baseUrl);   // 只有启动后才知道
    }

    @Autowired RestTestClient restTestClient;

    @Test
    void checkoutChargesViaPaymentService() {
        wireMock.stubFor(post("/charge").willReturn(okJson("""
                {"status":"succeeded","id":"ch_123"}
                """)));

        restTestClient.post().uri("/api/checkout").exchange().expectStatus().isOk();
    }

    @Test
    void surfacesPaymentTimeoutAsServiceUnavailable() {
        wireMock.stubFor(post("/charge").willReturn(aResponse().withFixedDelay(10_000)));

        restTestClient.post().uri("/api/checkout").exchange().expectStatus().is5xxServerError();
    }
}
```

WireMock 端口在启动时分配，因此它的 Base URL 是 `@DynamicPropertySource` 的典型用法（参见[Testcontainers](testcontainers.zh-CN.md)）。

第二个用例尤其值得编写：只有注入故障，才能实际证明下游超时会产生预期行为，而不是让请求一直挂起（参见[弹性模式](resilience.zh-CN.md)）。

## 测试异步与消息

异步断言必须等待结果，绝不能调用 `sleep`：

```java
@Test
void reservesInventoryOnOrderPlaced() {
    orderService.place(cmd);

    await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(reservationRepository.findByOrderId(orderId)).isPresent());
}
```

`Thread.sleep` 通常既不稳定又慢。参见[异步与定时任务](async-and-scheduling.zh-CN.md)和[消息](messaging.zh-CN.md)。

## 使用 Spring Boot 3.5.x 时

- `@SpringBootTest`（包括 `classes = …`）、`RANDOM_PORT`、`@ServiceConnection` 和 WireMock 行为相同。
- **REST 测试客户端**：`RestTestClient` 和 `@AutoConfigureRestTestClient` 属于 Boot 4（Spring Framework 7）。3.5.x 使用 `WebTestClient` 或 `TestRestTemplate`。
- Boot 4 模块化重组后，测试自动配置注解迁移了包名，因此自定义切片注解（例如 `@EnableDatabaseTest`）的 import 也不同。比如 4.x 中 `@AutoConfigureDataJpa` 位于 `org.springframework.boot.data.jpa.test.autoconfigure`，3.5.x 中位于 `org.springframework.boot.test.autoconfigure.orm.jpa`；4.x 的 `@EntityScan` 位于 `org.springframework.boot.persistence.autoconfigure`，3.5.x 中位于 `org.springframework.boot.autoconfigure.domain`。应让 IDE 帮助解析。
- Testcontainers 的坐标不同（参见[Testcontainers](testcontainers.zh-CN.md)）。
- 仅 4.x 支持上下文暂停和更方便的 `@TestConfiguration` Bean 覆盖。

## 常见问题

- 每个测试都启动完整 `@SpringBootTest`——使用 `classes = { … }` 加切片注解。
- 没有冒烟测试——Bean 定义故障会直到生产才暴露。
- 只有上下文加载测试就认为完成——能启动不等于能提供服务，还应测试实际请求。
- 再建一套 `TestcontainersConfig`——会有两套数据库和上下文缓存。
- 每个测试类都添加独有的 `@MockitoBean`——每种组合都会拆分上下文缓存。
- `@RestClientTest` 已足够却使用 WireMock——进程内 Stub 不需要端口。
- 硬编码 WireMock 端口——使用 `dynamicPort()` 并动态注册 Base URL。
- 只 Stub 成功响应——注入故障才能验证超时和重试行为。
- 用 `Thread.sleep` 断言异步结果——应使用 Awaitility。
- 认为 `RANDOM_PORT` 测试会回滚——请求在另一线程执行。
- 使用 `@MockBean`——Boot 4 已移除。
- 将 Boot 4 测试自动配置包名复制到 3.5.x 项目——两者包名不同。

## 相关文档

- [测试策略](testing-strategy.zh-CN.md) · [REST API 测试](testing-rest-api.zh-CN.md) · [Testcontainers](testcontainers.zh-CN.md) · [Web 切片测试](testing-slices-web.zh-CN.md) · [弹性模式](resilience.zh-CN.md) · [消息](messaging.zh-CN.md)

---

*冒烟测试和切片上下文方法参考 **Paul Bakker**（Netflix）的 [Testing Spring Boot the Netflix Way](https://github.com/paulbakker/testing-spring-boot-presentation)。WireMock 和集成测试模式参考 **Philip Riecks** 的 Testing Spring Boot Applications Demystified。*
