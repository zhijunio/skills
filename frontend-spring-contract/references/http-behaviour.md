# HTTP Behaviour Across the Seam

## Pagination

Publish an application-owned page envelope rather than Spring Data's internal `Page` shape. A
stable offset envelope normally needs `items`, `page`, `size`, `totalElements`, and `totalPages`;
omit totals when the product only needs `hasNext`. For cursor pagination, keep the cursor opaque.
Define zero- versus one-based indexing and repeatable sort keys in the contract.

## Optimistic concurrency

Use `ETag` on reads and require `If-Match` on writes when protecting against lost updates. The
client must echo the ETag value verbatim, including quotes and weak/strong syntax; never parse it as
a number. Return 428 when a required precondition is absent and 412 when it fails. Give 412 an
application-owned problem body if the UI needs to compare current and attempted values.

Keep 409 and 412 distinct: 412 means the client's HTTP precondition failed; 409 is a broader state
conflict. Global error handling should let the feature own stale-write recovery.

## Uploads

Align three limits: Spring multipart configuration, proxy/ingress limits, and the client preflight
check. Publish the application limit rather than duplicating an unexplained constant. A proxy may
reject first with HTML, while Spring may return an empty 413, so error normalization must tolerate
both.

Client checks improve feedback but do not enforce security. Treat content type and filename as
untrusted. Let the browser create the multipart boundary; do not set multipart `Content-Type`
manually. For large objects, consider a pre-signed upload contract with explicit completion and
failure states.

## Tracing

Agree on W3C `traceparent`. The browser or frontend server starts or forwards the trace so frontend
timing and backend work can be correlated. Preserve the logical operation's trace context across a
retry rather than making the retry look unrelated.

Spring logs can contain a trace id without exposing it to the client. Return it in the problem body
or a documented response header, then show a safe reference on the error screen. If a browser must
read a custom header cross-origin, expose that header through CORS. Never put credentials, personal
data, or raw SQL in trace attributes.
