# Error Contract

Spring Boot may return both application-defined RFC 9457 `ProblemDetail` responses and framework-
generated error maps. Enabling problem details does not guarantee that every 4xx/5xx response has
one shape. Proxies can also return HTML or an empty body.

## Stable client model

Define a boundary result that preserves:

- HTTP status and response media type
- RFC 9457 fields: `type`, `title`, `status`, `detail`, and `instance`
- flattened extension fields from `ProblemDetail`
- validation violations in an application-owned schema
- trace id when the server publishes one
- a fallback message for empty, HTML, malformed, or unknown responses

Branch on media type and shape before reading fields. Treat browser/network failures separately
from HTTP responses; many request libraries report these without a server status.

Use a stable absolute URI for each business failure type and branch on that URI, not localized
`title` or `detail` text. Keep `detail` safe to show to users and retain diagnostic internals only
in server logs connected by a trace id.

## OpenAPI mismatch to inspect

Spring's `ProblemDetail` extensions are serialized as top-level siblings. Some generated models
describe them as a nested `properties` map. Verify the actual response and adjust the published
schema or an application-owned error DTO; do not cast the generated model until it compiles.

## UI ownership

Global HTTP handling owns authentication expiry, network/offline failures, and a final unknown-
error fallback. Feature code owns recoverable domain failures such as duplicate data, stale writes,
and validation errors because it has the context to offer the next action.

Test at least these response classes: declared problem JSON, framework error JSON, validation
failure, empty body, HTML body, and request failure before an HTTP response exists.
