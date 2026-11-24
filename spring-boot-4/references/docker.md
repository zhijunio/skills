# Docker Compose

Read this when the task needs local PostgreSQL, Redis, or Mailpit,
`compose.yaml`, or `spring-boot-docker-compose`. The generator does not add
them.

## When to use

- Run PostgreSQL next to the app on a developer machine
- Run Redis for application caching
- Capture outbound email locally with Mailpit
- Add `compose.yaml` or `spring-boot-docker-compose`

## When not to use

- PostgreSQL engine tuning or replication → [postgresql-developer](../../postgresql-developer/SKILL.md)
- Persistence APIs → [mybatis-plus.md](mybatis-plus.md)
- Schema SQL → [flyway.md](flyway.md)
- Context-load tests with a datasource on the classpath → [testing.md](testing.md)

## Rules

1. Copy this skill's `assets/compose.yaml` to the project `compose.yaml` and
   keep only the services the application needs. Replace each retained
   service's image placeholder with its value from `versions.json`. Do not
   keep Initializr's Compose file. PostgreSQL 18 data is
   `/var/lib/postgresql`, not `/var/lib/postgresql/data`; Redis data is
   mounted at `/data`.
2. Add `spring-boot-docker-compose` as a development-time dependency. In
   Maven, mark it optional to avoid exposing it transitively; the Spring Boot
   packaging plugin excludes it from repackaged archives by default. In
   Gradle, use `developmentOnly`. If Compose-backed tests need it on the test
   runtime classpath, use Gradle's `testAndDevelopmentOnly` configuration.
   Add only matching client dependencies for retained services: the PostgreSQL
   driver for PostgreSQL,
   `spring-boot-starter-data-redis` for Redis, and `spring-boot-starter-mail`
   for Mailpit-backed email. Add `spring-boot-starter-cache` only when using
   Spring's cache abstraction. Let Spring Boot dependency management provide compatible versions where
   available; otherwise follow the project's build-tool version convention.
   Do not add `spring-boot-starter-jdbc`.

The following Maven example shows the combined dependency set when all three
services and Redis caching are used. Add only applicable dependencies using
the selected build tool.

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-docker-compose</artifactId>
	<optional>true</optional>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-cache</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
	<groupId>org.postgresql</groupId>
	<artifactId>postgresql</artifactId>
	<scope>runtime</scope>
</dependency>
```

3. When the context-load test needs the Compose-backed PostgreSQL datasource,
   Boot skips Compose during tests by default. Put this in
   `src/main/resources/application.yaml` to start the retained services during
   that test. Docker must be available when the project's test task runs. Do not add test-only auto-config excludes.

```yaml
spring:
  docker:
    compose:
      skip:
        in-tests: false
```

## Verify

Every numbered Rule in this file must hold on disk. Open `compose.yaml`,
the active build configuration, and `src/main/resources/application.yaml`; do not infer. Report
each Rule that failed.
