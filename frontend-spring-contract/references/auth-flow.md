# Authentication Across the Contract

## Choose storage with rendering and threat model together

| Storage | Survives reload | Readable by JavaScript | Sent automatically | SSR can receive it |
| --- | --- | --- | --- | --- |
| Memory | No | Yes | No | No |
| `localStorage` / `sessionStorage` | Yes | Yes | No | No |
| `HttpOnly` cookie with deliberate transport and cross-site settings | Yes | No | Yes | Yes |

For SSR, cookies usually form the coherent contract because the rendering server receives them;
that server must deliberately forward credentials to the API. For a pure SPA, a short-lived access
token in memory with a refresh cookie can be coherent. Cookie authentication also requires an
explicit CSRF design. In HTTPS deployments, mark authentication cookies `Secure`; choose `SameSite`
for the actual cross-site request flow (`None` also requires `Secure`) and retain explicit CSRF
protection for state-changing requests. Record the chosen pairing and why it fits the deployment.

The server is the authorization authority. Client navigation checks prevent broken transitions;
they do not protect endpoints. Decoding a JWT client-side does not verify its signature and must not
authorize an operation.

## Single-flight refresh

When several requests receive 401 concurrently, share one in-flight refresh operation:

1. The first eligible 401 creates a refresh promise.
2. Later eligible 401s await the same promise.
3. On success, retry each safe request once with the new credential.
4. Clear the shared promise in `finally`, on both success and failure.
5. A refresh request or once-retried request never enters refresh again.

Queue requests; do not drop them. Treat 403 as authorization failure and never refresh it. Treat a
request with no HTTP response as network/CORS/cancellation, not as 401.

Replay GET/HEAD requests when appropriate. Replay writes only when the endpoint has an idempotency
contract or evidence proves the rejected request had no side effect. Never attach an expired access
token to the refresh call when the refresh endpoint uses a separate credential.

## SSR and test evidence

Verify the initial server render and hydrated client agree on authentication state. Browser storage
does not exist on the rendering server, so a browser-only token causes an unauthenticated first
render unless the application deliberately defers that decision.

Saved browser test state can outlive an access token. Re-authenticate per worker, ensure the suite
fits within the intended lifetime, or explicitly exercise refresh in a dedicated test. Test two
separate expiry waves: one wave proves deduplication; the second proves the shared refresh state was
cleared.
