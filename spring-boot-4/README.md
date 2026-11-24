# Spring Boot 4

Agent skill for creating and changing Spring Boot 4 applications under one house style. Persistence is MyBatis-Plus. JSON is Jackson 3. API docs are code-first springdoc.

## Quick start

Run scripts from this directory. Set `--output-dir` to a workspace outside the skill tree:

```bash
node scripts/check-environment.mjs
node scripts/create-project.mjs \
  --name inventory-api \
  --group-id com.example \
  --output-dir /absolute/path/to/workspace
node scripts/doctor-project.mjs --strict /absolute/path/to/workspace/inventory-api
```

Defaults are Maven, Java 21, Boot from `versions.json`, MyBatis-Plus, PostgreSQL, Flyway, Jackson 3, and springdoc OpenAPI. Use `--persistence none --database none` for a web-only skeleton. The generator does not overwrite an existing target.

## Verify the skill

```bash
node test/smoke-test.mjs
```

Smoke generation needs network access to `start.spring.io` and a JDK matching `versions.json` `java.default`. `./mvnw verify` on the generated app does not require Docker.

## What phase 1 emits

A Boot 4 Maven skeleton with `@MapperScan`, Flyway, PostgreSQL Compose, Jackson 3 via `spring-boot-starter-webmvc`, and `springdoc-openapi-starter-webmvc-ui`. It keeps the Initializr `*ApplicationTests` context load. It does not copy a sample feature and does not add Testcontainers classes, slice tests, or architecture tests. Maven conventions live in [references/maven.md](references/maven.md). Remaining stack files land later.

[SKILL.md](SKILL.md) is the agent workflow. [versions.json](versions.json) is the pin file.
