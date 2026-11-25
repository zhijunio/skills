# template-security

Runnable Spring Boot 4 sample for servlet filter-chain security, HTTP Basic,
OIDC login, OAuth2 resource-server JWT validation, Spring Security MFA,
security-filter error responses, and outbound OAuth2 client-credentials calls
through `RestClient`. The base `template-maven` remains unauthenticated; this
separate sample shows optional security features.

## Prerequisites

- JDK 21
- Docker for the local Mailpit service
- An OIDC provider for browser login, a JWT issuer for the resource-server
  runtime flow, and a client-credentials registration plus protected API for
  the outbound call

Copy `.env.example` to `.env`, replace the identity-provider values, then run
`docker compose up -d`. Export the environment before starting the app:

```sh
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

Mailpit captures one-time-token email at `http://localhost:8025`. The demo
account is configured through `DEMO_USERNAME` and `DEMO_PASSWORD`; the password
is encoded with Spring's delegating password encoder before it enters the
in-memory user store. The in-memory account is for local examples only.

## Filter-based examples

- `GET /api/basic/report` uses HTTP Basic. The filter returns 401 with a
  `WWW-Authenticate: Basic` challenge; `/api/basic/admin/**` also requires the
  `ADMIN` role.
- `GET /api/browser/me` uses an authenticated browser session. Start OIDC with
  `/oauth2/authorization/company`; Spring Security owns the callback at
  `/login/oauth2/code/company`.
- `GET /api/resource/reports` requires a bearer token with the
  `reports:read` scope. Spring Security validates signature, issuer, and
  configured audience before authorization.
- `PartnerInventoryClient` calls the configured partner inventory API with a
  `client_credentials` token obtained by Spring Security and attached by a
  `RestClient` interceptor. The token is scoped to the application principal.
- `GET /api/mfa/profile` requires password and OTT factors. Submit an OTT
  generation request to Spring Security's `/ott/generate` filter endpoint;
  the generated token is delivered to the username's email address through
  Mailpit. `/api/mfa/admin/**` additionally requires the `ADMIN` role.

Authentication, OAuth2, and OTT endpoints are handled by Spring Security
filters. This sample has no controller mappings for protocol endpoints.
Browser session routes keep CSRF enabled. The two stateless bearer API chains
disable CSRF because they accept credentials only from their authorization
headers. API security errors use `application/problem+json` and retain the
appropriate Basic or Bearer challenge.

## Tests

Run `./mvnw verify`. The tests use MockMvc and Spring Security's request
post-processors; they do not call an external identity provider. They verify
Basic authentication and role denial, OAuth2 login, bearer scope authorization,
MFA factor requirements, security error bodies, OTT delivery through a mock
mail sender, and outbound client credentials against a local token/resource
server stub. The mocked OAuth2/JWT tests exercise the filter chain, not provider
discovery, token signatures, or issuer availability.
Taikai checks the Spring architecture baseline, and JaCoCo writes coverage to
`target/site/jacoco/`.

## Configuration

`application.yaml` reads OIDC and partner client credentials, token endpoint,
partner API base URL, and resource-server issuer, JWK Set URI, and audience
from environment variables.
The example `.env` uses placeholder provider endpoints; replace them with the
authorization server's actual metadata before trying browser login or bearer
token validation. Do not commit actual credentials. For production, replace
the in-memory `UserDetailsService` with the application's account store, use a
durable OTT service, enforce TLS, and configure a verified email delivery
provider. The in-memory OTT service is suitable only for this local demo.
