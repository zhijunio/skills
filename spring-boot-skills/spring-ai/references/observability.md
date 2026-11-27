# AI Observability

Use Spring Boot Actuator and the Micrometer/OTel integration already selected by
the application. Spring AI instruments `ChatClient`, model calls, embeddings, and
vector stores; begin with those observations before adding custom metrics.

## Protect prompt data

Prompt and completion logging is disabled by default because it can expose user
data, retrieved documents, and secrets. Keep it disabled outside a controlled
development environment. Traces may also contain identifiers or content: review
exporters, retention, access controls, and sampling before enabling them.

## Metrics and traces

Use built-in token usage, model, and latency observations where available. Add
custom metrics only for business dimensions not represented by the built-in
instrumentation. Keep metric tags low-cardinality: do not tag with prompts,
conversation IDs, user IDs, or arbitrary model output. Use traces or a protected
audit store for high-cardinality fields when there is a clear operational need.

Avoid recording full prompts or completions in custom advisors. Record a request
identifier, operation, provider/model, duration, outcome, and token counts where
the provider returns them. Treat token-based cost as an estimate; keep model
pricing versioned outside code and use provider billing data for invoicing.

## Audit records

Add a separate audit pipeline only when the application needs business or
compliance records beyond model telemetry. A best-effort asynchronous pipeline
needs a bounded executor and handling for both task rejection and persistence
failure. Mandatory compliance records need durable delivery and explicit failure
handling; do not make a model response depend on a best-effort audit write.

## Verify

- Confirm the expected model call and token observations appear in the configured
  metrics/tracing backend.
- Check cardinality, sampling, retention, and failure behavior under load.
- Confirm prompt and completion text are absent from production logs and traces.
- Distinguish model-call telemetry from business audit requirements.

## Official reference

- [Spring AI observability](https://docs.spring.io/spring-ai/reference/observability/index.html)
