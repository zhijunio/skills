# Observability

Read this when adding or changing application logs, metrics, traces, health
indicators, or Actuator endpoint exposure. Spring Boot uses Micrometer
Observation for metrics and traces. General property and secret conventions
are in [configuration.md](configuration.md); async context propagation is in
[async.md](async.md).

## When to use

- Add custom metrics, observations, or tracing export
- Change log format or correlation IDs
- Add or expose Actuator endpoints
- Add health indicators or operational metadata

## When not to use

- Provider-specific dashboards, alerting, or backend operations
- Business audit records that must be retained as domain data
- Application behavior unrelated to operational visibility

## Rules

1. Keep the three observability signals purposeful: logs explain individual
   events, metrics show aggregate trends, and traces follow work across
   boundaries. Use Spring Boot and Micrometer instrumentation before adding
   custom instrumentation.
2. Use Actuator for operational endpoints. Keep HTTP exposure limited to
   `health` by default. Expose additional endpoints only for a concrete
   operational need, and protect them with network controls or Spring
   Security. Never expose `env`, `configprops`, `beans`, `heapdump`, or
   `threaddump` publicly.
3. Add only required endpoint dependencies and exporters. For Prometheus
   scraping, add `micrometer-registry-prometheus` with its version managed by
   the Boot parent, then expose only the `prometheus` endpoint on a protected
   management interface. Do not add an exporter or tracing backend to every
   project by default.
4. Add custom metrics only when an operator can act on them. Use stable metric
   names and low-cardinality tags. Never use user IDs, request IDs, raw URLs,
   email addresses, or unbounded exception messages as metric tags. High
   cardinality details belong in traces or structured logs when safe.
5. Use Micrometer `Observation` for custom work that should contribute to
   metrics and traces together. Use low-cardinality key-values for metrics;
   put sensitive or high-cardinality details nowhere unless there is a clear
   operational need and approved data handling.
6. Preserve trace and correlation context across asynchronous work only when
   required. Follow [async.md](async.md) and verify context is cleared when a
   pooled thread is reused.
7. Log meaningful state transitions and failures with structured values.
   Do not log secrets, access tokens, passwords, full payment data, or
   unnecessary personal information. Include exception context without
   swallowing the failure.
8. Keep liveness and readiness semantics distinct. Liveness reports whether
   the process should be restarted; readiness reports whether it should
   receive traffic. Do not make liveness depend on remote services. Include a
   dependency in readiness only when its unavailability means the application
   cannot serve its contract.
9. Keep health output appropriately sanitized. Do not expose connection
   strings, credentials, internal hostnames, or detailed exception messages
   to unauthenticated callers.

## Examples

Expose health and Prometheus only when the registry dependency and protected
scrape path exist:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,prometheus"
  endpoint:
    health:
      probes:
        enabled: true
```

Create a custom observation around a meaningful application operation:

```java
@Component
class InventoryObservation {

	private final ObservationRegistry observationRegistry;

	InventoryObservation(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	void reserve(String warehouse) {
		Observation.createNotStarted("inventory.reserve", observationRegistry)
				.lowCardinalityKeyValue("warehouse", warehouse)
					.observe(() -> {
						// Perform the operation.
					});
	}
}
```

Use only bounded values such as a finite warehouse code for metric tags. Do
not tag by inventory item, customer, or request ID.

## Verify

Open the active build configuration, management configuration, security rules, custom observations,
and logging code. Confirm only intended endpoints are exposed, metrics tags
are bounded, sensitive data is excluded, readiness matches service behavior,
and custom observations do not duplicate built-in instrumentation. Report any
exposure or backend behavior that depends on deployment configuration.
