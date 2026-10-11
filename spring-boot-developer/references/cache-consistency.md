# Cache Consistency

This reference covers the consistency boundary between a database and a cache. For Spring's cache abstraction, keys, TTLs, serializers and Redis configuration, see [caching.md](caching.md). A cache is a derived copy: the database remains authoritative unless the architecture explicitly makes another store the source of truth.

## Choose the consistency promise

State the acceptable stale-read window per data set. A bounded-staleness cache may rely on TTL as a backstop; authorization, balances, inventory reservations and other correctness-critical decisions should read authoritative state or use a design with stronger coordination. Do not describe a TTL as immediate consistency.

Cache-aside is the default: read cache, load from the database on a miss, then populate the cache. Writes update the database first and invalidate or refresh the cache only after commit. Keep cache values immutable DTOs and use explicit keys; see [caching.md](caching.md).

## Invalidation and commit ordering

`@CacheEvict` can run before the surrounding database transaction commits. If commit fails, the cache is needlessly empty (usually self-correcting). `@CachePut` can publish data that a later rollback discards. For ordinary cache-aside data, invalidate after commit rather than publishing an uncommitted value.

An in-process `@TransactionalEventListener(AFTER_COMMIT)` avoids invalidating on rollback, but it is not durable: a process crash after commit can lose the event. If stale data is unacceptable or invalidation must survive crashes, write an outbox record in the database transaction and have a worker invalidate/refresh the cache. The database and Redis do not share a normal atomic transaction; make repeated invalidations idempotent.

For deployments with multiple instances, invalidation must reach every cache instance. A local in-memory cache plus single-instance eviction is not coherent across nodes. Use a shared cache or a durable/broadcast invalidation mechanism with observability for lag and failures.

Cache keys must include every dimension that changes visibility or representation, such as tenant, locale or authorization scope. Never let a cache hit bypass object-level authorization. Avoid caching permission decisions or highly sensitive data unless the invalidation and revocation window is explicit and tested.

## Races and stampedes

Invalidation has a race: a reader can load old database state, a writer commits and evicts, then the reader writes the old value back into cache. If this matters, use versioned cache entries, compare-and-set, a short TTL with acceptable staleness, or serialize refreshes for that key. Choose based on the measured impact; distributed locks add their own failure modes.

If an outbox drives invalidation, claim work with a lease or database locking strategy so multiple workers do not lose or endlessly duplicate work. Make events idempotent and retain enough version/generation information to reject an older refresh after a newer write. Monitor the age of the oldest pending invalidation; queue depth alone does not show how stale a key may be.

When many callers miss the same hot key, coordinate one refresh or use a bounded stale-while-revalidate policy. Do not let every request hit the database. Negative caching can protect a hot miss but use a short TTL and consider whether a newly created resource should become visible immediately.

## Failure behavior

Decide what happens when the cache is unavailable. For non-critical reads, falling back to the database may be acceptable if connection-pool capacity can absorb it; otherwise fail fast or shed load. Do not let a cache outage silently trigger an unbounded database stampede. Cache invalidation/refresh failures need metrics and logs without sensitive cached payloads.

## Testing

Test commit and rollback behavior, multiple cache instances or the chosen invalidation transport, the stale-reader race when relevant, and cache outage fallback. Use the real cache implementation in at least one integration test when serialization, TTL or pub/sub behavior is part of the guarantee. Testcontainers can provide Redis; see [testcontainers.md](testcontainers.md) and [transactions.md](transactions.md).

## Gotchas

- Agent assumes `AFTER_COMMIT` listeners are durable - use an outbox when invalidation must survive a process crash.
- Agent updates the cache before committing the database transaction - rollback can leave a value that never committed.
- Agent evicts a key but ignores a concurrent stale reader repopulating it - choose a race strategy if stale resurrection violates the requirement.
- Agent uses a local cache in a multi-instance service and assumes all nodes see eviction - choose shared storage or cross-instance invalidation.
- Agent treats TTL as immediate consistency - state and test the actual staleness window.
- Agent lets a Redis outage send every request to the database - bound fallback load and define a failure policy.
- Agent tests only annotations with mocks - verify real serialization and invalidation boundaries when they are part of the guarantee.

## If on Spring Boot 3.5.x

The transaction/cache ordering principles are unchanged. Redis serializer class names differ between Boot 3.5 (Jackson 2) and Boot 4 (Jackson 3); follow [caching.md](caching.md) and [boot-versions.md](boot-versions.md) for the selected version line.

## Related

- [caching.md](caching.md) · [transactions.md](transactions.md) · [messaging.md](messaging.md) · [resilience.md](resilience.md) · [testcontainers.md](testcontainers.md)
