# template-maven

Spring Boot 4 MVC template with PostgreSQL, MyBatis-Plus, Flyway, Redis cache,
OpenAPI, Actuator, and a user-domain example. BootUI is available in read-only
mode in the `dev` profile. The `prod` profile disables BootUI and API
documentation.

The application enables Java 21 virtual threads for the web server and
Boot-managed executors without a custom executor. The email `@Async` workload
uses a separately configured, bounded executor so its queue and concurrency
remain an explicit bulkhead; virtual threads do not increase database or
SMTP capacity.

## Prerequisites

- JDK 21
- Docker Engine or Docker Desktop for PostgreSQL, Redis, Mailpit, and the
  Testcontainers integration tests
- Task CLI for the `task` commands (optional; Maven wrapper commands work
  without it)

## Local startup and shutdown

Copy `.env.example` to `.env`, then start the local services and application:

```sh
docker compose up -d
SPRING_PROFILES_ACTIVE=dev task run
```

Task loads `.env` into the application process. Without Task, export the
environment before starting the dev profile:

```sh
set -a
. ./.env
set +a
SPRING_PROFILES_ACTIVE=dev ./mvnw -Pdev spring-boot:run
```

Stop the local services with `task compose-down` or `docker compose down`.

## Authentication and access boundary

API endpoints have no authentication in this template, so keep the service on
a trusted local network. Health is exposed through Actuator. The `prod`
profile exposes application info and Prometheus metrics on the loopback-only
management port.

## API examples and documentation

OpenAPI is available at `/swagger-ui.html` and `/v3/api-docs` in the `dev`
profile. BootUI is read-only at `/bootui`. Mailpit captures local SMTP mail at
`http://localhost:8025`.

List users with zero-based Spring Data pagination, defaulting to page `0` and
size `20` (maximum `100`):

```sh
curl 'http://localhost:8080/api/users?page=0&size=20'
```

Create a user with `POST /api/users`:

```sh
curl -X POST http://localhost:8080/api/users \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","displayName":"Ada"}'
```

The response includes a `version` field. Send that version with
`PUT /api/users/{id}`; a stale version returns `409 Conflict` so the client
can reload the user before retrying:

```sh
curl -X PUT http://localhost:8080/api/users/1 \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@example.com","displayName":"Ada Lovelace","version":0}'
```

## Configuration

Configuration variables include `DATABASE_NAME` (defaults to the application
name), `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `DATABASE_HOST` and
`DATABASE_PORT`; `REDIS_HOST` and `REDIS_PORT`; `SMTP_HOST`, `SMTP_PORT` and
`MAIL_FROM`; and `MANAGEMENT_ADDRESS` and `MANAGEMENT_PORT` for the `prod`
profile. Optional SMTP authentication and STARTTLS are controlled with
`SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` and `SMTP_STARTTLS`. Keep real
credentials in the untracked `.env` file or your deployment environment. The
example credentials are for local development only; see [`.env.example`](.env.example).

## Build, test, formatting, and reports

Run `./mvnw spotless:apply` or `task format` to format the project. Run
`./mvnw verify` or `task verify` to test and package it; Docker must be
available for integration tests. Unit and MVC slice tests need no containers.
JaCoCo writes the report to `target/site/jacoco/`.
