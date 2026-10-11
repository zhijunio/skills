# Demo Task Manager

A small Spring Boot REST API for creating tasks, listing them, and moving them through `OPEN`, `IN_PROGRESS`, and `DONE` states.

## Technology

- Java 21
- Spring Boot 4.1.1 (Spring Framework 7.0.9 or later, managed by Boot)
- Maven, Spring MVC, Spring Data JPA, Jakarta Validation, PostgreSQL 18, Flyway, springdoc OpenAPI 3.1.1
- Spring Cache with Caffeine (30-second detail cache), Prometheus metrics, ArchUnit architecture checks
- No authentication; intended for local demonstration

Boot's dependency management selects compatible versions of Spring Framework, Hibernate, Jackson, JUnit, and Testcontainers. Do not pin those versions separately.

## Run locally

Prerequisites: JDK 21+, Maven 3.6.3+, Docker, and Docker Compose V2.

```sh
./mvnw spring-boot:run
```

The `spring-boot-docker-compose` runtime integration discovers `compose.yaml` and starts PostgreSQL when the application starts. The `start_only` lifecycle leaves it running after the application exits; stop it with `docker compose down`. The API is available at `http://localhost:8080`. Flyway applies `V1__create_tasks.sql` at startup. The local database credentials in Compose are demo-only. For another database, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` in the environment; do not commit real credentials.

To start the database by itself before launching the application, run `docker compose up -d postgres`. Add `-v` to `docker compose down` only when you intend to delete the persisted demo data. Compose integration is disabled under the `prod` profile; production database settings must be supplied externally.

## API examples

Create a task (the version header is optional; when omitted, `1.0` is selected):

```sh
curl -i http://localhost:8080/api/tasks \
  -H 'API-Version: 1.0' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Prepare demo","description":"Show the task lifecycle"}'
```

List (page size is capped at 100):

```sh
curl 'http://localhost:8080/api/tasks?page=0&size=20'
```

Change status (replace `<id>` with the UUID returned on create):

```sh
curl -i -X PUT http://localhost:8080/api/tasks/<id>/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"IN_PROGRESS","expectedVersion":0}'
```

Each task response includes a `version` starting at `0`. Send that value as `expectedVersion` for the next status update; a stale value returns `409 Conflict`, and clients should fetch the task again before retrying. The database `@Version` check also detects a race that occurs after the request's version check.

Fetch one task:

```sh
curl http://localhost:8080/api/tasks/<id>
```

Read the task's append-only status history (creation is recorded as `null → OPEN`):

```sh
curl http://localhost:8080/api/tasks/<id>/history
```

Task status follows `OPEN → IN_PROGRESS → DONE`. Repeating the current status is idempotent; skipping or reversing a step returns `409 Conflict` as an RFC 9457 problem.
Task changes and their history entries commit in the same transaction. Since this demo has no authentication, history records status and time but does not claim an actor identity.

The API accepts the `API-Version` request header. The current supported version is `1.0`, and requests without the header use `1.0` by default. Unknown versions return `400 Bad Request`.

Interactive API documentation is available at `http://localhost:8080/swagger-ui.html`; the generated OpenAPI document is at `http://localhost:8080/v3/api-docs`. Swagger UI is disabled when the `prod` profile is active.

Task details are cached in-process for at most 30 seconds; a status update evicts the corresponding entry. The cache is local to one process and is intended for this single-instance demonstration.

Errors use RFC 9457 `application/problem+json`. Health and readiness probes are available at `/actuator/health`, `/actuator/health/liveness`, and `/actuator/health/readiness`. Prometheus metrics are exposed at `/actuator/prometheus`; built-in HTTP, JVM, and cache metrics include the bounded `application` tag.

On Java 21+, MVC request handling uses virtual threads. No fire-and-forget task or scheduled data-changing job is configured. In the `prod` profile, Actuator is moved to port `9090` (override with `MANAGEMENT_PORT`) and must remain on an internal network.

## Verification

```sh
./mvnw verify
```

The tests include MVC slice tests for HTTP serialization and validation, domain tests for the status rules, ArchUnit architecture checks, and API integration tests that launch PostgreSQL 18 with Testcontainers and apply the real Flyway migration. Docker must be available for the integration tests.
