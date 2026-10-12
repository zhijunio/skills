# Contract Review Checklist

## Highest impact

- [ ] `bigint` identifiers and exact decimals cross JSON as strings.
- [ ] Date-only values stay date-only; instant formatting names the intended timezone.
- [ ] Client navigation checks have server-side authorization counterparts.
- [ ] Concurrent 401s share one refresh and a second expiry starts a new refresh.
- [ ] The client handles problem JSON, framework JSON, HTML, empty bodies, and no-response failures.

## Schema and generation

- [ ] The OpenAPI document exists, is reviewable, and is the source of truth.
- [ ] Generator name, version, and options are pinned.
- [ ] Generated code is regenerated, never hand-edited.
- [ ] CI regenerates and fails on drift.
- [ ] Required fields, enums, extensions, dates, and exact numerics match real responses.
- [ ] Wire DTOs are mapped once into domain types.

## Authentication

- [ ] Storage choice accounts for XSS, CSRF, SSR, token lifetime, refresh rotation, `Secure`, and `SameSite`.
- [ ] 401, 403, and network failures take separate paths.
- [ ] Requests retry at most once; writes require an idempotency decision.
- [ ] Initial SSR and hydrated authentication state agree.
- [ ] Saved end-to-end test state cannot silently outlive credentials.

## HTTP behaviour

- [ ] Pagination uses an application-owned stable envelope and documented index base.
- [ ] `ETag` is echoed verbatim in `If-Match`; missing and stale conditions are distinct.
- [ ] Proxy, server, and published upload limits agree.
- [ ] Upload parsing tolerates HTML and empty 413 responses.
- [ ] Trace context crosses the client/server boundary and a user-visible trace reference reaches logs.

## Evidence

- [ ] Server compile/tests pass.
- [ ] Generated client type-checks without concealment casts.
- [ ] Contract tests cover representative success and failure responses.
- [ ] Large-id, exact-decimal, west-of-UTC date, parallel-refresh, and stale-write cases are exercised.
