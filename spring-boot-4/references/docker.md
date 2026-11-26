# Docker and Docker Compose

Read this when the task needs an application image, local PostgreSQL, Redis, or
Mailpit, `compose.yaml`, or `spring-boot-docker-compose`. The generator does not
add them.

## When to use

- Run PostgreSQL next to the app on a developer machine
- Run Redis for application caching
- Capture outbound email locally with Mailpit
- Add `compose.yaml` or `spring-boot-docker-compose`
- Build and run the application as a container
- Build a GraalVM Native Image, Spring AOT, or CRaC container

## When not to use

- PostgreSQL engine tuning or replication → [postgresql-developer](../../postgresql-developer/SKILL.md)
- Persistence APIs → [mybatis-plus.md](mybatis-plus.md)
- Schema SQL → [flyway.md](flyway.md)
- Context-load tests with a datasource on the classpath → [testing.md](testing.md)

## Rules

1. For a JVM application image, start from
   [`assets/Dockerfile`](../assets/Dockerfile) and
   [`assets/dockerignore`](../assets/dockerignore). The template expects Maven,
   a Java 21 toolchain, and the Spring Boot layered-jar tools. Set the build and
   runtime images to match the Java version in the project. For Gradle, adapt
   the wrapper and build commands to the project. Keep build-time tests in CI
   rather than silently treating a container build as test execution.
2. For AOT, Native Image, or CRaC image assets, read
   [native deployment](native-deployment.md) first. Check their build profiles,
   JDK requirements, target platform, and runtime constraints before use.
3. Keep the runtime image on a supported JRE and run as a non-root user. Keep
   the layered application files in separate image layers so source changes do
   not invalidate dependency layers.
4. Provide health checks through the orchestrator or a purpose-built probe.
   Do not add shell-based checks to a shell-free image.

5. Copy this skill's `assets/compose.yaml` to the project `compose.yaml` and
   keep only the services the application needs. Replace each retained
   service's image placeholder with its value from `versions.json`, while
   preserving the Compose `${VARIABLE}` substitutions. Do not replace these
   substitutions with fixed host ports, credentials, or service settings.
   Use required-variable syntax for credentials and defaults for optional
   values such as local host ports. Put safe local examples in `.env.example`,
   ignore real `.env` files in `.gitignore`, and document each variable.
   Follow [configuration.md](configuration.md): Compose uses `.env` for
   interpolation, but Spring Boot does not automatically load it when the app
   is launched directly.
   Do not keep Initializr's Compose file. PostgreSQL 18 data is
   `/var/lib/postgresql`, not `/var/lib/postgresql/data`. For cache-only Redis,
   do not mount a persistence volume or enable Redis persistence.
6. Add `spring-boot-docker-compose` as a development-time dependency. In
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

7. When the context-load test needs the Compose-backed PostgreSQL datasource,
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

For new local Compose services, verify the generated file retains the template's
environment-variable substitutions, including required credentials and
optional port defaults. Verify `.env.example` and the README agree with the
variable names. Also verify the build file contains the optional
`spring-boot-docker-compose` dependency as well as client dependencies for the
retained services. Verify the application starts services from the project's
`compose.yaml` when Compose lifecycle integration is intended; a documented
manual `docker compose up` command does not replace this dependency rule. If
the application deliberately uses manual Compose only, state that exception
and do not claim the skill's Compose integration rule is satisfied.

## Verify

For a new local-service setup, check every rule. For an existing setup, check
rules relevant to the requested service or configuration change. Open
`compose.yaml`, the active build configuration, and
`src/main/resources/application.yaml` as needed; do not infer. Confirm retained
Compose services, image pins, environment substitutions, volume paths, the
optional Compose dependency, client dependencies, and whether Boot or a manual
command owns service lifecycle. Report applicable failures.

The template's Compose file mirrors the shared
[`assets/compose.yaml`](../assets/compose.yaml) with concrete image pins and
the app-name database default:
[`assets/template-maven/compose.yaml`](../assets/template-maven/compose.yaml).
