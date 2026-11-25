# Testing

Read this when adding or changing tests, or when a datasource enters the
classpath. Build-tool-specific test lifecycle and plugin configuration belong
to the matching build reference; Maven guidance is in [maven.md](maven.md).
Controller behavior lives in [webmvc.md](webmvc.md). Use Testcontainers for
integration tests that need real PostgreSQL, Redis, or SMTP behavior; keep
unit tests independent of containers.

## When to use

- Add or change unit or integration tests
- Add MyBatis-Plus, Flyway, PostgreSQL, or a real service dependency
- The user mentions tests or context loading

## Test strategy

Choose the cheapest test that proves the behavior at the boundary where it
matters:

- **Pure domain rules and application logic:** direct-construction unit test;
  no Spring context or containers.
- **MVC binding, validation, serialization, and advice:** `@WebMvcTest`; mock
  only collaborators outside the slice.
- **Outbound blocking HTTP request/response mapping:** `@RestClientTest` with
  the mock request server; keep authorization-provider wiring in its own
  integration test.
- **SQL, migration, cache serialization/invalidation, or SMTP behavior:** a
  service integration test with the real Testcontainer for that dependency.
- **Cross-cutting application wiring:** retain the generated context-load
  smoke test and keep additional full-context tests small.
- **A complete web request path:** add a request-level test against a random
  port when real servlet filters, server wiring, or HTTP client/server
  interaction is part of the acceptance behavior. In Boot 4, use
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `RestTestClient`.

Do not force every feature through `@SpringBootTest`, and do not use a slice
to claim that database or provider integration works. Avoid arbitrary test
counts or a fixed pyramid ratio; select by behavior and startup cost.

Spring caches test contexts by their merged configuration. Different test
configuration classes/imports, active profiles, property overrides,
`@MockitoBean` definitions, initializers, and context customizers can create
distinct cache entries. Reuse stable configuration and service combinations;
add mocks only when they isolate an external boundary; avoid routine
`@DirtiesContext`, which evicts an expensive context. Use
`logging.level.org.springframework.test.context.cache=DEBUG` temporarily to
diagnose unexpected context churn. Container lifecycle must cover every
cached context that uses it.

## When not to use

- Maven plugin versions or `failIfNoTests` → [maven.md](maven.md)

## Rules

1. Keep the Initializr `*ApplicationTests` context-load test. Add separate
   test classes when useful. Name unit tests `<Type>Tests` and integration
   tests `<Feature>IntegrationTests` under the matching package in
   `src/test/java`; the project's test task must include both naming patterns.
2. Unit tests exercise observable behavior of one class or small domain unit
   without starting Spring. Use JUnit Jupiter assertions. Construct services
   directly with simple fakes or Mockito mocks. Do not use `@SpringBootTest`,
   `@Autowired`, or containers in unit tests.
3. Prefer real values and small fakes for deterministic domain behavior. Use
   Mockito when collaborator interaction is part of the behavior or a fake
   would add unnecessary setup. Verify returned values, state changes, and
   meaningful collaborator effects; avoid incidental call order and private
   implementation details.
4. Keep tests focused on one outcome. Cover normal behavior, boundary values,
   and promised failure behavior; do not test framework behavior or trivial
   getters and setters.
5. Do not add `src/test/resources/application.yaml`. Supply dynamic test
   endpoints with service connections or `@DynamicPropertySource`.
6. Prepare test data through the public API or application service when that
   behavior is under test. For persistence-specific cases, use small SQL
   fixtures (for example `@Sql`) to make the required database state explicit;
   avoid using a Mapper or raw SQL as incidental setup for an API test.
7. When a datasource-backed context test runs against Compose, follow
   [docker.md](docker.md). When it runs against Testcontainers, import its
   container configuration into `*ApplicationTests`. Choose one source for a
   service in a test; do not start both its Compose and Testcontainers
   instances. Do not exclude auto-configurations or remove production
   dependencies to make the context start.
8. Add Testcontainers only for tests that need real service behavior. Use the
   smallest required modules and manage their versions using the selected build
   tool and Boot dependency-management mechanism.
   Add `spring-boot-testcontainers` when using Boot's `@ServiceConnection`.
   Add `testcontainers-postgresql` for `PostgreSQLContainer`, `testcontainers`
   for `GenericContainer`, and `testcontainers-junit-jupiter` only when using
   the JUnit `@Testcontainers` extension. All are test-scoped. The PostgreSQL
   Testcontainers module does not supply the application's JDBC driver.
9. For Spring context tests, prefer Spring-managed container beans so the
   containers live as long as the cached application context. JUnit-managed
   `@Container` fields stop at the end of their test class and can leave a
   cached Spring context pointing at a stopped service.
   Reuse the same imported configuration for tests that need the same
   services. Keep service combinations separate when a test does not need
   every container; distinct combinations intentionally create distinct
   contexts. If distinct contexts must share one database, choose a lifecycle
   that outlives all of them and use unique test data. Never depend on test
   ordering or shared rows left by another test.
10. Use Testcontainers according to the service under test:
   - **PostgreSQL:** use `PostgreSQLContainer` with `@ServiceConnection` to
     provide JDBC connection details. Verify migrations, SQL behavior, and
     tenant isolation against PostgreSQL itself.
   - **Redis:** use `GenericContainer` with
     `@ServiceConnection(name = "redis")` to provide Redis connection details.
     Verify Redis serialization, expiry, and cache invalidation where these
     are part of the contract.
   - **Mailpit:** use `GenericContainer` with its SMTP port exposed and register
     `spring.mail.host` and `spring.mail.port` through
     `@DynamicPropertySource` or Spring `DynamicPropertyRegistrar`. Prefer the
     registrar with Spring-managed container beans. Send through `JavaMailSender` and verify the captured message through
     Mailpit; this checks SMTP integration, not production-provider delivery.
   Start only the containers the test exercises. Use the image pins in
   `versions.json` when they match the project's supported service versions.
11. Do not add `spring-boot-starter-test` alongside
    `spring-boot-starter-webmvc-test`. Use the matching Boot 4 `-test` starter
    for the production starter under test; the web MVC test starter is
    self-contained for web-specific support.

### Colima on macOS

If Testcontainers connects to Colima but Ryuk fails while mounting the host
socket path (for example, `operation not supported` for
`~/.colima/default/docker.sock`), point Testcontainers at the Docker socket
inside the Colima VM. Resolve the host socket for the active Colima profile
with `colima status`, then set:

```sh
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

Adjust the host path for the active profile. Prefer this socket override so
Ryuk remains enabled. Disable Ryuk only when the environment provides another
reliable container cleanup mechanism; Testcontainers otherwise relies on
Ryuk to clean up resources after abnormal JVM termination.

Boot 4 moved web test auto-configuration annotations. Import `@WebMvcTest` and
`@AutoConfigureMockMvc` from
`org.springframework.boot.webmvc.test.autoconfigure`; do not use the Boot 3
package `org.springframework.boot.test.autoconfigure.web.servlet`.

## Dependency examples

Add only the modules used by the integration tests. Maven example (use the
equivalent dependency notation for the selected build tool); the Maven parent
manages these versions.

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-testcontainers</artifactId>
	<scope>test</scope>
</dependency>
<dependency>
	<groupId>org.testcontainers</groupId>
	<artifactId>testcontainers-postgresql</artifactId>
	<scope>test</scope>
</dependency>
<dependency>
	<groupId>org.testcontainers</groupId>
	<artifactId>testcontainers-junit-jupiter</artifactId>
	<scope>test</scope>
</dependency>
```

For a `RestClient` slice test, use `spring-boot-starter-restclient-test`.
For a live-server test with `RestTestClient`, add the BOM-managed
`spring-boot-resttestclient` module in test scope.

For Redis or Mailpit `GenericContainer` usage, add
`org.testcontainers:testcontainers` if it is not already available through the
selected Testcontainers modules.
Only add `spring-boot-testcontainers` when using service connections.

## Examples

Direct construction keeps a unit test independent of Spring and Docker:

```java
class OrderServiceTests {

	@Test
	void place_returns_order_when_stock_is_available() {
		var inventory = new FakeInventory(5);
		var service = new OrderService(inventory);

		var result = service.place(new CreateOrderRequest("SKU-1", 2));

		assertEquals(2, result.quantity());
		assertEquals(3, inventory.remaining("SKU-1"));
	}
}
```

Spring-managed PostgreSQL and Redis containers can provide Boot service
connections to a context test:

```java
@TestConfiguration(proxyBeanMethods = false)
class DatabaseTestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer("postgres:18-alpine")
				.withDatabaseName("app")
				.withUsername("test")
				.withPassword("test");
	}

	@Bean
	@ServiceConnection(name = "redis")
	GenericContainer<?> redisContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
				.withExposedPorts(6379);
	}
}
```

Import this configuration in the test that needs it. Keep database and Redis
configuration separate when a test uses only one service.

Mailpit has no Boot service-connection factory. A mail integration test must
map the container's SMTP port dynamically:

```java
@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MailDeliveryIntegrationTests {

	@Container
	static final GenericContainer<?> mailpit = new GenericContainer<>(
			DockerImageName.parse("axllent/mailpit:v1.34.0"))
			.withExposedPorts(1025);

	@DynamicPropertySource
	static void mailProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.mail.host", mailpit::getHost);
		registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
	}
}
```

Adapt the lifecycle to the test: for a Spring context, keep the container
running until that context closes. Do not copy illustrative fixtures that do
not compile against the project's constructors and APIs.

## Verify

Open the changed tests and active build configuration; do not infer. Confirm
unit tests do not start Spring or Docker, integration tests start only needed
services, container lifecycle covers the Spring context, and Compose-backed
tests follow [docker.md](docker.md). If CI runs Testcontainers tests, confirm
the CI runner provides Docker and the tests execute; do not silently skip
container tests in CI when Docker is unavailable. Report applicable failures.

The template demonstrates this split with a controller validation slice in
[UserControllerTests](../assets/template-maven/src/test/java/com/example/templatemaven/user/api/UserControllerTests.java), direct-construction unit tests for
the user domain, user service, and email notification behavior, plus PostgreSQL-backed feature integration tests:
[unit tests](../assets/template-maven/src/test/java/com/example/templatemaven/user),
[email unit test](../assets/template-maven/src/test/java/com/example/templatemaven/notification/EmailNotificationServiceTests.java),
[MVC, pagination, validation, and OpenAPI integration tests](../assets/template-maven/src/test/java/com/example/templatemaven/user/UserIntegrationTests.java), [Redis cache integration tests](../assets/template-maven/src/test/java/com/example/templatemaven/user/CacheIntegrationTests.java), and [Mailpit integration tests](../assets/template-maven/src/test/java/com/example/templatemaven/notification/EmailIntegrationTests.java),
and a real-port [HTTP end-to-end test](../assets/template-maven/src/test/java/com/example/templatemaven/user/UserEndToEndTests.java). Test data for API behavior is created through the HTTP contract; persistence-specific fixtures can use SQL. Spring-managed service containers are [PostgreSQL](../assets/template-maven/src/test/java/com/example/templatemaven/PostgresTestcontainersConfiguration.java), [Redis](../assets/template-maven/src/test/java/com/example/templatemaven/RedisTestcontainersConfiguration.java), and [Mailpit](../assets/template-maven/src/test/java/com/example/templatemaven/MailpitTestcontainersConfiguration.java). The integration tests import only the services they exercise.
