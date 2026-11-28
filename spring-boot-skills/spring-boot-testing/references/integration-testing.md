# Integration testing

Use a real Spring context and compatible real dependencies when the required behavior crosses their
boundaries. Inspect the existing test infrastructure first. Verify the actual application wiring,
transaction manager, protocol, configuration and failure state. Reuse project-managed containers
where appropriate; pin the approved database version/image rather than treating this example as a
universal default. A full context is justified by required behavior, not by a fixed testing ratio.

## Integration Tests — Testcontainers

This illustrative application-level fragment assumes the first-party JWT setup and order API from
the related skills. Supply `src/test/resources/fixtures/order-user-and-product.sql` for
your application's schema: an enabled USER named `buyer@example.com` and an in-stock
product with ID `00000000-0000-0000-0000-000000000001`. Run it only against the disposable
test database. Adapt the fixtures and response DTO to your application.

Load the real persisted user before signing the token. `@WithMockUser` does not authenticate
a request sent over HTTP to a random-port server. For OAuth2 resource servers, use a test
issuer instead of introducing this application's JWT service.

```java
// Example shared context configuration; lifecycle depends on the test context
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() { // org.testcontainers.postgresql.PostgreSQLContainer (TC 2)
        return new PostgreSQLContainer("postgres:16-alpine");
    }
}

// Full integration test
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfig.class)
class OrderIntegrationTest {

    @Autowired RestTestClient restTestClient;
    @Autowired OrderRepository orderRepository;
    @Autowired JwtFactory jwtFactory;
    @Autowired UserDetailsService userDetailsService;

    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final ParameterizedTypeReference<OrderResponse> RESPONSE_TYPE =
        new ParameterizedTypeReference<>() {};

    @Test
    @Sql("/fixtures/order-user-and-product.sql")
    void createAndRetrieveOrder_endToEnd() {
        var user = userDetailsService.loadUserByUsername("buyer@example.com");
        String token = jwtFactory.generateAccessToken(user);
        var createRequest = new CreateOrderRequest("buyer@example.com",
            List.of(new CreateOrderRequest.OrderItemRequest(PRODUCT_ID, 1)));
        var created = restTestClient.post()
            .uri("/api/v1/orders")
            .headers(headers -> headers.setBearerAuth(token))
            .body(createRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(RESPONSE_TYPE)
            .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        UUID id = created.id();
        assertThat(id).isNotNull();
        assertThat(orderRepository.findById(id)).isPresent();

        var retrieved = restTestClient.get()
            .uri("/api/v1/orders/{id}", id)
            .headers(headers -> headers.setBearerAuth(token))
            .exchange()
            .expectStatus().isOk()
            .expectBody(RESPONSE_TYPE)
            .returnResult().getResponseBody();
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.id()).isEqualTo(id);
        assertThat(retrieved.status()).isEqualTo(created.status());
        assertThat(retrieved.items()).hasSize(1);
        assertThat(retrieved.items().get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(retrieved.items().get(0).quantity()).isEqualTo(1);
    }
}
```

Real HTTP requests run on application threads, so test-thread rollback does not clean their writes.
Use a disposable database or scoped cleanup of owned records; ensure repeated test runs do not
reuse conflicting fixtures. For rollback assertions observe state after the application transaction
ends. For concurrency use independent connections/transactions and bounded synchronization.
Test relevant failure paths, not just the successful round trip. RestTestClient with
@AutoConfigureRestTestClient is a Boot 4 option; preserve compatible existing clients and verify
availability in the exact project version. Do not silently upgrade a Boot line.

## Testcontainers Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId> <!-- Testcontainers 2 PostgreSQL artifact; check managed version -->
    <scope>test</scope>
</dependency>
```

For Testcontainers 2, use the managed `testcontainers-postgresql` artifact and
`org.testcontainers.postgresql.PostgreSQLContainer` (non-generic). Check the selected Boot BOM
and integration modules rather than assuming every module has the same artifact naming.
