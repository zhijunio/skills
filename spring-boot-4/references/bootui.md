# BootUI

Read this when adding BootUI, the embedded local developer console. BootUI is
an optional diagnostics UI for local development. It does not replace
Actuator, production monitoring, or APM.

## Compatibility

BootUI 2.x targets Spring Boot 4.x and Java 17+. It does not support Boot
3.x. The pinned version is `bootui.default` in `versions.json`; verify its
compatibility against the official setup guide before changing it. The
application's existing Spring MVC or WebFlux starter selects the web stack;
do not add a second web stack.

## When to use

- Inspect a running application during local development
- Add the BootUI starter to an existing Spring Boot 4 application

## When not to use

- Production monitoring, alerting, or distributed tracing →
  [observability.md](observability.md)
- Actuator endpoint policy → [observability.md](observability.md)
- Boot 3 applications

## Rules

1. Add BootUI only when requested. Keep its version in `versions.json`, expose
   it using the project's build-tool version-management convention, and add
   `bootui-spring-boot-starter`. Maven projects may use a property and
   `dependencyManagement`.
2. Keep the starter on the runtime classpath only when the developer chooses
   to include the UI. Enable it explicitly in `application-dev.yaml` or
   `application-local.yaml`; set `bootui.enabled: false` in
   `application-prod.yaml`. Never enable it in production.
3. Add `.bootui/` to the project's `.gitignore`. Local overrides and dismissed
   findings belong to the developer's workspace.
4. Keep `/bootui` reachable only from local development. Do not expose
   `/bootui/**` through a public ingress or treat local-only defaults as
   production access control.
5. Review BootUI's startup security notices when the app has a custom
   `SecurityFilterChain`. It can register permitted paths needed for its UI.
   Secret masking is not a guarantee that all captured data is safe to share;
   inspect diagnostics before sharing them.
6. BootUI panels may perform mutating developer actions. Enable read-only
   mode globally or per panel when inspection-only access is required. If a
   panel uses Actuator, expose only its required endpoints and keep the
   management interface local and protected.

## Maven example

Use the equivalent dependency declaration for other build tools.

```xml
<properties>
	<bootui.version>COPY_FROM_VERSIONS_JSON</bootui.version>
</properties>

<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>com.julien-dubois.bootui</groupId>
			<artifactId>bootui-spring-boot-starter</artifactId>
			<version>${bootui.version}</version>
		</dependency>
	</dependencies>
</dependencyManagement>

<dependency>
	<groupId>com.julien-dubois.bootui</groupId>
	<artifactId>bootui-spring-boot-starter</artifactId>
</dependency>
```

For explicit profile configuration:

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

Activate the `dev` profile when starting the application. With the default
port, open `http://localhost:8080/bootui`. BootUI can also auto-activate for
`dev` or `local` profiles and when DevTools is present; keep the explicit
profile settings so the production behavior is clear.

## Verify

Open the active build configuration, `versions.json`, profile configuration, `.gitignore`, and
security configuration. With `dev` active, confirm `/bootui` loads and the
expected panels report availability. With `prod` active, confirm the UI is
unavailable. Verify behavior with the application's real security and
management-port configuration.

## Official references

- [BootUI setup](https://www.julien-dubois.com/boot-ui/setup)
- [Activation and safety](https://www.julien-dubois.com/boot-ui/setup/activation)
- [Properties](https://www.julien-dubois.com/boot-ui/properties)
- [Framework support](https://www.julien-dubois.com/boot-ui/framework-support)
