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
   `threaddump` publicly. If the application intentionally has no Spring
   Security, configure and document the network boundary (for example, a
   management interface bound to loopback or a private management network with
   ingress restricted to scrapers/operators). Do not treat `access: read-only`
   as access control. If the repository contains no deployment controls, keep
   sensitive additional endpoints unexposed and report that external controls
   remain unverified.
3. Add only required endpoint dependencies and exporters. For Prometheus
   scraping, add `micrometer-registry-prometheus` with its version managed by
   the Boot parent, then expose only the `prometheus` endpoint on a protected
   management interface. Do not expose it on the public application listener
   without a verifiable network restriction or Spring Security rule. Do not
   add an exporter or tracing backend to every project by default.
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

Put safe shared endpoint defaults in
`src/main/resources/application.yaml`. Keep health as the only exposed web
endpoint unless the application has an operational need and a verified access
boundary:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health"
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never
```

When Prometheus scraping is required, add the registry dependency and put the
additional exposure and management listener in the appropriate profile file,
for example `src/main/resources/application-prod.yaml`:

```yaml
management:
  server:
    address: ${MANAGEMENT_ADDRESS:127.0.0.1}
    port: ${MANAGEMENT_PORT:8081}
  endpoints:
    web:
      exposure:
        include: "health,prometheus"
```

The loopback default prevents the separate management listener from being
reachable from other hosts. A deployment that needs a remote scraper must set
`MANAGEMENT_ADDRESS` to a private interface and provide a reviewed network
policy restricting ingress to approved scrapers/operators. Keep the profile
activation and environment values in deployment configuration, not packaged
defaults. If the repository has no deployment policy, do not claim remote
endpoint protection is verified. Add `info` only for an explicit need and
apply the same access-boundary rule.

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

Open `src/main/resources/application.yaml`, relevant
`application-{profile}.yaml` files, security rules, observations, and deployment
network policy affected by the change. Confirm the effective profile, only
intended endpoints are exposed, and every non-health endpoint has an evidenced
access boundary. Also confirm metrics tags are bounded, sensitive data is
excluded, readiness matches service behavior, and custom observations do not
duplicate built-in instrumentation. Do not mark endpoint protection verified
based only on read-only access or a README assertion. Report deployment
controls not present in the reviewed code.

The template keeps default web exposure at health only and binds the
production management listener to loopback while enabling info and Prometheus:
[base configuration](../assets/template-maven/src/main/resources/application.yaml)
and [production profile](../assets/template-maven/src/main/resources/application-prod.yaml).
Its asynchronous executor propagates context as shown in [async.md](async.md).
