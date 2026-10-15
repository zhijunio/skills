# Spring Boot 4 In-Memory JWT

A stateless REST API demonstrating local username/password registration and JWT bearer authentication. Users are stored in a concurrent in-memory map and passwords are stored as encoded hashes. It issues short-lived RS256 access tokens and does not issue refresh tokens. Restarting the application clears registered users; tokens expire without renewal.

## Requirements

- Java 21 or newer
- OpenSSL
- Maven (or the included Maven wrapper)

## Local setup

The application loads signing keys from files outside the repository. Generate a local RSA key pair and copy the placeholder environment file:

```bash
mkdir -p .local/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out .local/keys/private.pem
openssl pkey -in .local/keys/private.pem -pubout -out .local/keys/public.pem
cp .env.sample .env
```

The `.env` file is imported by Spring Boot and ignored by Git. The private key must remain private; for deployed environments, provide both key files through the platform's secret or mounted-secret mechanism and set `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` to their resource locations. The configured key pair is checked at startup.

Run the service:

```bash
./mvnw spring-boot:run
```

## API

Register a user:

```bash
curl -i http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"correct-horse-42"}'
```

Login returns a short-lived access token only:

```bash
curl -i http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"correct-horse-42"}'
```

Use `accessToken` to call the protected profile endpoint:

```bash
curl http://localhost:8080/api/me -H 'Authorization: Bearer <accessToken>'
```

| Endpoint | Access | Result |
|---|---|---|
| `POST /api/auth/register` | Public | Creates an in-memory user; duplicate username returns `409` |
| `POST /api/auth/login` | Public | Returns an RS256 bearer token; invalid credentials return `401` |
| `GET /api/me` | Bearer JWT | Returns the authenticated username |
| `GET /.well-known/jwks.json` | Public | Publishes the public signing key only |

Validation errors and authentication failures use RFC 9457 Problem Details. Passwords must be 8–72 characters. There is no logout or token revocation endpoint; issued tokens remain valid until expiry. Use an external identity provider and persistent user store for production systems.

## Configuration

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `JWT_PUBLIC_KEY` | `file:.local/keys/public.pem` | PEM X.509 public key resource |
| `JWT_PRIVATE_KEY` | `file:.local/keys/private.pem` | PEM PKCS#8 private key resource |
| `JWT_KEY_ID` | `local-rsa-key` | JWT `kid` and JWKS key identifier |
| `JWT_ISSUER` | `memory-jwt-api` | JWT issuer claim |
| `JWT_AUDIENCE` | `memory-jwt-client` | Required JWT audience claim |
| `JWT_ACCESS_TOKEN_TTL` | `15m` | Access token lifetime |

## Verify

```bash
./mvnw verify
```
