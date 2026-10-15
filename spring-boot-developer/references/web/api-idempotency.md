# API Idempotency

Idempotency makes a retry of one logical command produce no duplicate effect. HTTP methods such as `PUT` and `DELETE` are defined as idempotent, but the application must still uphold that contract. Use an idempotency key for non-idempotent operations such as creating a payment or order when clients may retry after a timeout.

## Define the contract

Document which operation accepts a key, who creates it, its scope, retention period, and the response for a duplicate. Scope the key by authenticated tenant/client and operation/route; a key from one tenant must never replay another tenant's response. Require high-entropy client-generated values, cap their length, and reject malformed or duplicate header fields.

The `Idempotency-Key` header is used by industry APIs, but the IETF specification is currently an expired Internet-Draft, not an RFC. Treat its syntax and error recommendations as provisional; verify its status before claiming standards compliance. Publish your own exact header syntax and duplicate-request behavior in the API contract.

## Persist the outcome atomically

For a short database-only command, store a unique record keyed by `(tenant, operation, key)` in the same transaction as the business write. Include a fingerprint of the validated semantic request, state, resulting resource ID, response status/body or a stable result reference, and expiry. Store only what is needed to replay the contract; response data may contain sensitive fields.

- First use reserves the key and performs the operation once.
- Same key and same request returns the original result without repeating side effects.
- Same key with a different request is rejected as key reuse.
- Concurrent requests with the same key follow a documented policy: wait and replay, or return a documented in-progress response (often 409 or 202 with a status resource).

Use a database unique constraint as the final concurrency guard. Do not implement idempotency as a cache check followed by a write. If a concurrent insert loses the unique-key race, let its transaction roll back and resolve the existing record afterward. Retain records longer than the client's retry horizon, and clean them with a bounded retention job.

Hash a canonical, validated command rather than raw JSON bytes, whose field ordering or whitespace may differ. Include the authenticated scope and operation in the uniqueness key. Recheck authorization before replaying a stored response.

## External side effects and long-running work

Do not hold a database transaction open while calling a payment, email or other remote provider. Persist the command outcome and an outbox message atomically, then deliver outside the transaction. Pass a stable downstream idempotency key to a provider when supported; reconcile ambiguous timeouts instead of assuming failure. See [transactions.md](../persistence/transactions.md), [messaging.md](../integration/messaging.md) and [mail.md](../integration/mail.md).

For long-running operations, persist an explicit `PENDING` state and a recoverable lease/attempt policy. Return `202 Accepted` with a status-resource `Location` when work continues asynchronously. Define how abandoned pending records are recovered and when a key may be reused.

## Security and testing

Authenticate and authorize before returning a stored result. Never use an idempotency key as an authorization token. Protect stored response data, redact keys from logs if they could correlate sensitive operations, and avoid unbounded key storage through rate and retention limits.

Integration tests should cover first success, retry after a lost response, same-key/different-payload rejection, simultaneous duplicate requests, rollback, expiry, tenant isolation, downstream timeout reconciliation and authorization changes before replay. Run uniqueness and race tests against the production database engine.

## Gotchas

- Agent equates HTTP idempotency with exactly-once execution - retries and remote timeouts still require durable state and reconciliation.
- Agent checks a key in Redis and then writes the database - concurrent requests can both pass; use a database uniqueness constraint for transactional commands.
- Agent scopes a key globally - include tenant/client and operation so results cannot cross security boundaries.
- Agent replays a stored response before rechecking authorization - permission revocation can leak old data.
- Agent compares raw JSON strings - fingerprint the validated semantic command.
- Agent expires keys before clients stop retrying - set retention beyond the documented retry window.
- Agent retries a remote side effect without a stable downstream key - the provider may perform it twice.
- Agent claims the `Idempotency-Key` header is an RFC - verify the IETF document status; the referenced draft is expired.

## Related

- [rest-controllers.md](rest-controllers.md) · [database-concurrency.md](../persistence/database-concurrency.md) · [transactions.md](../persistence/transactions.md) · [messaging.md](../integration/messaging.md) · [resilience.md](../integration/resilience.md)

## References

- [HTTPAPI Idempotency-Key Header](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/)
- [RFC 9110: HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110)
