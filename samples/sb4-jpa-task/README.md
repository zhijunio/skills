# Spring Boot 4 JPA Task API

A small Spring Boot REST API for creating tasks, listing them, and moving them through `OPEN`, `IN_PROGRESS`, and `DONE` states.

## Technology

- Java 21
- Spring Boot 4.1.1 (Spring Framework 7.0.9 or later, managed by Boot)
- Maven, Spring MVC, Spring Data JPA, Jakarta Validation, PostgreSQL 18, Flyway, and `springdoc-openapi-starter-webmvc-ui` 3.1.1
- Spring Cache with Redis (30-second detail cache and explicit JSON value serialization), Prometheus metrics, ArchUnit architecture checks
- Spring Security 7 OAuth2 Resource Server; JWT issuer and audience validation with `tasks:read`, `tasks:write`, and `ops:read` scopes
- Health probes and OpenAPI documentation are public; task endpoints require the matching scope, and other routes are denied

Boot's dependency management selects compatible versions of Spring Framework, Hibernate, Jackson, JUnit, and Testcontainers. Do not pin those versions separately.

## Run locally

Prerequisites: JDK 21+, Docker with Compose V2, and an OAuth2/OIDC issuer that issues JWTs for this API. The committed Maven Wrapper downloads Maven 3.9.9, so a separate Maven installation is not required.

```sh
export SECURITY_ISSUER_URI='https://identity.example.com/realms/example'
export SECURITY_AUDIENCE='https://api.example.com/tasks'
./mvnw spring-boot:run
```

The identity provider's issuer must match `SECURITY_ISSUER_URI`, and its access tokens must include the configured audience. Obtain a token with `tasks:read` and/or `tasks:write` scopes before calling the API. `ops:read` is required for non-health Actuator endpoints. The `spring-boot-docker-compose` runtime integration discovers `compose.yaml` and starts PostgreSQL and Redis when the application starts. The `start_only` lifecycle leaves them running after the application exits; stop them with `docker compose down`. Redis is a disposable local cache and has no data volume. The API is available at `http://localhost:8080`. Flyway applies `V1__create_task.sql` at startup. The local database credentials in Compose are demo-only. For another database, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in the environment; configure `REDIS_HOST`, `REDIS_PORT`, and `REDIS_PASSWORD` for Redis. Do not commit real credentials.

To start PostgreSQL and Redis before launching the application, run `docker compose up -d postgres redis`. Add `-v` to `docker compose down` only when you intend to delete the persisted PostgreSQL demo data. Compose integration is disabled under the `prod` profile; production database and Redis settings must be supplied externally.

## API examples

Create a task (the version header is optional; when omitted, `1.0` is selected):

```sh
curl -i http://localhost:8080/api/tasks \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'API-Version: 1.0' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Prepare demo","description":"Show the task lifecycle"}'
```

List (page size is capped at 100):

```sh
curl 'http://localhost:8080/api/tasks?page=0&size=20' \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Change status (replace `<id>` with the UUID returned on create):

```sh
curl -i -X PUT http://localhost:8080/api/tasks/<id>/status \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS","expectedVersion":0}'
```

Each task response includes a `version` starting at `0`. Send that value as `expectedVersion` for the next status update; a stale value returns `409 Conflict`, and clients should fetch the task again before retrying. The database `@Version` check also detects a race that occurs after the request's version check.

Fetch one task:

```sh
curl http://localhost:8080/api/tasks/<id> \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Read the task's append-only status history (creation is recorded as `null → OPEN`):

```sh
curl http://localhost:8080/api/tasks/<id>/history \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

Task status follows `OPEN → IN_PROGRESS → DONE`. Repeating the current status is idempotent; skipping or reversing a step returns `409 Conflict` as an RFC 9457 problem.
Task changes and their history entries commit in the same transaction. History records status and time but does not claim an actor identity because this sample does not persist the authenticated subject with each change.

The API accepts the `API-Version` request header. The current supported version is `1.0`, and requests without the header use `1.0` by default. Unknown versions return `400 Bad Request`.

Interactive API documentation is available at `http://localhost:8080/swagger-ui.html`; the generated OpenAPI document is at `http://localhost:8080/v3/api-docs`. Both are public and describe the bearer JWT scheme. Swagger UI is disabled when the `prod` profile is active. Unauthenticated API requests and insufficient scopes return RFC 9457 `application/problem+json` responses with 401 and 403 status codes.

Task details are cached in Redis for at most 30 seconds; a status update evicts the corresponding entry. Redis lets instances share entries, but task records remain the source of truth in PostgreSQL. Cache read, write, and eviction failures are logged and do not fail the request; a failed eviction can leave stale data for up to the 30-second TTL. Readiness checks both PostgreSQL and Redis; liveness does not depend on either service.

Errors use RFC 9457 `application/problem+json`. Health and readiness probes are available at `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness`. Prometheus metrics are exposed at `/actuator/prometheus`; built-in HTTP, JVM, and cache metrics include the bounded `application` tag.

On Java 21+, MVC request handling uses virtual threads. No fire-and-forget task or scheduled data-changing job is configured. In the `prod` profile, Actuator is moved to port `9090` (override with `MANAGEMENT_PORT`) and must remain on an internal network.

## Verification

```sh
./mvnw verify
```

The tests include MVC slice tests for HTTP serialization, unknown JSON fields, and authentication; domain tests for the status rules; ArchUnit architecture checks; and API integration tests for scope authorization, Redis cache eviction, OpenAPI schemas, PostgreSQL 18, and Redis with Testcontainers. The integration tests apply the real Flyway migration. Security tests mock the JWT decoder and do not call a real identity provider. Docker must be available for the integration tests.
