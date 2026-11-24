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

## When not to use

- Maven plugin versions or `failIfNoTests` → [maven.md](maven.md)
- Slice-test or Failsafe conventions not required by the current task

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
6. When a datasource-backed context test runs against Compose, follow
   [docker.md](docker.md). When it runs against Testcontainers, import its
   container configuration into `*ApplicationTests`. Choose one source for a
   service in a test; do not start both its Compose and Testcontainers
   instances. Do not exclude auto-configurations or remove production
   dependencies to make the context start.
7. Add Testcontainers only for tests that need real service behavior. Use the
   smallest required modules and manage their versions using the selected build
   tool and Boot dependency-management mechanism.
   Add `spring-boot-testcontainers` when using Boot's `@ServiceConnection`.
   Add `testcontainers-postgresql` for `PostgreSQLContainer`, `testcontainers`
   for `GenericContainer`, and `testcontainers-junit-jupiter` only when using
   the JUnit `@Testcontainers` extension. All are test-scoped. The PostgreSQL
   Testcontainers module does not supply the application's JDBC driver.
8. For Spring context tests, prefer Spring-managed container beans so the
   containers live as long as the cached application context. JUnit-managed
   `@Container` fields stop at the end of their test class and can leave a
   cached Spring context pointing at a stopped service.
9. Use Testcontainers according to the service under test:
   - **PostgreSQL:** use `PostgreSQLContainer` with `@ServiceConnection` to
     provide JDBC connection details. Verify migrations, SQL behavior, and
     tenant isolation against PostgreSQL itself.
   - **Redis:** use `GenericContainer` with
     `@ServiceConnection(name = "redis")` to provide Redis connection details.
     Verify Redis serialization, expiry, and cache invalidation where these
     are part of the contract.
   - **Mailpit:** use `GenericContainer` with its SMTP port exposed and set
     `spring.mail.host` and `spring.mail.port` using `@DynamicPropertySource`.
     Send through `JavaMailSender` and verify the captured message through
     Mailpit; this checks SMTP integration, not production-provider delivery.
   Start only the containers the test exercises. Use the image pins in
   `versions.json` when they match the project's supported service versions.
10. Do not add `spring-boot-starter-test` alongside
    `spring-boot-starter-webmvc-test`. Use the matching Boot 4 `-test` starter
    for the production starter under test; the web MVC test starter is
    self-contained for web-specific support.

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
	PostgreSQLContainer<?> postgresContainer() {
		return new PostgreSQLContainer<>(DockerImageName.parse("postgres:18-alpine"))
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

Open `src/test` and the active build configuration; do not infer. Confirm
unit tests do not start Spring or Docker, each integration test starts only required services,
container lifecycle covers the Spring context lifetime, and Compose-backed
tests follow [docker.md](docker.md). Report each Rule that failed.
