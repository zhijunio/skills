---
name: spring-boot-observability
description: >
  Use when adding service-wide Spring Boot 4 Actuator, Micrometer Observation and Tracing,
  OpenTelemetry or OTLP export, structured logging, health probes, dashboards, or alerts. Use [Spring AI](../spring-ai/SKILL.md) for model-specific token, prompt, and telemetry concerns.
---

# Spring Boot Observability

For version-sensitive changes, follow [shared version resolution](../spring-boot/references/version-resolution.md); if unavailable, inspect resolved dependencies and release-matched official documentation, and report missing evidence.

Instrument user-visible operations and operational boundaries, not every method.

## Baseline

- Preserve existing instrumentation and select the metrics registry/export path required by the platform. Add Actuator and only the dependencies needed for the approved signals; Prometheus scraping and OTLP push are distinct choices.
- For OpenTelemetry tracing over OTLP, use `spring-boot-starter-opentelemetry` and Boot's
  `management.opentelemetry.tracing.export.otlp.*` properties.
- Use Micrometer Observation for application metrics and traces.
- Prefer Micrometer APIs over direct OpenTelemetry APIs in application code.
- Export metrics and traces through OTLP when an OpenTelemetry collector is the platform boundary.
- Expose only required actuator endpoints and secure non-public endpoints.

## Observation conventions

- Name observations by stable operation, such as `orders.create`.
- Use bounded low-cardinality values for metrics. High-cardinality trace attributes are not automatically safe: minimize sensitive identifiers and control retention, sampling and access. Map unknown or untrusted label values to a bounded fallback before recording.
- Never tag metrics with user IDs, entity IDs, URLs containing IDs, or exception messages.
- For async boundaries, verify whether context propagation is already configured. Apply a compatible `ContextPropagatingTaskDecorator` to the actual executor when needed; test context restoration/cleanup and do not assume telemetry propagation conveys authorization.
- Enable Reactor automatic context propagation only deliberately and test it.
- Avoid annotating already instrumented MVC controllers or repositories with `@Observed`.

## Health and readiness

- Keep liveness independent from remote dependencies.
- Include external dependencies in readiness only when their failure makes this instance unable to serve useful traffic. Evaluate shared-dependency outages and fleet-wide removal risk; do not make readiness a blanket dependency checklist. When management uses a separate port, verify probes also reflect the serving connector's availability.
- Give custom health checks strict timeouts and stable detail keys.
- Keep sensitive health details hidden from unauthenticated callers.

## Export and endpoint safety

Exposure, endpoint access and authentication are separate controls. Verify the actual security filter chains, management port, ingress and network policy. A custom security chain can change Boot's default protection. Never expose heap dumps, environment values, configuration properties or runtime log-level changes publicly. Select TLS, authentication, bounded buffering and export timeouts for the actual collector. Avoid duplicate instrumentation or duplicate export when combining a Java agent, Boot tracing and registries. Sampling is a policy, not a guarantee that sensitive data is absent; sanitize before export, including baggage and MDC.

## Logging and alerts

- Emit structured logs correlated with trace and span IDs.
- Redact credentials, tokens, personal data, prompts, and request bodies by default.
- Alert on service-objective symptoms: error ratio, latency, saturation, or queue lag.
- Include a runbook and service/dependency identity in every actionable alert.

## Verify

Test the effective configuration against the selected registry and tracing exporter. Check allowed and denied management routes, health status and probe behavior under dependency failure, bounded metric series for arbitrary inputs, trace continuity across async work, and synthetic secret redaction in exported telemetry. Simulate an unavailable collector in an isolated environment; ensure bounded resource use and document telemetry loss behavior. Do not claim successful export from configuration alone.

## Examples

- Compare [bounded observations](examples/bounded-observation.java) with
  [high-cardinality observations](examples/high-cardinality-observation.java), and see the
  [observability configuration](examples/observability-config.yml).

## Official sources

- Boot observability: https://docs.spring.io/spring-boot/reference/actuator/observability.html
- Boot tracing: https://docs.spring.io/spring-boot/reference/actuator/tracing.html
- Boot structured logging: https://docs.spring.io/spring-boot/reference/features/logging.html#features.logging.structured

## Gotchas

- Agent uses high-cardinality IDs as metric tags - reserve them for traces or logs.
- Agent directly configures an OpenTelemetry SDK and disables Boot integration accidentally - prefer Boot and Micrometer support.
- Agent exposes all actuator endpoints - expose the minimum and secure them.
- Agent ties liveness to a database or broker - dependency outages can trigger restart loops.
- Agent loses trace context in `@Async` work - register context propagation explicitly.
