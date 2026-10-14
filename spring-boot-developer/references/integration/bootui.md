# BootUI for Spring Boot

BootUI is an optional, embedded developer console for inspecting a running application during local development. It is not a production monitoring or APM system, and it does not replace Actuator. The default UI path is `/bootui`.

## Compatibility

BootUI 2.x targets Spring Boot 4.x and Java 17+. It does not support Spring Boot 3.x. The example below uses BootUI `2.0.0`, the version shown in the official setup guide checked on 2026-10-14. Re-check the official setup guide and compatibility before upgrading or adding it; do not assume that the application's Boot version can use the latest BootUI release.

Use the `bootui-spring-boot-starter` with either Spring MVC or WebFlux; the application's existing web starter selects the web stack. Do not add a second web stack for BootUI. Panels vary by runtime and available integrations, so consult its framework support matrix for current coverage.

## Add the starter

Keep the BootUI version in a Maven property and declare the starter alongside the application's other dependencies. The example uses `2.0.0`, the version shown in the official setup guide checked on 2026-10-14. Check the current release before copying a version.

```xml
<properties>
    <bootui.version>2.0.0</bootui.version>
</properties>

<dependencies>
    <!-- Optional panels use Actuator data when this starter is present. -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>com.julien-dubois.bootui</groupId>
        <artifactId>bootui-spring-boot-starter</artifactId>
        <version>${bootui.version}</version>
    </dependency>
</dependencies>
```

For an explicit YAML setup, add this optional development profile file:

```yaml
# src/main/resources/application-dev.yml
bootui:
  enabled: true
```

Activate the `dev` profile when starting the app, then open `http://localhost:8080/bootui` (replace `8080` if the app uses another port). For example:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

BootUI's `AUTO` mode also activates for `dev` or `local` profiles and when DevTools is on the classpath; the explicit YAML setting makes the activation decision visible in configuration.

The direct dependency leaves BootUI on the application's runtime classpath in all builds. Disable it explicitly in the production configuration:

```yaml
# src/main/resources/application-prod.yml
bootui:
  enabled: false
```

BootUI's default `AUTO` activation is limited to the `dev` or `local` active profile, or an application with DevTools. It disables itself for `prod` and `production` profiles unless explicitly forced on. The explicit `false` value makes the production intent clear and takes precedence over auto-activation. Never set `bootui.enabled: true` in production. Add `.bootui/` to `.gitignore`; local overrides and dismissed findings belong to the developer's workspace.

BootUI accepts YAML booleans for this setting: `true` means `ON`, and `false` means `OFF`.

## Security and operations

- Keep BootUI reachable only from local development. Do not expose `/bootui/**` through a public ingress or treat the default local-only policy as a production access-control system.
- Do not enable it in production to work around a missing local profile. Explicitly activate `dev` when launching a packaged development jar; setting it only as the default profile does not activate it for BootUI.
- BootUI may register permitted paths in Spring Security so the local UI can load. Review the startup warning and application security configuration if the project has custom filter chains.
- BootUI masks secret-like configuration values, but do not treat that as a guarantee that all application data is safe to expose. Review captured diagnostics before sharing them.
- BootUI can perform mutating developer actions in supported panels. Use its global or per-panel read-only settings when a local environment should allow inspection only.
- If BootUI uses Actuator endpoints, expose only the endpoints needed by the selected panels and keep the management boundary local.

## Verify

With the `dev` or `local` profile active, confirm that `/bootui` loads and the expected panels report their availability. Start with the production profile active and confirm the explicit `bootui.enabled: false` setting makes `/bootui` unavailable. Test with the application's actual Spring Security and management-port configuration.

## References

- [BootUI setup](https://www.julien-dubois.com/boot-ui/setup)
- [BootUI activation and safety](https://www.julien-dubois.com/boot-ui/setup/activation)
- [BootUI Spring WebFlux setup](https://www.julien-dubois.com/boot-ui/setup/webflux)
- [BootUI properties](https://www.julien-dubois.com/boot-ui/properties)
- [BootUI framework support](https://www.julien-dubois.com/boot-ui/framework-support)
