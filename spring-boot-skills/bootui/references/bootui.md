# BootUI

Read this when adding or configuring BootUI in a Spring Boot application. BootUI is a local
developer console for runtime inspection; it does not replace Actuator, production monitoring,
alerting, or APM.

## Compatibility and dependency

The setup guide targets Spring Boot 4.x and Java 17+. The same starter supports Spring MVC and
WebFlux; keep the application's existing web starter. Preserve an existing compatible BootUI version. Before adding or upgrading, verify the
version and compatibility using the [official setup guide](https://www.julien-dubois.com/boot-ui/setup)
and the published artifact metadata. The dependency versions below are examples, not a live registry.

Add `com.julien-dubois.bootui:bootui-spring-boot-starter` only when requested. If the application
must not contain BootUI in its production artifact, put the dependency in a development-only build
profile instead of relying only on runtime activation settings.

Maven:

```xml
<dependency>
    <groupId>com.julien-dubois.bootui</groupId>
    <artifactId>bootui-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

Gradle Kotlin DSL:

```kotlin
developmentOnly("com.julien-dubois.bootui:bootui-spring-boot-starter:2.0.0")
```

The Gradle example assumes Boot's developmentOnly configuration; verify the actual packaged artifact excludes the starter when required. The Maven example requires a development-only profile for artifact exclusion. Use the project's dependency-management convention for the version. For build-file details, follow
[spring-boot-maven](../../spring-boot-maven/SKILL.md) or
[spring-boot-gradle](../../spring-boot-gradle/SKILL.md).

## Activate for development

BootUI activates in `dev` and `local` profiles, or when Spring Boot DevTools is on the classpath.
Production profiles `prod` and `production` disable it unless explicitly forced on. Prefer explicit
profile configuration so the intended environments are easy to review:

```yaml
# application-dev.yaml
bootui:
  enabled: true
```

```yaml
# application-prod.yaml
bootui:
  enabled: false
```

Start with the development profile and open `http://localhost:8080/bootui`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

For Gradle, use `./gradlew bootRun --args='--spring.profiles.active=dev'`.
Add `.bootui/` to `.gitignore`; local overrides and dismissed findings belong to the developer's
workspace.

## Protect diagnostics

- Keep the console reachable only through loopback during local development. Do not expose
  `/bootui/**` through a public ingress or treat local-only defaults as production access control.
- Review BootUI's startup security notices when the application defines a custom
  `SecurityFilterChain`. Check route coverage and filter-chain ordering against the actual app.
- Secret masking reduces accidental exposure but does not guarantee captured data is safe to share.
  Inspect diagnostic output before sharing it.
- Use global or per-panel read-only mode when inspection-only access is required. Treat action-capable
  panels as state-changing operations.
- Expose only the Actuator endpoints required by enabled panels, and protect management endpoints
  independently.
- Keep BootUI disabled outside explicitly approved local environments, including staging and deployments with no prod profile. Verify default-off behavior with no active profile and effective overrides. If the starter remains in the artifact, verify the production
  profile actually disables it.

For security-chain design, use [spring-boot-security](../../spring-boot-security/SKILL.md). For
Actuator exposure and production telemetry, use
[spring-boot-observability](../../spring-boot-observability/SKILL.md).

## Verify

Inspect the effective dependency configuration, active profile, `.gitignore`, security chains, and
management endpoint exposure. Verify that the console loads with the development profile and is
unavailable with the production profile. Use the real security and management-port configuration.

## Official references

- [Setup](https://www.julien-dubois.com/boot-ui/setup)
- [Activation and safety](https://www.julien-dubois.com/boot-ui/setup/activation)
- [Properties](https://www.julien-dubois.com/boot-ui/properties)
- [Framework support](https://www.julien-dubois.com/boot-ui/framework-support)
