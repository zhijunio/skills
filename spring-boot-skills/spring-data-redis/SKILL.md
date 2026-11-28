---
name: spring-data-redis
description: >
  Use when implementing caching, session storage, rate limiting, or any Redis integration.
  Covers cache-aside pattern, key naming, TTL strategy, and serialization config.
---

# Spring Data Redis

Inspect the actual Boot, Spring Data Redis, driver, Jackson, Redis server and topology.
Preserve existing dependencies, key formats and stored-value compatibility. Identify Redis's
role before choosing TTL, serialization, consistency or failure policies. Do not add a
cache manager to a project that only needs sessions, counters or direct data access.

## Select guidance by role

| Task | Read |
| --- | --- |
| Connections, dependencies, serialization, keys, direct data access or sessions | [Configuration and serialization](references/configuration-serialization.md) |
| Declarative caching, cache-aside, TTL, invalidation or stampede control | [Caching](references/caching.md) |
| Atomic counters and request rate limits | [Rate limiting](references/rate-limiting.md) |
| Transaction coordination, recovery, outage policy or durable data | [Consistency and failure policy](references/consistency-failure-policy.md) |

Read only relevant guides. The templates target Spring Data Redis 4 and Jackson 3;
check exact APIs and the managed versions before use. Examples are illustrative, not
certified standalone applications.

## Protect data and authorization

- Distinguish disposable cache from session/authorization state, traffic counters and
  authoritative business data. Specify TTL, eviction, persistence and recovery according
  to that role. Cache values should have bounded size and a deliberate expiry policy.
- Derive key scope from trusted tenant/principal and operation data; include all dimensions
  affecting the result. Authorize before cache lookup or replay, including cache hits.
  Cache interception can bypass a method body, so do not rely on checks inside it alone.
  Use [security](../spring-boot-security/SKILL.md) and [multi-tenancy](../multi-tenancy/SKILL.md).
- Prefer stable typed serialization. Never enable unrestricted polymorphic deserialization,
  broad type allowlists or native Java deserialization for untrusted payloads. Verify
  serialized shape and mixed-version compatibility; do not blindly flush shared namespaces.
- Use atomic Redis operations/scripts for compound counter changes. Bound input, key
  cardinality and script work. Redis counters and cache locks do not provide durable
  business idempotency; see [idempotency](../idempotency-patterns/SKILL.md).
- Align cache invalidation with database commits; transaction-aware caching is not a
  distributed transaction. Decide how to recover from failed eviction after commit and
  stale refill races. Do not promise strong consistency without evidence.
- Allow bounded fallback only for explicitly disposable data. Authentication or security
  controls must not grant access simply because Redis is unavailable. No production
  FLUSHALL/FLUSHDB, broad deletes, live load tests or unrelated infrastructure changes.

## Verify

Use [Spring Boot testing](../spring-boot-testing/SKILL.md) and compatible real Redis for
serialization, expiry, concurrent requests, transaction callbacks and failure semantics.
Exercise applicable scope isolation, recovery and rolling-deployment compatibility. Record
actual versions, commands and observed results. Mock tests and illustrative examples do not
prove server behavior. Historical upstream fixture claims are not locally verified; see
[verification limitations](references/consistency-failure-policy.md).
