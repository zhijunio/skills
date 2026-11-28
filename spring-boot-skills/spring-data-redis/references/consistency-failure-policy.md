# Consistency and failure policy

## Failure policy

| Usage | Redis unavailable | Recovery concern |
| --- | --- | --- |
| Disposable read cache | Optionally fall back to the source, with bounded concurrency | Avoid a database overload during an outage |
| Write-side eviction | Do not silently ignore failures | A committed DB update can leave stale cached values; use bounded TTL plus durable invalidation when needed |
| Authentication/session/authorization | Do not grant access because Redis failed | Return a controlled unavailable response or require reauthentication |
| Security rate limit | Fail closed or use an explicitly approved bounded fallback | Never silently allow unlimited traffic |

[OptionalCacheLookup](../templates/OptionalCacheLookup.java) demonstrates narrow read-side
fallback for connection failures and timeouts, with an observability callback. It deliberately
does not suppress serialization errors or write/eviction failures. Recovery restores cache
reads; do not assume `transactionAware()` makes Redis and the database one atomic transaction.

For applicable cache behavior verify concurrent misses, TTL expiration, DTO round-trips (including
money/time fields), rollback, outage fallback and recovery. A short negative-cache TTL can be
useful for repeated misses; disabling null caching is an example choice, not a universal rule.

## Verification scope

The upstream skill reports a fixture targeting Boot 4.1.0 with Java 17/21 and Redis 7.4,
covering serialization, TTL, normal concurrent loads and transaction callbacks, not failover or lease expiry. The pinned upstream tree was unavailable during the local review; these results were not independently reproduced here. See the historical
[coverage matrix](https://github.com/rrezartprebreza/spring-boot-skills/blob/b90c3dbd9699c0d0bfb81660c857ef58262168e6/verification/MATRIX.md)
before treating another minor version or an illustrative snippet as verified.

A cache put or eviction deferred until after commit can fail after the database change is durable. A concurrent reader may repopulate stale data around an eviction; choose an explicit staleness bound, versioned cache strategy or durable invalidation where required. Do not assume ordinary cache-aside provides linearizable reads. Use [application transactions](../../spring-boot-webmvc/references/transactions.md) for database transaction boundaries. For authoritative Redis data, establish durability, backup/restore, replication/failover and eviction policy with the platform owner; do not use cache-oriented fallback or blanket TTL. For distributed coordination require explicit lease, ownership and fencing semantics where needed; the cache coalescing template is not a business lock.
