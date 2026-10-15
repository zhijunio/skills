# Spring Boot 4 JWT

A Spring Boot 4 / Java 25 REST API demonstrating local account registration and login through Spring Security filters. Users and password hashes are stored in PostgreSQL with `JdbcClient`; bearer tokens are RS256 JWTs. The current implementation issues access tokens only; refresh-token and active-session support are planned below.

## Scope

This sample focuses on password-based authentication, JWT issuance and validation, and the security controls around local accounts. The checklist separates implemented behavior from the target feature scope; everything under **Planned account security** is not implemented yet.

### Implemented

- Register a local account and authenticate through a Spring Security login filter.
- Store users in PostgreSQL and hash passwords with Spring Security's delegating password encoder.
- Issue short-lived RS256 access tokens and publish the active public key through JWKS.
- Validate bearer tokens and protect the current-user endpoint.
- Return RFC 9457 Problem Details for API validation and authentication errors.

### Planned account security

- **Password policy and change:** validate password length and reject common/known-compromised passwords where a suitable local list is available; allow authenticated password changes. Do not impose arbitrary periodic rotation. Define how a password change affects already issued access tokens.
- **Brute-force defense:** count failed password attempts per account, temporarily lock accounts after a configurable threshold, and clear or decay failures according to a documented policy. Return a generic login error that does not reveal whether the account exists or is locked. Include a safe unlock path and address attacker-triggered account lockout. This is account-level lockout, not request rate limiting.
- **Authentication audit:** record successful and failed login events, lockouts, password changes, and administrative unlocks with timestamp, account identifier, source IP, and User-Agent. Never record passwords, bearer tokens, or other credentials.
- **Dual-token login:** issue a short-lived JWT access token and an opaque refresh token. Rotate the refresh token on every refresh. Store only a hash of the refresh token in Redis, with an expiry matching its lifetime; do not store or log raw refresh tokens.
- **Refresh-token replay defense:** make rotation atomic in Redis so concurrent requests cannot successfully consume the same refresh token twice. Reuse of a consumed token revokes its token family/session and requires the user to sign in again. Set Redis TTLs and define behavior when Redis is unavailable; authentication operations must fail closed.
- **Active login management:** create a server-side login session at login and list a user's active sessions with device label, User-Agent, source IP, login time, and last-used time. Allow revoking one session or all sessions. The access JWT carries the session ID (`sid`); protected requests check the session is still active, so revocation takes effect before the access token expires.
- **Session and token lifecycle:** bind each refresh-token family to one login session. Logout revokes the current session and its refresh token family. Password changes and account disablement revoke all sessions. Clean up expired Redis state by TTL and update session last-used metadata without writing on every request if that would create unnecessary load.
- **Unusual-location sign-in detection:** provide a small detection extension point that receives sign-in context and can flag a login for review. Do not integrate geolocation or notification providers yet; IP location is only a signal, not proof of identity.

### Implementation constraints

- Put optional security capabilities behind explicit configuration switches. When disabled, the related flow and endpoints should be absent or return a documented unavailable response; validate incompatible settings at startup (for example, refresh-token rotation requires session tracking).
- Keep authentication processing in Spring Security's filter/provider flow. Use Spring Security's authentication events for authentication outcomes; use Spring application events for a small number of completed application actions such as session revocation or password change when a separate listener is useful.
- Avoid an event framework, generic plugin registry, or layer of pass-through services. Add an event/listener only when it separates an actual side effect from the authentication or domain operation.
- Keep unusual-login detection behind a narrow interface, such as `LoginRiskDetector`, with a simple default implementation. Do not build a notification abstraction or send email in this sample; a later feature can listen to the detected event.
- No feature switch may weaken token validation or allow a revoked session to authenticate silently. If Redis is unavailable, refresh, logout, and session-sensitive authentication fail closed.

The planned HTTP surface is:

| Endpoint | Access | Purpose |
|---|---|---|
| `POST /api/auth/login` | Public | Authenticate credentials and return access and refresh tokens; create an active session. |
| `POST /api/auth/refresh` | Refresh token | Atomically rotate the refresh token and return a new token pair. |
| `POST /api/auth/logout` | Current access token | Revoke the current session and its refresh-token family. |
| `GET /api/me/sessions` | Bearer JWT | List the current user's active login sessions. |
| `DELETE /api/me/sessions/{sessionId}` | Bearer JWT | Revoke one of the current user's sessions. |
| `DELETE /api/me/sessions` | Bearer JWT | Revoke all of the current user's sessions. |

### Deliberate boundaries

- Redis stores refresh-token hashes, token-family rotation state, and active login-session state. PostgreSQL continues to own durable user records; this design does not require a refresh-token database table.
- No request rate limiting in this sample. Account lockout demonstrates brute-force defense; production deployments should select request throttling appropriate to their infrastructure.
- MFA and social/OIDC login remain in the separate `sb4-mfa` and `sb4-oauth2-login` samples.
- No IP geolocation service, notification integration, risk-scoring platform, or production account-recovery workflow.

## Requirements

- Docker
- Java 25
- OpenSSL

## Run locally

Generate an RSA key pair and copy the environment example:

```bash
mkdir -p .local/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out .local/keys/private.pem
openssl pkey -in .local/keys/private.pem -pubout -out .local/keys/public.pem
cp .env.sample .env
```

Start PostgreSQL and the application:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

Flyway creates the user table on startup. The application imports `.env`; the default database values are for local development only. The configured public and private RSA keys are checked at startup. Keep the private key outside source control.

## API

Register an account:

```bash
curl -i http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"correct-horse-42"}'
```

Login returns a short-lived access token:

```bash
curl -i http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"correct-horse-42"}'
```

Use `accessToken` to access the protected profile endpoint:

```bash
curl http://localhost:8080/api/me -H 'Authorization: Bearer <accessToken>'
```

| Endpoint | Access | Result |
|---|---|---|
| `POST /api/auth/register` | Public | Creates a persistent user; duplicate username returns `409` |
| `POST /api/auth/login` | Public | Returns an RS256 bearer token; invalid credentials return `401` |
| `GET /api/me` | Bearer JWT | Returns the authenticated username |
| `GET /.well-known/jwks.json` | Public | Publishes the active public signing key only |

Usernames are trimmed and normalized to lowercase. Passwords are stored with Spring Security's delegating password encoder. Validation and authentication failures use RFC 9457 Problem Details. Passwords must be 8–72 characters.

## Configuration

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/jwt_demo` | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | `jwt_demo` | Database username |
| `DATABASE_PASSWORD` | `jwt_demo_local` | Local database password |
| `JWT_PUBLIC_KEY` | `file:.local/keys/public.pem` | PEM X.509 public key resource |
| `JWT_PRIVATE_KEY` | `file:.local/keys/private.pem` | PEM PKCS#8 private key resource |
| `JWT_KEY_ID` | `local-rsa-key` | JWT `kid` and JWKS key identifier |
| `JWT_ISSUER` | `sb4-jwt-api` | JWT issuer claim |
| `JWT_AUDIENCE` | `sb4-jwt-client` | Required JWT audience claim |
| `JWT_ACCESS_TOKEN_TTL` | `15m` | Access token lifetime |

Currently the application issues access tokens only and has no logout, refresh, or token-revocation endpoint. JWT validation is stateless. The planned refresh-token rotation and active-session checks will make authentication stateful through Redis: protected requests will check the access token's session ID, and refresh/logout operations will update Redis atomically. PostgreSQL remains the durable user store. Use a managed identity provider and managed secrets for production deployments.

## Verify

```bash
./mvnw verify
```

Tests use Testcontainers and require Docker.
