# Local Development

The goal is that a new developer clones the repository, runs one command, and has a working application with a real database. Anything else - a wiki page of setup steps, a shared development database, "ask someone for the credentials" - costs a day per joiner and drifts out of date.

## Docker Compose

Use the local Compose workflow documented in [Docker](../deployment/docker.md). It covers Spring Boot's optional Compose integration, service connection details, lifecycle management, and the manual startup fallback.

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

## Startup information

The project scaffold adds `config/StartupInfoListener.java`, which logs the bound application URL and active profile. When springdoc is on the classpath and its endpoints are enabled, it also logs the OpenAPI JSON and Swagger UI URLs; when a `frontend/` directory is present, it hints at the Vite development URL. The template is [`StartupInfoListener.java.tmpl`](../../assets/StartupInfoListener.java.tmpl). Keep the banner informational; do not make application startup depend on optional OpenAPI/frontend files or assert on ANSI-formatted output in tests.

## Profiles

```
application.yml            shared defaults - safe if run with no profile
application-local.yml      developer machine
application-test.yml       automated tests
application-prod.yml       production (no secrets)
```

Run with `-Dspring-boot.run.profiles=local`, or set `SPRING_PROFILES_ACTIVE=local`.

The no-profile default must be **safe**. If someone runs the jar without a profile it must not reach production. See [configuration.md](../configuration/configuration.md).

Secrets never live in any of these files - a gitignored `.env` or your platform's secret store.

## mise (optional)

On macOS, Linux and Windows, [mise](https://mise.jdx.dev/) can select the project JDK and expose wrapper-backed development tasks. It is a local convenience; direct Maven/Gradle Wrapper commands remain available without it.

The generator writes the selected Temurin major and tasks to `mise.toml`. Install mise using its [official guide](https://mise.jdx.dev/getting-started.html), then run `mise install` to install the configured JDK, `mise tasks` to list commands, and `mise run verify` to run verification. Keep the configured Java major aligned with build toolchains and CI. CI and containers should select their JDK explicitly. See [maven.md](maven.md), [gradle.md](gradle.md), and [mise.md](mise.md).

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

Profile-scoped and idempotent. Development data does **not** belong in a Flyway migration - migrations run everywhere, including production, and cannot be changed afterwards ([flyway.md](../persistence/flyway.md)).

## A task runner

A short list of named commands beats a wiki page. [mise](https://mise.jdx.dev/) is one option; a `Makefile` or a `scripts/` directory works as well. What matters is that the commands are in the repository and discoverable. See [mise.md](mise.md) for mise tasks design rules and a portable starter example. Use the repository's build wrapper and actual Compose service names ([maven.md](maven.md)).

## Fast feedback

- Keep `spring.jpa.open-in-view=false` locally too. It is the setting that makes N+1 problems fail loudly in development instead of quietly in production ([spring-data-jpa.md](../persistence/spring-data-jpa.md)).
- `logging.level.org.hibernate.SQL=DEBUG` when you need to see queries. Turn it off again - it is noisy enough to hide real output.
- Boot's failure analyzers turn common startup failures into readable messages. Read the whole message before scrolling to the stack trace; the answer is usually in it.
- `spring.jpa.hibernate.ddl-auto=validate` locally as well, so schema drift fails at startup rather than at the first query.

## Gotchas

- Agent seeds development data in a Flyway migration - it reaches production and cannot be changed
- Agent writes a non-idempotent seeder - it duplicates rows on every restart
- Agent commits a `.env` or puts secrets in `application-local.yml`
- Agent makes mise mandatory or treats its selected JDK as a replacement for the project's Wrapper and toolchain configuration
- Agent debugs a devtools classloader oddity as if it were an application bug - do a full restart first
- Agent makes startup information a hard requirement for manually maintained applications - the listener is a scaffold convenience and can be omitted or adapted
- Agent turns `open-in-view` on locally to make a `LazyInitializationException` go away - that hides an N+1 until production

## Related

- [configuration.md](../configuration/configuration.md) · [flyway.md](../persistence/flyway.md) · [testcontainers.md](../testing/testcontainers.md) · [maven.md](maven.md) · [mise.md](mise.md) · [docker.md](../deployment/docker.md) · [graalvm.md](../deployment/graalvm.md)

## References

- [mise installation](https://mise.jdx.dev/getting-started.html)
- [mise configuration](https://mise.jdx.dev/configuration.html)
