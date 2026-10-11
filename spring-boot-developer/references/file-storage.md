# File Upload and Download

Treat uploaded files as untrusted input and stored files as protected resources. Spring MVC handles multipart transport; a storage adapter owns bytes and a service owns metadata, authorization and lifecycle. Start with local disk only for a single-instance demo. Production storage usually belongs in an object store or dedicated file service.

## Upload boundary

Accept `MultipartFile` in the web adapter, then pass validated content and metadata to a service. Never bind an upload directly to an entity. Set explicit request and per-file limits, and bound concurrent uploads so temporary files cannot exhaust memory or disk:

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 12MB
```

Validate the size, allowed media types and file signature. The client-provided filename and `Content-Type` are claims, not proof. If the product accepts risky formats, scan them before making them downloadable. Reject empty content unless it is a supported use case.

Apply quotas per principal/tenant as well as per request. Limit archive entry count, nesting depth, expanded size and compression ratio before extracting; a small archive can expand into a disk-exhaustion attack. Treat SVG, HTML, XML and office documents as active or complex content. Quarantine files until validation/scanning completes, and never serve untrusted active content from the authenticated application's origin unless the format is safely sanitized.

Generate an opaque storage key on the server. Keep original filenames only as sanitized display metadata. Never concatenate a submitted filename with a storage root: canonicalization mistakes create path traversal and overwrite vulnerabilities. Store files outside the executable's static-resource directory and web root; download them through an authorized handler.

## Metadata and storage boundary

Keep file metadata in the database: opaque file ID, owner or tenant, storage key, size, detected media type, checksum, original display name, creation time and scan state as needed. Keep large binary content in a file/object store by default. A storage port keeps local filesystem and object-store implementations replaceable.

The database transaction cannot atomically commit a filesystem or object-store write. Use an explicit lifecycle: stage bytes under a temporary key, persist metadata, promote the object, then mark it available; clean abandoned temporary objects with a scheduled job. For simpler demos, write bytes first and compensate by deleting them if the metadata transaction fails. Make cleanup idempotent and observable.

## Download boundary

Resolve a public opaque ID to metadata, authorize the current principal for that specific file, then stream from storage. Do not expose storage paths or allow callers to choose arbitrary keys. Avoid loading large files into `byte[]`; use `Resource` or `StreamingResponseBody` with appropriate content length when known.

Use a validated media type and a safely encoded `Content-Disposition` filename. Prefer `attachment` for untrusted or active formats, and send `X-Content-Type-Options: nosniff`. Consider range requests only when the client and storage implementation need them. Return 404 for unknown IDs and avoid leaking whether another tenant owns a file.

If downloads use object-store pre-signed URLs, authorize the file before issuing the URL and constrain it to one object, one operation and a short expiry. A pre-signed URL is a bearer capability: anyone who obtains it can use it until expiry, and later application authorization checks cannot revoke it. Keep buckets private and avoid logging the URL. Use proxy streaming when immediate revocation, per-download auditing or application-enforced rate limits are required.

## Lifecycle and testing

Model deletion explicitly: remove metadata and bytes with retryable cleanup, or mark the record pending deletion until storage confirms removal. A database row deletion alone does not remove an object, and object deletion alone can leave broken metadata.

Test size and type rejection, malformed signatures, filename traversal attempts, authorization failures, streaming headers and cleanup failures. Keep storage tests against a temporary directory or a local object-store container; test database metadata against the production database engine. See [testing-slices-persistence.md](testing-slices-persistence.md) and [testcontainers.md](testcontainers.md).

## Gotchas

- Agent trusts the upload filename, extension or MIME header - validate content and use a server-generated storage key.
- Agent stores uploads under `static/` or `public/` - serve through an authorization-aware handler.
- Agent reads an unbounded upload into memory - set multipart limits and stream large content.
- Agent limits each request but not total usage - enforce per-user/tenant storage and concurrent-upload quotas.
- Agent extracts archives without expanded-size and entry-count limits - bound decompression work and reject archive bombs.
- Agent serves user-provided SVG or HTML from the application origin - quarantine/sanitize it or serve it from an isolated origin as an attachment.
- Agent assumes object storage participates in `@Transactional` - define staging, compensation, cleanup or an outbox-driven lifecycle.
- Agent authorizes only the download route - check ownership/tenant access for each file ID.
- Agent returns user-supplied names in `Content-Disposition` without safe encoding - sanitize and encode the display filename.
- Agent deletes the metadata row and assumes the bytes disappeared - coordinate and retry storage cleanup.
- Agent treats a pre-signed URL as harmless - it grants bearer access until expiry; constrain scope and lifetime and never log it.

## Related

- [rest-controllers.md](rest-controllers.md) · [security-fundamentals.md](security-fundamentals.md) · [transactions.md](transactions.md) · [testing-integration.md](testing-integration.md)
