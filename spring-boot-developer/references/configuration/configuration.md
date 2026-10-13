# Configuration

Read this when adding application settings, selecting environment-specific values, loading secrets, or diagnosing unexpected configuration. Keep the configuration surface explicit and fail at startup when a required value is missing or invalid.

## Version scope

This guide covers the Spring Boot 4.x baseline and existing Boot 3.5.x applications. The patterns below apply to both unless a version note says otherwise. Preserve the project's current Boot line; see [version compatibility and migration](../spring-boot-migration.md) before applying version-specific changes.

## Bind related settings with `@ConfigurationProperties`

Use a properties type for a named group of related values. It gives the group one owner, supports type conversion and validation, and can be constructed directly in tests. Use `@Value` for a single, unrelated value.

```java
@Validated
@ConfigurationProperties("app.orders")
public record OrderProperties(
        @NotNull @Positive Integer maxLinesPerOrder,
        @NotBlank String defaultCurrency,
        @NotNull Duration reservationTimeout,
        @NotNull @Valid Retry retry) {

    public record Retry(@Positive int maxAttempts, @NotNull Duration initialDelay) {}
}
```

Register properties types with scanning on the application class:

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {}
```

Use readable values for `Duration` and `DataSize`, such as `15m`, `200ms`, and `10MB`:

```yaml
app:
  orders:
    max-lines-per-order: 100
    default-currency: USD
    reservation-timeout: 15m
    retry:
      max-attempts: 3
      initial-delay: 200ms
```

Add the Validation starter managed by the project's Spring Boot line when binding Jakarta validation constraints. Validate required values at startup instead of allowing a later request to fail. See [validation](../web/validation.md) for constraint conventions.

For a one-off value, `@Value("${app.banner:ready}")` is appropriate. Avoid using it for a group that needs validation, structured nesting, or direct unit testing.

## Choose configuration files and profiles

Prefer YAML for new human-authored application configuration. Its nesting makes related settings and profile-specific overrides easier to scan. Spring Boot also supports `.properties`; follow that format when the project already uses it consistently. Do not maintain both formats for the same settings because duplicate keys make precedence harder to reason about.

Keep shared defaults in `application.yaml` and environment-specific overrides in `application-{profile}.yaml`:

```text
src/main/resources/
├── application.yaml
├── application-local.yaml
├── application-test.yaml
└── application-prod.yaml
```

A profile selects an environment or runtime configuration. It is not a feature flag. Keep the no-profile defaults safe for local startup, and do not let them point at production services. Activate a profile through the deployment environment or command line, for example `SPRING_PROFILES_ACTIVE=prod` or `--spring.profiles.active=prod`.

Avoid putting `spring.profiles.active`, `spring.profiles.include`, or profile groups in a profile-specific document. Spring Boot requires these activation properties to be declared in a non-profile-specific document.

Use `@Profile` for environment-specific infrastructure or development conveniences, such as a local data seeder. Do not use it to hide an untested production implementation of business behavior; select behavior explicitly and test each supported path.

## Externalize deployment values

Keep the artifact independent of the environment. Put non-secret defaults in the packaged configuration and provide deployment-specific values through environment variables, command-line arguments, or an external configuration location. Higher-priority property sources override lower-priority sources; command-line arguments can therefore override environment and file values. Consult the official [externalized configuration reference](https://docs.spring.io/spring-boot/4.1/reference/features/external-config.html) when config locations or precedence are part of the change.

Use placeholders to distinguish optional values from required ones:

```yaml
app:
  orders:
    max-lines-per-order: ${MAX_ORDER_LINES:100}
  payment:
    api-key: ${PAYMENT_API_KEY}
```

Spring Boot's relaxed binding maps `app.orders.max-lines-per-order` to `APP_ORDERS_MAXLINESPERORDER`: dots become underscores, dashes are removed, and the name is uppercased. Verify the resulting key for nested properties and indexed lists rather than guessing underscore placement.

Spring Boot does not automatically load a file named `.env`. If the project deliberately uses one for local development, import it explicitly and keep it out of version control:

```yaml
spring:
  config:
    import: "optional:file:.env[.properties]"
```

Commit a placeholder-only `.env.sample` for onboarding. Never copy real credentials into it.

## Keep secrets outside source control

Use the deployment platform's secret store or workload identity for production credentials. For mounted secret files, Spring Boot can import a config tree:

```yaml
spring:
  config:
    import: "configtree:/run/secrets/"
```

Files under `/run/secrets/` become properties named after the files. For example, a file named `app.payment.api-key` supplies that property. Add `optional:` only when the application can safely start without that secret.

For local work, use developer-specific environment variables or an ignored local file with non-production credentials. Do not log resolved secrets or expose configuration diagnostics publicly. Protect Actuator endpoints and review their sanitization for the exact Spring Boot version in use; custom diagnostic endpoints need their own redaction. See [observability](../observability/observability.md) and [local development](../tooling/local-development.md).

Secret handling includes ownership and rotation: document who provisions each credential, how it is rotated or revoked, and which service identity can read it. Keep CI secrets unavailable to untrusted pull request code.

## Configure a PostgreSQL datasource

Externalize the connection URL and credentials. The PostgreSQL driver is detected from the JDBC URL, so an explicit driver class is normally unnecessary:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/app}
    username: ${SPRING_DATASOURCE_USERNAME:app}
    password: ${SPRING_DATASOURCE_PASSWORD}
```

The fallback URL is suitable for local development; production credentials should come from the deployment secret store. If Spring Boot Docker Compose support supplies `ConnectionDetails`, avoid configuring a competing datasource URL. See [local development](../tooling/local-development.md). Keep Hibernate settings in [Spring Data JPA](../persistence/spring-data-jpa.md), and schema changes in [Flyway](../persistence/flyway.md).

## Treat feature flags separately

Use a validated property for a simple startup-time switch:

```java
@ConfigurationProperties("app.features.recommendations")
public record RecommendationProperties(boolean enabled) {}
```

`@ConditionalOnProperty` decides whether to create a bean when the application context starts. It does not provide a live runtime toggle. Use a feature-flag service when flags need per-user targeting, audit history, or changes without restarting the application.

## Test binding and validation

Test the properties type directly for its domain behavior. Use `ApplicationContextRunner` when the test needs to prove Spring binding, registration, or validation without starting the whole application:

```java
class OrderPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsDurationAndNestedValues() {
        contextRunner
                .withPropertyValues(
                        "app.orders.max-lines-per-order=50",
                        "app.orders.default-currency=EUR",
                        "app.orders.reservation-timeout=5m",
                        "app.orders.retry.max-attempts=2",
                        "app.orders.retry.initial-delay=100ms")
                .run(context -> assertThat(context.getBean(OrderProperties.class).reservationTimeout())
                        .isEqualTo(Duration.ofMinutes(5)));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OrderProperties.class)
    static class TestConfiguration {}
}
```

Add a failure case for missing or invalid required values. A validation annotation that is never tested can silently stop protecting the application. For test profile and context-cache choices, see [testing strategy](../testing/testing-strategy.md).

## If the project uses Spring Boot 3.5.x

Preserve its existing binding and validation setup. The external configuration model, profile files, relaxed binding, config trees, and `ApplicationContextRunner` are available on both supported lines. Check the project's managed validation starter and any changed property names against its exact Boot release; compare with the [Boot 3.5 external configuration reference](https://docs.spring.io/spring-boot/3.5/reference/features/external-config.html); do not copy Boot 4 starter coordinates into a 3.5.x build. See [version compatibility and migration](../spring-boot-migration.md).

## Gotchas

- A related group of settings is injected through scattered `@Value` fields; bind it as one validated `@ConfigurationProperties` type.
- A required secret has a convenient default; omit the default so a misconfigured deployment fails at startup.
- An environment variable is guessed from the property name; apply relaxed-binding rules, especially for hyphens and list indexes.
- A `.env` file is assumed to load automatically; import it explicitly and keep it ignored.
- A profile is used as a business feature switch; model the behavior as a property or a feature flag instead.
- `@ConditionalOnProperty` is expected to change a running application; it is evaluated while building the application context.
- A configuration or Actuator endpoint is exposed for troubleshooting; secure it and check that sensitive values are redacted.
- Configuration tests start the full application unnecessarily; use `ApplicationContextRunner` for focused binding and validation checks.

## Related

- [Validation](../web/validation.md) · [Observability](../observability/observability.md) · [Testing strategy](../testing/testing-strategy.md) · [Local development](../tooling/local-development.md) · [Spring Data JPA](../persistence/spring-data-jpa.md) · [Flyway](../persistence/flyway.md)

## References

- [Externalized Configuration](https://docs.spring.io/spring-boot/4.1/reference/features/external-config.html)
- [Profiles](https://docs.spring.io/spring-boot/4.1/reference/features/profiles.html)
- [Configuration Metadata](https://docs.spring.io/spring-boot/4.1/specification/configuration-metadata/index.html)
