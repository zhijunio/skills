# Spring Cloud Config

Spring Cloud Config centralizes external configuration. Use it when configuration must be managed
outside application releases or shared across services. For a small deployment, platform environment
variables or a secrets manager may be simpler and safer than operating a Config Server.

## Client configuration import

For current Spring Boot-based clients, use Config Data import. A required import prevents startup if
remote configuration cannot be loaded; an `optional:` import allows local startup without it:

```yaml
spring:
  config:
    import: "configserver:${CONFIG_SERVER_URL}"
```

For optional local development, use `optional:configserver:`. Do not make production configuration
optional unless the application has safe local defaults and the failure mode is intentional.
`bootstrap.yml` is not needed for Config Data import; the legacy bootstrap mechanism requires explicit
enablement and should not be copied into a newer setup without a migration reason.

## Startup, retries, and availability

Choose one explicit startup policy:

- **Required centralized config:** omit `optional:`, provide connect/read timeouts, and allow startup
  to fail if the server cannot supply valid settings.
- **Optional config:** use the `optional:` prefix only when all required settings have safe local
  defaults and operators can detect that remote config was unavailable.

Retries add time and load during an outage. If enabled, bound attempts and backoff, avoid multiplying
retries across Config Client, service discovery, and deployment restart policies, and account for the
startup deadline of the platform. Multiple imports are processed in order, with later sources taking
precedence; document that precedence rather than relying on an accidental override.

Spring Cloud Config's current documentation uses `spring.config.import` and explains fail-fast,
retry, timeouts, profile loading, and multiple URLs. Check details against the chosen train in the
[Config Client reference](https://docs.spring.io/spring-cloud-config/reference/client.html).

## Repository, labels, and secrets

- Use a version-controlled backend with reviewed changes and a rollbackable label or commit.
- Pin production releases to an immutable commit or release tag when reproducibility matters; do not
  assume a moving branch is an atomic deployment across all replicas.
- Store secrets in a dedicated secret manager or encrypted backend. Restrict repository and server
  access, protect transport with TLS, and avoid embedding credentials in URLs, source files, logs, or
  error messages.
- Treat configuration as executable operational input: restrict who can change routes, security
  settings, datasource endpoints, and feature toggles.
- Define how configuration updates reach running instances. Refresh mechanisms can create mixed
  versions during rollout; prefer a controlled restart or explicit staged refresh for changes that
  must be coordinated.

## Verification

Test startup with Config Server available and unavailable, required and optional import policies,
profile-specific configuration, malformed or missing values, credentials and authorization failures,
and rollback to a prior label. In deployment, verify that the application reports which configuration
revision it loaded without exposing secret values.

## Sources

- [Spring Cloud Config Client reference](https://docs.spring.io/spring-cloud-config/reference/client.html)
- [Spring Cloud Config Server reference](https://docs.spring.io/spring-cloud-config/reference/server.html)
- [Spring Cloud Config project](https://spring.io/projects/spring-cloud-config)
