# Spring Boot Skill Validation

A small task-management REST API generated with the local `spring-boot-developer` skill. It uses Spring Boot 4.1.2, Java 25, Maven, Spring MVC, Spring Data JPA, PostgreSQL, and Flyway.

## Run locally

Prerequisites: Java 25, Docker, and Maven Wrapper (included).

Start PostgreSQL and the application:

```bash
docker compose up -d postgres
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080/api/tasks`. Spring Boot runs the Flyway migrations at startup and Hibernate validates the resulting schema. Override `SERVER_PORT` to change the HTTP port. Stop PostgreSQL with `docker compose stop`.

## Search and pagination

`GET /api/tasks` accepts `completed`, `q`, `page`, `size`, and `sort` query parameters. For example:

```bash
curl 'http://localhost:8080/api/tasks?completed=false&q=skill&page=0&size=10&sort=createdAt,desc'
```

The response contains `content` and page metadata. The default page size is 20, requests are capped at 100, and sortable fields are limited to `title`, `createdAt`, `updatedAt`, and `completed`. An unknown sort field returns `400 ProblemDetail`. Set `TASKS_MAX_PAGE_SIZE` to a value from 1 to 100 to lower the cap; invalid values fail application startup. The cap is enforced in the service even if a client requests a larger page.

For example, start with a lower cap and request a larger page:

```bash
TASKS_MAX_PAGE_SIZE=75 ./mvnw spring-boot:run
curl 'http://localhost:8080/api/tasks?size=500'
```

## Concurrent updates

Each response includes a `version`. Send that version with a full `PUT` update. A stale version returns `409 ProblemDetail`, so the client can reload before retrying:

```bash
curl -i -X PUT http://localhost:8080/api/tasks/TASK_ID \
  -H 'Content-Type: application/json' \
  -d '{"title":"Review the skill","description":"Updated","completed":true,"version":0}'
```

The version check gives clients an explicit conflict contract; the JPA `@Version` column is the database-level guard when updates race.

## OpenAPI

API docs are disabled by default. Enable them for local inspection with `API_DOCS_ENABLED=true ./mvnw spring-boot:run`. Then open `http://localhost:8080/swagger-ui/index.html` or fetch `http://localhost:8080/v3/api-docs`. Keep the UI disabled in production and apply the deployment's access policy to the API specification.

## Health probes

Only the Actuator `health` endpoint is exposed. Liveness and readiness probes are available at `/actuator/health/liveness` and `/actuator/health/readiness`; liveness checks process state, while readiness also checks PostgreSQL. Health responses hide component names and details, and endpoints such as `/actuator/env`, `/actuator/configprops`, and `/actuator/metrics` are not exposed. This sample has no authentication and binds management to the application port, so deploy it behind network access controls and do not treat these settings as a complete production management boundary. A separate management port can reduce network exposure but does not replace authentication or network policy.

```bash
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```

## API

Create a task:

```bash
curl -i -X POST http://localhost:8080/api/tasks \
  -H 'Content-Type: application/json' \
  -d '{"title":"Review the skill","description":"Check generated defaults"}'
```

Get one with `GET /api/tasks/{id}`; replace its editable fields with `PUT /api/tasks/{id}` and a body containing `title`, `description`, `completed`, and the current `version`; delete it with `DELETE /api/tasks/{id}`. Creation returns `201 Created` with a `Location` header, deletion returns `204 No Content`, and validation, missing IDs, invalid sorting, and stale updates return RFC 9457 Problem Details.

## Verify

```bash
./mvnw test
./mvnw verify
```

`verify` also runs configuration binding tests and HTTP integration tests against PostgreSQL through Testcontainers; it requires Docker.
