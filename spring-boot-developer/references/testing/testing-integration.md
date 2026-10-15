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
- Agent uses `Thread.sleep` for asynchronous assertions - use Awaitility
- Agent expects a `RANDOM_PORT` test to roll back - the request runs on another thread
- Agent uses `@MockBean` - removed in Boot 4

## Related

- [testing-strategy.md](testing-strategy.md) · [testing-rest-api.md](testing-rest-api.md) · [testcontainers.md](testcontainers.md) · [testing-slices-web.md](testing-slices-web.md) · [resilience.md](../integration/resilience.md) · [security fundamentals](../security/security-fundamentals.md)

---

*The smoke-test and sliced-context approach credits **Paul Bakker** (Netflix), [Testing Spring Boot the Netflix Way](https://github.com/paulbakker/testing-spring-boot-presentation). WireMock and integration patterns credit **Philip Riecks**, Testing Spring Boot Applications Demystified.*

## References

- [Spring Boot testing reference](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
- [Testing Spring Boot the Netflix Way](https://github.com/paulbakker/testing-spring-boot-presentation)
