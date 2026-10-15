# Logging

Use Spring Boot's logging defaults unless the project has a concrete need for another backend. With the standard starters, Boot uses Logback; use SLF4J in application code. Write logs to standard output in containers and let the platform collect, rotate, and retain them.

## Set log levels

Use `application.yaml` for ordinary configuration. Keep production levels at `INFO` by default and raise a package temporarily when diagnosing a problem:

```yaml
logging:
  level:
    root: INFO
    com.example.orders: DEBUG
    org.springframework.web: INFO
```

Prefer configuration over code changes for environment-specific levels. Avoid enabling SQL or framework-wide `DEBUG` logs for long periods; they can expose data and overwhelm useful events.

## Write useful log events

Use parameterized messages and include stable identifiers that help connect an event to a request or business operation:

```java
log.info("Order placed: orderId={}, customerId={}", orderId, customerId);
log.warn("Payment provider timed out: orderId={}, provider={}", orderId, provider);
```

Log the exception object when recording a failure so the stack trace and cause chain remain available:

```java
log.error("Payment failed: orderId={}", orderId, exception);
```

Choose a level that signals the action required. `ERROR` should mean an operator or developer needs to investigate; expected client errors and handled business outcomes usually do not need error-level logs. Log an exception at one boundary where it is handled, rather than logging and rethrowing it at every layer.

Never log passwords, tokens, credentials, payment data, session identifiers, or sensitive personal data. Redact at the source, and treat request and response bodies as sensitive by default. Do not put user, order, or request identifiers in metric tags; their unbounded values belong in logs or traces.

## Emit structured logs

Spring Boot supports structured JSON logging without an additional encoder. Use a format supported by the project's Boot version and the log collector:

```yaml
logging:
  structured:
    format:
      console: ecs # or logstash, or gelf
```

Structured fields make severity, service name, trace context, and MDC values searchable without parsing a formatted message. Add custom fields deliberately; avoid copying entire request objects into the log event.

When Micrometer Tracing is configured, Boot can include trace and span IDs in log output. Use them to move from a log event to its distributed trace. For a client-provided correlation ID, validate its length and character set or generate a server-side value; never trust it for authorization.

## Customize Logback only when needed

Use `logback-spring.xml` for conditional appenders or logging behavior that cannot be expressed through properties. The `-spring` variant lets Spring Boot apply its extensions before Logback initializes. Keep container output on the console; use a rolling file appender only when the deployment owns local file collection and retention.

Async appenders can reduce logging I/O on request threads, but queue saturation can drop events and shutdown can lose buffered messages. Measure before enabling them and choose an explicit loss and shutdown policy.

## Gotchas

- Agent concatenates values into log messages - use SLF4J placeholders.
- Agent logs `exception.getMessage()` - pass the exception object to retain the stack trace.
- Agent records the same exception at every layer - log once where it is handled and let higher layers propagate it.
- Agent logs tokens, credentials, personal data, or entire payloads - redact fields and log only the identifiers needed for diagnosis.
- Agent writes container logs to local files without a collection or rotation plan - use standard output and platform collection.
- Agent adds a custom JSON encoder despite Boot structured logging - use the built-in format when it meets the collector's needs.
- Agent enables broad `DEBUG` or SQL logging in production indefinitely - scope it to a package and a diagnostic window.

## Related

- [Observability](observability.md) · [Configuration](../configuration/configuration.md) · [Error handling](../web/error-handling.md) · [Security fundamentals](../security/security-fundamentals.md)

## References

- [Spring Boot 4.1 logging](https://docs.spring.io/spring-boot/4.1/reference/features/logging.html)
- [SLF4J manual](https://www.slf4j.org/manual.html)
- [Logback documentation](https://logback.qos.ch/documentation.html)
