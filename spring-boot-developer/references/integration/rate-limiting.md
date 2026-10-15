# Rate Limiting and Resource Protection

Protect the application from abusive traffic and legitimate bursts that exceed finite capacity. Rate limiting caps arrivals over time; concurrency limiting caps work in flight; quotas cap cumulative usage. They solve different problems and may be combined. See [resilience.md](resilience.md) for Spring Framework concurrency limiting and retry.

## Put each limit at the right boundary

Use an API gateway or edge proxy for broad per-client/IP request protection before traffic consumes application resources. Enforce business limits in the application when the key is an authenticated user, tenant, plan or operation. A gateway rule alone cannot enforce domain quotas or object-level authorization.

Choose a policy per route and principal: token bucket for bursts with a sustained rate, fixed/sliding window for simple quotas, and a semaphore/concurrency limit for expensive work in flight. Define the window, burst allowance, cost weights and reset behavior in the API contract. Avoid one global limit that blocks health probes, callbacks or all tenants equally.

Client IP is meaningful only behind correctly configured trusted proxies. Ignore arbitrary `Forwarded` / `X-Forwarded-For` values from untrusted sources. Prefer stable authenticated identities for account quotas; avoid raw tokens or personal data as limiter keys.

## Protect resource dimensions too

Rate limits do not replace hard resource bounds. Set request/body and multipart limits, cap page size and query complexity, bound upload/storage quota, configure request and dependency timeouts, and limit expensive concurrent work. Use backpressure or `429`/`503` responses rather than queueing unbounded work. Bound executor and connection-pool queues; virtual threads do not create database capacity.

Apply cheap rejection before body parsing and expensive service work when possible. Do not use a servlet interceptor as the sole protection if the request has already consumed a large body or the route bypasses MVC. Place controls in the gateway, servlet filter, service or storage layer according to the resource they protect.

## Response and distributed behavior

Return `429 Too Many Requests` for a client-specific rate policy, with a useful `Retry-After` when the reset time is known. Use RFC 9457 `ProblemDetail` for the error body. The standardized `RateLimit` response fields in [RFC 9333](https://www.rfc-editor.org/rfc/rfc9333.html) can communicate policy and remaining quota; expose only values clients can act on, and do not promise a precise reset when the limiter is approximate.

A process-local counter is per instance and resets on restart. Use a shared limiter store or enforce at a shared gateway when the policy must hold across replicas. Decide what happens if that store is unavailable: fail open for low-risk reads, fail closed for costly or abuse-sensitive actions, or apply a conservative local fallback. Bound calls to the limiter store and do not let its outage become an application-wide outage.

## Observability and tests

Measure allowed/rejected counts, limiter-store latency/errors, queue saturation and work duration. Use bounded-cardinality tags such as route template and policy name; never tag raw IDs, IPs or tokens. Do not expose internal thresholds if doing so would help evasion.

Test the boundary and status/body/headers, burst and refill behavior, authenticated tenant isolation, trusted-proxy handling, multiple application instances and limiter-store failure policy. Use a controllable clock for deterministic algorithm tests and the real shared store for at least one distributed integration test.

## Gotchas

- Agent treats `@ConcurrencyLimit` as a request rate limiter - concurrency and arrival-rate policies are distinct.
- Agent keys a limiter on an arbitrary forwarded IP header - trust proxy headers only from configured proxy hops.
- Agent uses an in-memory counter for a multi-instance quota - each replica enforces a separate limit.
- Agent adds a rate limiter but leaves unbounded body, page, upload, queue or concurrency sizes - set hard resource bounds too.
- Agent queues every rejected burst for later - bound queues and reject overload promptly.
- Agent returns 429 without a retry hint or structured error - document retry behavior and use `ProblemDetail`.
- Agent tags metrics with raw IPs or account IDs - use route templates and bounded policy labels.
- Agent fails all reads when Redis limiter state is unavailable - define a route-specific failure policy.

## Related

- [resilience.md](resilience.md) · [security-fundamentals.md](../security/security-fundamentals.md) · [rest-controllers.md](../web/rest-controllers.md) · [configuration.md](../configuration/configuration.md) · [observability.md](../observability/observability.md) · [file-storage.md](file-storage.md)

## References

- [RFC 9333](https://www.rfc-editor.org/rfc/rfc9333.html)
