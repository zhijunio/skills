# Integration and Sliced-Context Testing

`@SpringBootTest` spans a range - from a full-context smoke test that boots everything, to a fast sliced-context test that boots only what you list. These complement built-in slices and unit tests; choose each layer based on the behavior and risk ([testing-strategy.md](testing-strategy.md)).

## Smoke test - does the application start?

The cheapest high-value test in the suite:

```java
@SpringBootTest
@Import(TestcontainersConfig.class)
class ApplicationSmokeTest {

    @Test
    void contextLoads() {
        // fails on any DI, configuration, or mapping wiring error
    }
}
```

Write one per application. A unit-test suite can be entirely green while a bean definition is broken; this catches that class of failure in one line.

But starting is not the same as working - a handler missing its annotation starts fine and serves nothing. Add at least one **request-level** smoke test:

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

## Sliced-context tests - the workhorse

Use Spring-managed tests for framework behavior, choosing between built-in slices and a custom context:

- **Built-in slices** such as `@WebMvcTest` and `@DataJpaTest` load a predefined layer. See [web slice testing](testing-slices-web.md) and [persistence slice testing](testing-slices-persistence.md).
- **Custom partial contexts** use `@SpringBootTest(classes = { … })` with selected `@AutoConfigure…` annotations and imported test configuration. Use this when a built-in slice cannot express the real component path you need to verify.

Do not combine multiple `@…Test` slice annotations on one test class. For custom contexts, register only the components under test and stub slow or costly external boundaries. See the [Spring Boot testing reference](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html) for the built-in slice model.

**Mock the external, keep the rest real:**

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

**Variant A - repository mocked. Unit-test speed, real service wiring:**

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

**Variant B - real database via a reusable slice.** Drop the repository mock, list the real repository, add a meta-annotation that wires JPA and a real PostgreSQL. This pays a container start, so it is not unit-test fast, but it exercises real SQL:

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

The meta-annotation is written once and reused across the suite. On Boot 4, import `@EntityScan` from `org.springframework.boot.persistence.autoconfigure.EntityScan`, `@AutoConfigureDataJpa` from `org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa`, `@AutoConfigureTestEntityManager` from `org.springframework.boot.jpa.test.autoconfigure.AutoConfigureTestEntityManager`, and `@AutoConfigureTestDatabase` from `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`. Boot 3.5.x uses `org.springframework.boot.autoconfigure.domain.EntityScan`, keeps the two JPA annotations in `org.springframework.boot.test.autoconfigure.orm.jpa`, and keeps `@AutoConfigureTestDatabase` in `org.springframework.boot.test.autoconfigure.jdbc`; do not carry those packages into a Boot 4 project.

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

Notes that matter:

- Assert through the layer's own entry point - a controller via `@WebMvcTest`, a service directly. You do not need `@AutoConfigureMockMvc` at this level unless you are deliberately exercising HTTP.
- Every distinct `@MockitoBean` set and `@TestConfiguration` changes the context cache key. Consolidate shared ones in a base class ([testing-strategy.md](testing-strategy.md)).
- **Reuse the suite's existing `TestcontainersConfig`** rather than writing a second one - two container configurations mean two databases and two cache families.

## Full REST API tests

The end-to-end layer: the application on a real port, a real database, driven only through HTTP. Keep this layer **thin** - it is the slowest tier and the one that catches the fewest bugs per test. Its job is to prove the whole stack is wired together, not to cover business rules ([testing-strategy.md](testing-strategy.md)).

### The base class

One base class, imported by every end-to-end test, so the whole suite shares a single context and a single container:

```java
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;

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

Every element earns its place:

- **`RANDOM_PORT`** - a fixed port breaks on CI and under parallel runs.
- **`@Import(TestcontainersConfig.class)`** - the *shared* configuration ([testcontainers.md](testcontainers.md)). A second one means a second database.
- **`@AutoConfigureRestTestClient`** - required on Boot 4; `RestTestClient` is not auto-provided.
- **`@ActiveProfiles("test")`** with settings in `application-test.yml` rather than scattered `@TestPropertySource` - every distinct property set forks the context cache.
- **`JsonMapper`** - Jackson 3 on Boot 4, not `ObjectMapper` ([testing-slices-web.md](testing-slices-web.md)).

Put shared mocks here too, not on individual test classes, for the same cache reason.

### `RestTestClient`

Spring Framework 7's `RestTestClient` is built on Spring's `RestClient` and unifies MockMvc and `WebTestClient` behind one fluent API, with a bind mode per test level: `bindToController(...)`, `bindTo(MockMvc)`, `bindToApplicationContext(...)`, and `bindToServer()`. Prefer it for request-level tests against the application's own HTTP server: it uses Spring's client stack and includes response assertions. Keep raw `RestClient` for production outbound calls, and use a JDK HTTP client in tests only when the production code itself uses that client. The same client spans the sliced-to-end-to-end range instead of switching tools per level.

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

Text blocks keep request bodies readable - but never interpolate untrusted content into one; use `.formatted(...)` with known values, exactly as in [security fundamentals](../security/security-fundamentals.md).

### What belongs at this level

A handful of tests, chosen deliberately:

| Worth an end-to-end test | Test it lower down instead |
|---|---|
| The happy path of each major resource | Every validation rule ([testing-slices-web.md](testing-slices-web.md)) |
| Authentication and authorization actually apply over HTTP | Every business rule ([testing-unit.md](testing-unit.md)) |
| The error contract really is `ProblemDetail` on the wire | Every query variant ([testing-slices-persistence.md](testing-slices-persistence.md)) |
| Content negotiation, headers, status codes | Every field's serialisation ([`@JsonTest`](testing-slices-web.md)) |
| A full write-then-read round trip through the real database | |

Adding an end-to-end test for every validation case is the most common way a suite becomes slow. Those belong in a web slice, which runs in a fraction of the time and gives a sharper failure.

### Test data

`RANDOM_PORT` tests **do not roll back** - the request runs on another thread, outside the test's transaction. Options in order of preference:

1. **Each test creates its own data** with unique identifiers. Order-independent and parallel-safe.
2. **`@Sql("/test-data.sql")`** on the base class for a shared, known starting state.
3. Truncate in `@BeforeEach` - works, and breaks the day someone adds a test that forgets.

Do not let tests depend on each other's leftovers, and never on execution order.

### Authentication

Generating a real token per test is slow and couples the suite to the auth implementation. Better:

- Issue one token in the base class and reuse it.
- Or use the JWT post-processor for the request rather than a real token ([OAuth2 resource server](../security/oauth2-resource-server.md)).

Assert the negative cases here: unauthenticated is 401, wrong role is 403. These are the paths that silently break when a `securityMatcher` changes.

### Gotchas

- Agent writes end-to-end tests for every validation rule - they belong in a web slice; this layer stays thin
- Agent uses a fixed port instead of `RANDOM_PORT` - clashes on CI and in parallel runs
- Agent omits `@AutoConfigureRestTestClient` on Boot 4 - the client is not auto-provided
- Agent uses `TestRestTemplate` on Boot 4 without `@AutoConfigureTestRestTemplate`
- Agent injects `ObjectMapper` on Boot 4 - the bean is a `JsonMapper`
- Agent creates a second `TestcontainersConfig` - a second database and a second cache family
- Agent puts `@TestPropertySource` on individual test classes - use one `application-test.yml` with `@ActiveProfiles`
- Agent expects the test transaction to roll back the request's writes - it does not
- Agent writes tests that depend on execution order or another test's data
- Agent asserts only the happy path - assert 401, 403, and the `ProblemDetail` shape
- Agent generates a real auth token per test - issue one in the base class or use the JWT post-processor


## External HTTP: in-process first, WireMock when you need a port

Full and sliced tests still call whatever external services the code depends on. Stub them:

1. **Default - `@RestClientTest` with `MockRestServiceServer`** for `RestClient`/`RestTemplate` clients. In-process, no port ([testing-slices-web.md](testing-slices-web.md)). For a costly external inside a larger `@SpringBootTest`, a `@TestConfiguration` stub of the client bean is usually enough.
2. **Escalate to WireMock** only when you need a real HTTP server on a port: the client is `WebClient`, an HTTP-interface client or a third-party SDK; you need real timeouts, TLS or retry behaviour; you want fault or latency injection; or one stub must serve the whole context.

**In-process for the slice, WireMock for the port.**

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
        registry.add("app.payments.base-url", wireMock::baseUrl);   // known only after startup
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

WireMock's port is assigned at startup, so its base URL is a textbook `@DynamicPropertySource` value ([testcontainers.md](testcontainers.md)).

The second test is the one worth writing: fault injection is the only practical way to prove that a downstream timeout produces the behaviour you intended rather than a hung request ([resilience.md](../integration/resilience.md)).

### Provider contracts and external dependency tests

A mock can prove application branching; it cannot prove HTTP serialization, authentication headers, timeout behavior or
compatibility with a provider. Keep the HTTP client behind a small application port. Unit tests can fake that port to
check domain decisions; client-boundary tests should exercise the actual request factory and serialization against a
local HTTP server, then assert method, path, headers, body, response mapping, error translation and timeout behavior.
Return representative provider payloads, including unknown fields and malformed/error responses. Do not mock Spring's
internal `RestClient` or `WebClient` chain; verify behavior at the network boundary.

Use a local stub such as WireMock or MockWebServer when a test needs a port. Keep stubs deterministic, bind to an
ephemeral port, reset mappings and scenarios between tests, and never share mutable expectations across parallel test
cases. For critical integrations, maintain a provider contract from its OpenAPI/schema or a consumer-driven contract.
Contract tests complement a small number of sandbox checks; they do not prove provider availability or operational
behavior. Pin the provider API version where supported, and sanitize recorded fixtures.

For local development, document a substitute or sandbox for each external dependency when feasible. Use fake
credentials, reject unintended calls, and never send real email, payments or customer data. Use Testcontainers when the
production protocol or engine matters, such as a broker or S3-compatible store; use a local mail sink for email
rendering and delivery flow. Make costly or stateful sandbox checks opt-in or isolated from ordinary CI.

Test failure and recovery as well as success: timeouts, connection refusal, 4xx/5xx mapping, malformed payloads,
duplicate delivery, retry limits, circuit state and cleanup. Configure explicit test base URLs and fail fast if a test
profile points to a production host. Avoid logging full request/response bodies that can contain tokens or personal
data. If parallel tests share a substitute, isolate scenarios by unique IDs and ensure cleanup cannot delete another
test's data.

## Testing async and messaging

Anything asynchronous needs an awaited assertion, never a sleep:

```java
@Test
void reservesInventoryOnOrderPlaced() {
    orderService.place(cmd);

    await().atMost(Duration.ofSeconds(10))
            .untilAsserted(() -> assertThat(reservationRepository.findByOrderId(orderId)).isPresent());
}
```

`Thread.sleep` is either flaky or slow, usually both. See [testing-strategy.md](testing-strategy.md) for isolation and bounded test execution.

## Gotchas

- Agent uses full `@SpringBootTest` for every test - use `classes = { … }` plus a slice
- Agent writes no smoke test - a broken bean definition then surfaces in production
- Agent writes a context-loads test and calls it done - starting is not serving; add a request-level test
- Agent writes a second `TestcontainersConfig` - two databases and two context-cache families
- Agent adds a `@MockitoBean` per test class - each distinct set forks the context cache
- Agent reaches for WireMock where `@RestClientTest` would do - the in-process stub needs no port
- Agent hardcodes a WireMock port - use `dynamicPort()` and register the base URL dynamically
- Agent stubs only success responses - fault injection is the only way to test timeout and retry behaviour
- Agent treats a local stub as proof of provider compatibility - add a contract or controlled sandbox check for that claim
- Agent shares mutable stub scenarios across parallel tests - isolate and reset them
- Agent records real tokens or customer data in fixtures - sanitize fixtures and use fake credentials
- Agent uses `Thread.sleep` for asynchronous assertions - use Awaitility
- Agent expects a `RANDOM_PORT` test to roll back - the request runs on another thread
- Agent uses `@MockBean` - removed in Boot 4

## Related

- [testing-strategy.md](testing-strategy.md) · [testcontainers.md](testcontainers.md) · [testing-slices-web.md](testing-slices-web.md) · [HTTP clients](../integration/http-clients.md) · [OpenAPI](../web/openapi.md) · [resilience.md](../integration/resilience.md) · [security fundamentals](../security/security-fundamentals.md)

---

*The smoke-test and sliced-context approach credits **Paul Bakker** (Netflix), [Testing Spring Boot the Netflix Way](https://github.com/paulbakker/testing-spring-boot-presentation). WireMock and integration patterns credit **Philip Riecks**, Testing Spring Boot Applications Demystified.*

## References

- [Spring Boot testing reference](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Testing Spring Boot the Netflix Way](https://github.com/paulbakker/testing-spring-boot-presentation)
- [WireMock Documentation](https://wiremock.org/docs/)
