# Rate limiting

## Rate Limiting with Redis

Use [AtomicWindowLimiter](../templates/AtomicWindowLimiter.java) with StringRedisTemplate.
It runs a single-key Lua script: the first allowed request creates the counter and expiry
in one SET; later requests increment only below the limit and do not extend the window.
Separate INCR and EXPIRE calls can leave a permanent counter if the client fails between them.

This is a first-request-anchored fixed window, not a sliding window or token bucket; bursts
across expiry are possible. The example restricts windows to 1ms through one day. Derive the
bounded bucket from trusted tenant/principal and operation data, not arbitrary client keys.
Keep quota/window settings consistent across callers, bound key cardinality and use a dedicated
namespace. The v2 prefix avoids reusing counters leaked by the old non-atomic example.

Redis errors propagate: choose the endpoint's explicit fail-closed or fail-open policy; do not
silently grant permission on infrastructure failure. Do not pipeline this boolean decision.
Redis restart/failover can lose counters; this is traffic control, not a financial quota ledger.
Test simultaneous requests, expiry, scope isolation and unavailable Redis against real Redis.
See [Redis INCR rate-limiter discussion](https://redis.io/docs/latest/commands/incr/) and
[Spring Data Redis scripting](https://docs.spring.io/spring-data/redis/reference/redis/scripting.html).

Use the documented algorithm only when its burst and window semantics meet the requirement. Keep quota changes and bucket versioning deliberate. Existing corrupt or TTL-less keys are not fixed by this script; inspect and recover only the owned namespace under an approved procedure. Treat unexpected script output as an infrastructure error, not an authorization decision. Do not use the example as a durable quota ledger. See [failure policy](consistency-failure-policy.md).
