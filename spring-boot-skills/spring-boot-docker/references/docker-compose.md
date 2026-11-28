# Docker and Compose

This guide covers Spring Boot container images, local PostgreSQL, Redis, Mailpit, and the
`spring-boot-docker-compose` integration. Keep Compose focused on the services the application uses.

## Build the image

- For a standard JVM image, adapt [`templates/Dockerfile`](../templates/Dockerfile) and
  [`.dockerignore`](../templates/.dockerignore). Match build and runtime Java versions to the
  project. The example uses Maven and Spring Boot layered-jar tools; adapt the wrapper commands
  for Gradle.
- When the runtime invokes `org.springframework.boot.loader.launch.JarLauncher` from exploded
  layers, extract with `--layers --launcher`. Without `--launcher`, the loader stays inside the
  executable application JAR, so the separate `spring-boot-loader` directory is empty and the
  launcher class cannot be loaded. Keep the extraction options, copied layer directories, and
  runtime entry point consistent.
- Keep dependency, loader, snapshot, and application files in separate image layers so source
  changes do not invalidate dependency layers.
- Run as a non-root user. Use a read-only filesystem where possible, and keep writable paths
  explicit and bounded, including temporary files. Keep application binaries root-owned and non-writable by the runtime user. Verify exact JAR selection; do not rely on a wildcard matching one artifact. Pin reviewed base-image digests for releases and verify the built image.
- Run tests in CI rather than treating an image build as test execution. Provide health checks
  through the orchestrator or a purpose-built probe; do not add shell checks to shell-free images.
- For AOT, Native Image, or CRaC, read [advanced runtime guidance](native-runtime.md) before
  adapting those templates. Check build profiles, JDK requirements, target platform, and runtime
  constraints.

## Configure local services

Copy [`templates/compose.yaml`](../templates/compose.yaml) to the project and remove unused
services. Replace each image placeholder with a reviewed, version-pinned image reference. Preserve
Compose variable substitutions; use required-variable syntax for credentials and defaults for
optional local ports. Add safe values to `.env.example`, ignore real `.env` files, and document the
variables.

Compose uses `.env` for interpolation; Spring Boot does not automatically load that file when the
application starts directly. Follow [Spring Boot configuration](../../spring-boot-configuration/SKILL.md)
for application properties and secret injection.

Match the volume mount to the selected PostgreSQL image major: the official 18+ image uses `/var/lib/postgresql`, while earlier majors commonly use `/var/lib/postgresql/data`. Do not reuse a data volume across major versions without an approved upgrade procedure. For cache-only Redis, do not mount a volume or
enable persistence. Do not keep Initializr's generated Compose file alongside the configured one.

## Start services through Spring Boot

Add `spring-boot-docker-compose` when Boot should own the local Compose lifecycle. In Maven, mark
it optional; the Spring Boot packaging plugin excludes it from repackaged archives by default. In
Gradle, use `developmentOnly`. If Compose-backed tests need the dependency at test runtime, use
Gradle's `testAndDevelopmentOnly` configuration.

Add only client dependencies for retained services: the PostgreSQL driver, `spring-boot-starter-data-redis`,
or `spring-boot-starter-mail`. Add `spring-boot-starter-cache` only when using Spring's cache
abstraction. Let Spring Boot manage compatible versions where available; otherwise follow the
project's build convention. Do not add `spring-boot-starter-jdbc` just for Compose integration.

This Maven snippet shows all three services and Redis caching. Keep only applicable dependencies.

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

Boot skips Compose during tests by default. If a context-load test needs a Compose-backed
datasource, set this property and make Docker available to the test task:

```yaml
spring:
  docker:
    compose:
      skip:
        in-tests: false
```

Manual `docker compose up` is a valid alternative when the application intentionally does not use
Boot's lifecycle integration. State which mode owns service startup.

## Verify

Inspect `compose.yaml`, the active build file, and `application.yaml`. Confirm retained services,
image pins, environment substitutions, volume paths, service-specific client dependencies, and
whether Boot or a manual command owns startup. For new Compose services, check that `.env.example`
and the README document the same variable names. Verify image startup under the intended UID, filesystem and resource limits, and check the image history and final filesystem for credentials. Use an isolated local Compose project with synthetic credentials; do not print resolved secret-bearing Compose configuration. Report any applicable failures.
