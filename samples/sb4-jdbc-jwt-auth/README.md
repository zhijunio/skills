# Spring Boot JWT Authentication with JdbcClient

A small Spring Boot 4 REST API that stores local accounts in PostgreSQL and uses Spring Security's JWT support for bearer access tokens. Persistence uses `JdbcClient`; Flyway owns the database schema.

## Requirements

- Java 21+
- Docker Compose

## Run locally

Start PostgreSQL:

```bash
docker compose up -d
```

Create a local RSA key pair and start the app:

```bash
mkdir -p .local/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out .local/keys/private.pem
openssl pkey -in .local/keys/private.pem -pubout -out .local/keys/public.pem
./mvnw spring-boot:run
```

The key files are ignored by Git. For another database or deployment, set `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`; supply `JWT_PUBLIC_KEY` and `JWT_PRIVATE_KEY` as file locations from the secret store, and use a unique `JWT_KEY_ID`. Never commit private keys. The sample uses RS256, 15-minute access tokens, and 30-day refresh tokens. The Compose database credentials are for local development only.

Flyway applies the schema on startup. The API listens on `http://localhost:8080`.

Run the unit tests with `./mvnw test` (Docker is not required for these tests).

## API

Register an account (passwords must contain at least 12 characters and no more than 72 UTF-8 bytes):

```bash
curl -i http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"alex@example.com","password":"a-long-demo-password"}'
```

Log in to get an access token and an opaque refresh token:

```bash
curl -s http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alex@example.com","password":"a-long-demo-password"}'
```

Use `accessToken` as a bearer token:

```bash
curl http://localhost:8080/api/users/me \
  -H 'Authorization: Bearer <accessToken>'
```

Rotate a refresh token after use. The previous token is revoked, and replaying a used token revokes its entire token family:

```bash
curl -X PUT http://localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'
```

Log out by revoking the refresh-token family:

```bash
curl -X DELETE http://localhost:8080/api/auth/logout \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'
```

List active sessions and revoke a session by its `sessionId`:

```bash
curl http://localhost:8080/api/sessions \
  -H 'Authorization: Bearer <accessToken>'

curl -X DELETE http://localhost:8080/api/sessions/<sessionId> \
  -H 'Authorization: Bearer <accessToken>'
```

| Method | Path | Access | Purpose |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Create an account; returns `409` if the email is taken. |
| `POST` | `/api/auth/login` | Public | Security filter authenticates credentials and issues an access/refresh token pair. |
| `PUT` | `/api/auth/refresh` | Public | Security authentication filter validates and rotates a refresh token, then issues a new token pair. |
| `DELETE` | `/api/auth/logout` | Public | Spring Security's logout filter revokes the associated session. |
| `GET` | `/api/sessions` | Bearer token | List the user's active sessions and identify the current session. |
| `DELETE` | `/api/sessions/{sessionId}` | Bearer token | Revoke one of the user's sessions. |
| `GET` | `/api/users/me` | Bearer token | Return the authenticated user's public profile. |
| `GET` | `/.well-known/jwks.json` | Public | Publish the RSA public key for token verification. |

User and session identifiers are URL-safe strings. The access token is signed with RS256, uses the stable user ID as `sub`, includes a session ID (`sid`) and the configured audience, and is validated by Spring Security's bearer-token filter. Each bearer request checks that the session is still active and belongs to the token subject, so session revocation and logout invalidate its access tokens on subsequent requests. Each login creates a `user_sessions` row containing user-agent and peer IP metadata; `last_refreshed_at` is updated when a refresh token is rotated and remains null until the first refresh. Refresh-token rotations stay linked to that session. Refresh tokens are random opaque values stored directly in PostgreSQL, so database access must be tightly restricted and backups protected as credential material. Expired sessions and their refresh-token records are removed by a scheduled maintenance task. Login successes/failures use Spring Security authentication events. Registration, refresh, reuse detection, and logout publish application events that are logged after their database transaction commits. Refresh-token contents are kept out of Spring authentication events.
