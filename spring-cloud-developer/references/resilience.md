# Spring Cloud Resilience

Timeouts, retries, bulkheads, and circuit breakers control how a caller behaves when a remote
dependency is slow or unavailable. Select them from the operation's semantics and the application's
execution model; do not add a resilience stack by default.

## Bound every remote call

Set a connection timeout and a response or deadline timeout for each client. A caller's total
deadline must include connection setup, retries, downstream work, and response handling. Propagate
cancellation where the client supports it. A circuit breaker does not replace a timeout: without a
timeout, one hung call can occupy resources indefinitely.

Use retries only when an operation is safe to repeat or protected by an idempotency key. Retry
transient failures, not validation, authorization, or other permanent errors. Bound both attempt
count and elapsed time, add jitter when many clients may retry together, and avoid retry
multiplication across gateways, HTTP clients, message brokers, and job schedulers.

Use bulkheads or concurrency limits when one dependency could consume all request threads or
connections. A circuit breaker is useful when failure rates should temporarily stop calls, but its
thresholds and fallback must reflect the operation's semantics. Do not return cached or fabricated
success for writes unless the domain explicitly supports that behavior.

## Choose an implementation

Spring Cloud CircuitBreaker offers integrations including Resilience4j, Spring Retry, and Spring
Framework Retry. The Framework Retry integration is based on Spring Framework 7 and does not support
reactive applications; reactive flows need an implementation that supports their execution model.
Check the [CircuitBreaker reference](https://docs.spring.io/spring-cloud-circuitbreaker/reference/)
and the project's release-train BOM before choosing a starter or API.

Keep retries at one intentional layer where possible. If an outer retry wraps a circuit breaker (or
the reverse), document which failures each layer counts and test the resulting attempt budget. Avoid
large fallback methods that hide sustained dependency failure.

## Verification and operations

Test with a controllable downstream that can return errors, delay responses, hang, and recover.
Verify timeout behavior, retry count, circuit open/half-open/close transitions, bulkhead rejection,
cancellation, and recovery. Where service discovery or client-side balancing is used, also verify
instance selection using the guidance in [service discovery](service-discovery.md) and
[HTTP clients](http-clients.md).

Observe request latency, active calls, rejected calls, breaker state, and retry count by
low-cardinality dependency name; never label metrics with raw URLs or user IDs.

## Sources

- [Spring Cloud CircuitBreaker reference](https://docs.spring.io/spring-cloud-circuitbreaker/reference/)
- [Spring Cloud LoadBalancer reference](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html)
- [Spring Cloud OpenFeign reference](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html)
