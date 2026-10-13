# Service Discovery and Resilience

Service discovery answers where a service instance can be reached. Client-side load balancing
chooses among instances. Timeouts, bounded retries, bulkheads, and circuit breakers control what a
caller does when a remote dependency is slow or unavailable. These mechanisms solve different
problems and should be selected from the deployment topology, not added as a default microservice
stack.

## Decide whether a registry is needed

Use platform-native DNS or service routing when the deployment platform already provides stable
service names and health-aware load balancing. Add a registry such as Eureka, Consul, or Kubernetes
Discovery when applications need dynamic instance lists or routing metadata that the platform does
not provide. Avoid running a second registry without a clear need.

With Eureka, a client starter can register the application and provide discovery to clients. Give
each service a stable `spring.application.name`, secure registry communication, and decide how health,
zones, metadata, and stale instances affect routing. Treat registry responses as routing input, not
as proof that a service is authorized to receive a request.

The [Spring Cloud Netflix reference](https://docs.spring.io/spring-cloud-netflix/reference/) documents
Eureka clients, servers, and Spring Cloud LoadBalancer integration. Prefer the Spring `DiscoveryClient`
abstraction over a native vendor client unless vendor-specific behavior is required.

## Bound every remote call

Set a connection timeout and a response/deadline timeout for each client. A caller's total deadline
must include connection setup, retries, downstream work, and response handling. Propagate cancellation
where the client supports it. A circuit breaker does not replace a timeout: without a timeout, one
hung call can occupy resources indefinitely.

Use retries only when the operation is safe to repeat or protected by an idempotency key. Retry
transient failures, not validation, authorization, or other permanent errors. Bound both attempt count
and elapsed time, add jitter when many clients may retry together, and avoid retry multiplication
across gateways, HTTP clients, message brokers, and job schedulers.

Use bulkheads or concurrency limits when one dependency could consume all request threads or
connections. A circuit breaker is useful when failure rates should temporarily stop calls, but its
thresholds and fallback must reflect the operation's semantics. Do not return cached or fabricated
success for writes unless the domain explicitly supports that behavior.

## Pick the resilience implementation by stack and version

Spring Cloud CircuitBreaker currently offers Resilience4j, Spring Retry, and Spring Framework Retry
integrations. The Framework Retry implementation is based on Spring Framework 7 and does not support
reactive applications; reactive flows should use an implementation that supports their execution
model. Check the [CircuitBreaker reference](https://docs.spring.io/spring-cloud-circuitbreaker/reference/)
and release-train BOM before choosing a starter or API.

Keep retries at one intentional layer where possible. If an outer retry wraps a circuit breaker (or
the reverse), document which failures each layer counts and test the resulting attempt budget. Avoid
large fallback methods that hide sustained dependency failure.

## Verification and operations

Test with a controllable downstream that can return errors, delay responses, hang, and recover. Verify
instance selection, timeout behavior, retry count, circuit open/half-open/close transitions, bulkhead
rejection, cancellation, and recovery. Observe request latency, active calls, rejected calls, breaker
state, and retry count by low-cardinality dependency name; never label metrics with raw URLs or user
IDs.

## Sources

- [Spring Cloud Netflix reference](https://docs.spring.io/spring-cloud-netflix/reference/)
- [Spring Cloud CircuitBreaker reference](https://docs.spring.io/spring-cloud-circuitbreaker/reference/)
- [Spring Cloud LoadBalancer reference](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html)
