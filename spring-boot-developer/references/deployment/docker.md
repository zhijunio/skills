# Docker

Read this when building an OCI image, wiring the application to PostgreSQL with Compose, or preparing a container for deployment. Prefer Spring Boot buildpacks for a standard image; use the bundled Dockerfile when the project needs its custom runtime or the skill's Docker scaffold.

## Build an image

Spring Boot's Maven and Gradle plugins can build an OCI image with Cloud Native Buildpacks. For Maven:

```bash
./mvnw spring-boot:build-image -DskipTests
```

Buildpacks are a good default when the project has no base-image, OS-package, or custom build-stage requirement. They produce a layered image and keep Dockerfile maintenance out of the project. See [Spring Boot buildpacks](https://docs.spring.io/spring-boot/4.1/reference/packaging/container-images/cloud-native-buildpacks.html).

The bundled [`Dockerfile`](../../assets/Dockerfile) is a Maven-oriented alternative used by this skill's `--docker` scaffold. It builds the app, extracts Spring Boot layers, creates a `jlink` runtime, and runs as a non-root user on a distroless base. The runtime has no shell or package manager; use orchestrator health probes and container logs for operations.

```bash
docker build -t inventory-api:local .
docker run --rm -p 8080:8080 inventory-api:local
```

The template copies the project context after resolving Maven dependencies. The `frontend/` directory is optional; `.dockerignore` keeps local dependency and build outputs out of that context. Keep the runtime base compatible with the build image's libc, Java version, and native dependencies. Read the Dockerfile comments before changing its `jlink` module list or runtime flags.

Spring Boot layered jars keep rarely changing dependencies separate from application classes. When writing your own Dockerfile, extract and copy layers in their declared order: `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, then `application`. This lets Docker reuse dependency layers when only application code changes. See [efficient container images](https://docs.spring.io/spring-boot/4.1/reference/packaging/container-images/efficient-images.html).

## Choose an image variant

The skill includes these Dockerfile assets. The generator currently copies the standard JVM Dockerfile; use the others only when their trade-offs and Maven configuration fit the project.

| Asset | Use it when | Main constraint |
| --- | --- | --- |
| [`Dockerfile`](../../assets/Dockerfile) | Standard JVM deployment | Maven build; `.dockerignore` should exclude local outputs |
| [`Dockerfile-aot`](../../assets/Dockerfile-aot) | AOT-generated application wiring can reduce startup work | Add the matching AOT Maven configuration and keep build-time bean conditions stable |
| [`Dockerfile-crac`](../../assets/Dockerfile-crac) | Fast restore from a JVM checkpoint is valuable | Linux host and CRIU capabilities; checkpoint resources must be restorable |
| [`Dockerfile-native`](../../assets/Dockerfile-native) | Native startup and memory characteristics justify native compilation | See [GraalVM Native Image](graalvm.md); native builds are slower and need compatibility checks |

Choose based on measurements from the target workload. A warmed JVM can provide better throughput and simpler diagnostics for long-running services. Native Image specifics are in [GraalVM Native Image](graalvm.md).

## Run local services with Compose

Use the project's Compose files and service names; do not hardcode container IP addresses. The generated project separates local database startup from the full application stack:

| File | Purpose |
| --- | --- |
| `compose.yaml` | PostgreSQL for local development; Spring Boot Docker Compose support can manage its lifecycle |
| `compose.full.yaml` | Application image and PostgreSQL together; added by `--docker` |

Start the full stack and inspect the resolved Compose configuration before changing deployment values:

```bash
docker compose -f compose.full.yaml config -q
docker compose -f compose.full.yaml up --build
docker compose -f compose.full.yaml logs -f
docker compose -f compose.full.yaml down
```

Use a named volume for database data. `down -v` deletes that volume and its data. Replace scaffold credentials before using the stack outside local development. Compose environment values override packaged Spring configuration; keep secrets in the deployment secret store and out of committed YAML. See [configuration](../configuration/configuration.md).

When the project uses Spring Boot's Docker Compose support, its `compose.yaml` is a development convenience. Disable that integration in tests or environments where the application should not start local containers:

```yaml
spring:
  docker:
    compose:
      enabled: false
```

For local development, Boot can start the services in `compose.yaml` with the application and wire connection details automatically. This requires the `spring-boot-docker-compose` dependency on the runtime classpath, Docker Compose V2, and a supported Compose file in the application working directory. If any prerequisite is missing or the integration is disabled, start the dependency yourself with `docker compose up -d <service>` before starting the app. Document which mode the project uses and keep the manual command as a fallback.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-docker-compose</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

The `runtime` and `optional` scopes keep this development integration out of production and downstream modules. For recognised services, Boot supplies `ConnectionDetails`; a manual `spring.datasource.url` is normally unnecessary and can conflict with the discovered service. This uses the same connection-details mechanism as `@ServiceConnection` in tests ([Testcontainers](../testing/testcontainers.md)).

Leave container ports unmapped (`- '5432'`, not `- '5432:5432'`) unless a host client needs access. Boot reads the mapped port, while a fixed host port can clash with another local service. Match local image tags to production. For PostgreSQL 18+, mount persistent data at `/var/lib/postgresql`, following the image's major-version-specific layout. Do not reuse a volume from another major version as if it were upgraded; follow PostgreSQL's upgrade procedure or use a fresh development volume.

### Redis cache with Compose

Redis is optional; add it to `compose.yaml` only when the application uses Redis. Start it manually with `docker compose up -d redis`, or let Spring Boot's Docker Compose integration manage it. An ephemeral host port avoids clashes; Boot reads the mapped port and supplies the connection details.

```yaml
services:
  redis:
    image: redis:7-alpine
    # Optional: add when the application uses Redis.
    labels:
      org.springframework.boot.service-connection: redis
    ports:
      - "6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 10
```

Include `spring-boot-docker-compose` as an optional runtime dependency and start the application from the directory containing this file. Without that integration, configure `spring.data.redis.host` and `spring.data.redis.port` for the mapped address. Redis used only as a cache does not need a named volume because its entries can be rebuilt from the database. Use persistence only when Redis stores data the system cannot reconstruct. For cache behavior and Redis-specific configuration, see the [caching guide](../persistence/caching.md).

For local email flows, [Mailpit](https://mailpit.axllent.org/docs/install/docker/) is an optional SMTP sink that captures messages for inspection instead of delivering them to real recipients. Add it to `compose.yaml` when needed, then start it with `docker compose up -d mailpit`. Publish its SMTP port when the application runs on the host, and its web UI port for browser access:

```yaml
services:
  mailpit:
    image: axllent/mailpit:v1.31
    # Optional: add for local email development and testing.
    ports:
      - "1025:1025" # SMTP for an application running on the host
      - "8025:8025" # web UI at http://localhost:8025
```

Use a reviewed Mailpit release tag and update it deliberately. Configure a host-run application with `spring.mail.host=localhost` and `spring.mail.port=1025`; an application in the same Compose network uses host `mailpit` and port `1025`. Mailpit does not need a named volume for disposable local messages. Keep it confined to local development, and use the same application mail adapter with environment-specific SMTP settings. See the [mail guide](../integration/mail.md) for Spring mail configuration.

### Optional messaging and identity services

These are optional local development services, not default project dependencies. Add only the service used by the application to `compose.yaml`; start it manually with `docker compose up -d <service>`, or let Spring Boot's Docker Compose integration manage it.

RabbitMQ works with Spring Boot's Docker Compose service connection when port `5672` is published. The management UI is optional:

```yaml
services:
  rabbitmq:
    image: rabbitmq:4-management
    # Optional: add when the application uses RabbitMQ.
    environment:
      RABBITMQ_DEFAULT_USER: dev
      RABBITMQ_DEFAULT_PASS: dev
    ports:
      - "5672"
      - "15672:15672" # management UI at http://localhost:15672
```

Use `spring-boot-starter-amqp` for Spring AMQP. Boot supplies the broker connection details when its Docker Compose integration is enabled; otherwise configure the mapped host and port. The example credentials are for local development only; replace them with environment-backed values if needed and never reuse them outside local development.

Apache Kafka's official image provides a simple single-node local broker:

```yaml
services:
  kafka:
    image: apache/kafka:4.2.2
    # Optional: add when the application uses Kafka.
    ports:
      - "9092:9092"
```

For a Spring application running on the host, set `spring.kafka.bootstrap-servers=localhost:9092`. Spring Boot Docker Compose does not provide Kafka connection details, so configure the bootstrap address explicitly. If the application also runs in Compose, configure Kafka listeners and advertised addresses for both the host and Compose network; the [Apache Kafka Docker guide](https://kafka.apache.org/42/getting-started/docker/) explains the listener setup.

Dex can provide a local OpenID Connect issuer for login and resource-server development. Mount a project-specific Dex config file; its issuer URL must be reachable by both the application and the browser, and must match the issuer in issued tokens:

```yaml
services:
  dex:
    image: ghcr.io/dexidp/dex:v2.45.1
    # Optional: add when local OIDC development needs Dex.
    command: ["serve", "/etc/dex/config.yaml"]
    ports:
      - "5556:5556"
    volumes:
      - ./dex/config.yaml:/etc/dex/config.yaml:ro
```

Configure Dex clients, redirect URIs, storage, issuer URL, and local users in `./dex/config.yaml`; use test credentials only. Dex has no Spring Boot Docker Compose service connection, so configure OAuth2 issuer/client properties in the application. See Dex's [getting started guide](https://dexidp.io/docs/getting-started/) and [local connector guide](https://dexidp.io/docs/connectors/local/).

Choose the Compose lifecycle deliberately:

```yaml
spring:
  docker:
    compose:
      lifecycle-management: start_and_stop   # or start_only
```

The default `start_and_stop` starts services with the app and stops them when it exits. `start_only` leaves them running across application restarts. Stopping containers does not necessarily delete named-volume data. Start selected services manually with `docker compose up -d postgres`; use `docker compose down` to stop them, and avoid `-v` unless deleting the data is intended.

## Tune shutdown and health checks

Allow in-flight requests to finish during a deployment, and set Spring's shutdown timeout below the container platform's termination grace period:

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 25s
```

Use Spring Boot's readiness and liveness endpoints as separate probes. A database outage should normally make an instance unready; making it fail liveness can restart every replica during the same outage. See [observability](../observability/observability.md) for probe configuration and dependency health.

Container-aware JVMs read the container memory limit. Avoid hardcoding `-Xmx` unless the workload has been measured; leave headroom for metaspace, thread stacks, direct buffers, and other native memory. `JAVA_TOOL_OPTIONS` can pass JVM flags without requiring a shell in the runtime image:

```text
-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError
```

Treat `MaxRAMPercentage` as a starting value and measure it under the actual memory limit. Spring AOT can reduce startup work without producing a native binary; lazy initialization also starts fewer beans initially, but moves startup failures to the first request and is usually unsuitable for production. See [`Dockerfile-aot`](../../assets/Dockerfile-aot) for the bundled AOT variant.

## Keep the image safe and operable

- Run as a non-root user and use a maintained base image. Pin production images by version or digest; rebuild them to receive security fixes.
- Add `.dockerignore` entries for `.git/`, `target/`, `node_modules/`, local configuration, and secrets. Keep the build context small.
- Pass secrets at runtime as environment variables or mounted files. Never put credentials in Dockerfile `ARG`, image layers, or committed Compose files.
- Let container-aware JVMs size the heap from the memory limit. Do not hardcode `-Xmx` without measuring native memory, thread stacks, and direct buffers as well.
- Configure graceful shutdown so Spring's shutdown timeout is shorter than the orchestrator's termination grace period.
- Use readiness probes to remove an instance that cannot serve traffic. Keep liveness checks focused on whether the process can make progress; do not make a shared database outage restart every replica.
- Distroless images have no shell or `curl`; do not add an in-container `HEALTHCHECK` that depends on them. Configure application probes in the orchestrator. A Compose health check can still be used for services such as PostgreSQL that include a probe command.

For Actuator probes and safe operational endpoints, see [observability](../observability/observability.md). For build profiles and supported Maven/Gradle behavior, see [Maven](../tooling/maven.md) and [Gradle](../tooling/gradle.md).

## Gotchas

- A Dockerfile is added before checking buildpacks; use buildpacks unless the project needs custom image behavior.
- The entire executable jar is copied into one layer; separate dependencies from application classes to improve cache reuse.
- The bundled Dockerfile is used for a Gradle project; the skill generator's Docker option currently targets Maven.
- The Docker build copies local build outputs or secrets into the image context; maintain `.dockerignore` and never copy real `.env` files.
- The JVM is run as root or secrets are baked into an image; use a non-root runtime and inject secrets at deployment.
- The runtime image is distroless but the guide assumes `docker exec ... sh` or an in-image `curl` health check; use logs and external probes.
- A liveness probe depends on a shared database; use readiness to signal dependency outages.
- `docker compose down -v` is used for routine cleanup; it deletes named database volumes.

## Related

- [GraalVM Native Image](graalvm.md) · [Configuration](../configuration/configuration.md) · [Observability](../observability/observability.md) · [Maven](../tooling/maven.md) · [Gradle](../tooling/gradle.md)

## References

- [Spring Boot Cloud Native Buildpacks](https://docs.spring.io/spring-boot/4.1/reference/packaging/container-images/cloud-native-buildpacks.html)
- [Efficient Container Images](https://docs.spring.io/spring-boot/4.1/reference/packaging/container-images/efficient-images.html)
- [Spring Boot Docker Compose support](https://docs.spring.io/spring-boot/4.1/reference/features/dev-services.html#features.dev-services.docker-compose)
- [Apache Kafka Docker guide](https://kafka.apache.org/42/getting-started/docker/)
- [Dex getting started](https://dexidp.io/docs/getting-started/) · [Dex local connector](https://dexidp.io/docs/connectors/local/)
- [Docker documentation](https://docs.docker.com/)
- [Docker Compose documentation](https://docs.docker.com/compose/)
- [Google Distroless](https://github.com/GoogleContainerTools/distroless)
