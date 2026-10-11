# Local Development

The goal is that a new developer clones the repository, runs one command, and has a working application with a real database. Anything else - a wiki page of setup steps, a shared development database, "ask someone for the credentials" - costs a day per joiner and drifts out of date.

## Docker Compose support

Boot can start services in `compose.yaml` as part of application startup and wire connection details automatically, but only when all prerequisites are present: the `spring-boot-docker-compose` dependency is on the runtime classpath, Docker Compose V2 is installed and available, and Boot can find a supported Compose file from the application working directory. If any prerequisite is missing (or Compose integration is disabled), starting the application does **not** start the database; run `docker compose up -d <service>` yourself before starting the app. State which mode the project uses in its README and keep the manual command as a useful fallback.

With Compose integration enabled, Boot supplies `ConnectionDetails` for recognised services, so a manually configured `spring.datasource.url` is normally unnecessary and can conflict with the discovered service. Without the integration, configure the datasource normally and start Compose yourself.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-docker-compose</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

`runtime` and `optional` matter: the dependency must not reach production, where the services are real.

```yaml
# compose.yaml
services:
  postgres:
    image: 'postgres:18-alpine'
    environment:
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app
      POSTGRES_DB: app
    ports:
      - '5432'

  redis:
    image: 'redis:7-alpine'
    ports:
      - '6379'

  mailpit:
    image: 'axllent/mailpit:v1.29'
    ports:
      - '1025'
      - '8025:8025'      # web UI on a fixed port so you can open it
```

Boot detects these by image name and applies the same `ConnectionDetails` mechanism as `@ServiceConnection` in tests ([testcontainers.md](testcontainers.md)) - the two are the same machinery, which is why local and test environments stay consistent for free.

**Leave container ports unmapped** (`- '5432'`, not `- '5432:5432'`) unless you need to attach a client. Boot reads the mapped port; a fixed one clashes with anything else running Postgres.

**Match the image tags to production.** Developing against PostgreSQL 18 and deploying to 15 is the divergence this setup exists to prevent.

Match the volume target to the image version too. The official PostgreSQL 18+ images use a major-version-specific data directory beneath `/var/lib/postgresql`; mount the volume at `/var/lib/postgresql` as recommended by that image, rather than reusing the older `/var/lib/postgresql/data` target. Do not point a new major image at an existing volume from another major version and assume it is upgraded - use the PostgreSQL upgrade procedure or a fresh demo volume.

```yaml
spring:
  docker:
    compose:
      lifecycle-management: start_and_stop   # or start_only to leave them running
```

`start_only` is often nicer day to day - containers survive application restarts, so you keep your data and skip the startup wait.
The default `start_and_stop` lifecycle starts Compose services with the app and stops them when the app exits. Choose deliberately: lifecycle management affects the developer's local database process, and stopping containers does not necessarily delete named-volume data. To start only selected services manually, use `docker compose up -d postgres`; to stop them, use `docker compose down` (avoid `-v` unless deleting data is intended).

## Devtools

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

Restarts the application when classes change and disables template and static-resource caching. Automatically disabled in a fully packaged application, and excluded from the repackaged jar - but keep `optional` so it cannot leak into another module's classpath.

Devtools is a **local convenience, not a development platform**. Its restart is a classloader trick that occasionally produces confusing failures - `ClassCastException` between two copies of the same class is the classic. If something inexplicable happens, do a full restart before debugging it.

## Profiles

```
application.yml            shared defaults - safe if run with no profile
application-local.yml      developer machine
application-test.yml       automated tests
application-prod.yml       production (no secrets)
```

Run with `-Dspring-boot.run.profiles=local`, or set `SPRING_PROFILES_ACTIVE=local`.

The no-profile default must be **safe**. If someone runs the jar without a profile it must not reach production. See [configuration.md](configuration.md).

Secrets never live in any of these files - a gitignored `.env` or your platform's secret store.

## SDKMAN! (optional)

On macOS, Linux or Windows Subsystem for Linux, [SDKMAN!](https://sdkman.io/install/) can manage per-user JDKs and optional JVM tools such as Maven and Gradle. It is a local convenience; do not make SDKMAN! a prerequisite for building the repository.

```bash
sdk list java                 # see available distributions and identifiers
sdk install java <candidate> # replace with the project-approved id from `sdk list java`
sdk use java <candidate>     # replace with that same id for this shell
java -version
```

The angle-bracketed value is a placeholder; do not type the brackets literally. See the [SDKMAN! usage guide](https://sdkman.io/usage/) for `.sdkmanrc` and `sdk env` behavior.

For a team that has agreed on an SDKMAN! distribution, commit an `.sdkmanrc` with the exact candidate identifiers, then developers can run `sdk env install` and `sdk env` in the project directory. Keep it aligned with the Java version in the Maven/Gradle build and CI. Do not commit machine-specific absolute paths.

SDKMAN! selects tools on the developer's shell `PATH`; the Maven or Gradle Wrapper remains the project build entry point and pins the build-tool distribution. Java toolchains in the build configure the JDK used for compilation and tests where supported. Verify `java -version` when diagnosing which JDK launches a local app or build. CI and containers should select their JDK explicitly instead of depending on a developer's SDKMAN! defaults. See [maven.md](maven.md) and [gradle.md](gradle.md).

## Seed data

```java
@Component
@Profile("local")
class DevDataSeeder implements ApplicationRunner {

    private final CustomerRepository customers;

    DevDataSeeder(CustomerRepository customers) {
        this.customers = customers;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (customers.count() > 0) {
            return;
        }
        customers.save(Customer.create(EmailAddress.of("dev@example.com")));
    }
}
```

Profile-scoped and idempotent. Development data does **not** belong in a Flyway migration - migrations run everywhere, including production, and cannot be changed afterwards ([flyway.md](flyway.md)).

## A task runner

A short list of named commands beats a wiki page. [Task](https://taskfile.dev/) is one option; a `Makefile` or a `scripts/` directory works as well. What matters is that the commands are in the repository and discoverable. See [taskfile.md](taskfile.md) for Taskfile design rules and a portable starter example. Use the repository's build wrapper and actual Compose service names ([maven.md](maven.md)).

## Fast feedback

- Keep `spring.jpa.open-in-view=false` locally too. It is the setting that makes N+1 problems fail loudly in development instead of quietly in production ([spring-data-jpa.md](spring-data-jpa.md)).
- `logging.level.org.hibernate.SQL=DEBUG` when you need to see queries. Turn it off again - it is noisy enough to hide real output.
- Boot's failure analyzers turn common startup failures into readable messages. Read the whole message before scrolling to the stack trace; the answer is usually in it.
- `spring.jpa.hibernate.ddl-auto=validate` locally as well, so schema drift fails at startup rather than at the first query.

## If on Boot 3.5.x

Docker Compose support (3.1+), devtools, profiles and `ConnectionDetails` all behave identically. The only difference is elsewhere in the build - Flyway arriving transitively, starter names ([boot-versions.md](boot-versions.md)).

## Gotchas

- Agent puts `spring-boot-docker-compose` on `implementation` - it must be `runtime` and `optional` so it cannot reach production
- Agent assumes Compose starts automatically without the runtime integration dependency, Docker Compose V2, and a discoverable Compose file - document prerequisites or provide a manual `docker compose up` command
- Agent adds Compose integration but leaves conflicting datasource settings without checking `ConnectionDetails` precedence - use the discovered connection or document the manual mode clearly
- Agent chooses `start_and_stop` without telling developers - application exit stops the services; state lifecycle behavior and offer `start_only` when appropriate
- Agent maps fixed host ports in `compose.yaml` - clashes with anything else running the same service
- Agent uses different image versions locally and in production - that is the divergence this setup prevents
- Agent reuses `/var/lib/postgresql/data` for the official PostgreSQL 18+ image - follow that image's major-version-specific volume layout and mount at `/var/lib/postgresql`
- Agent adds `spring.datasource.url` alongside Compose support - Boot wires it; the manual value can conflict
- Agent seeds development data in a Flyway migration - it reaches production and cannot be changed
- Agent writes a non-idempotent seeder - it duplicates rows on every restart
- Agent commits a `.env` or puts secrets in `application-local.yml`
- Agent makes SDKMAN! mandatory or treats its selected JDK as a replacement for the project's Wrapper and toolchain configuration
- Agent makes the no-profile default point at production
- Agent debugs a devtools classloader oddity as if it were an application bug - do a full restart first
- Agent turns `open-in-view` on locally to make a `LazyInitializationException` go away - that hides an N+1 until production

## Related

- [configuration.md](configuration.md) · [flyway.md](flyway.md) · [testcontainers.md](testcontainers.md) · [maven.md](maven.md) · [taskfile.md](taskfile.md) · [containerization-and-native.md](containerization-and-native.md)
