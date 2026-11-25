# Spring Boot Configuration

Read this when adding or changing Spring Boot application properties,
profiles, or `@ConfigurationProperties`. Provider-specific settings remain in
their stack references, which should link here for shared configuration
rules.

## When to use

- Add or change `application.yaml` or a profile-specific configuration file
- Bind application settings with `@ConfigurationProperties`
- Externalize deployment values or manage configuration secrets

## When not to use

- Build-file dependency or plugin configuration → follow the selected build
  tool; Maven-specific rules are in [maven.md](maven.md)
- A stack's specific property values → its matching reference, such as
  [email.md](email.md), [cache.md](cache.md), or [docker.md](docker.md)

## Rules

1. Use `src/main/resources/application.yaml` for shared defaults. Use
   `application-{profile}.yaml` only for intentional environment differences.
   Do not duplicate the full shared configuration in every profile.
2. Keep environment-specific values outside the packaged artifact. Resolve
   deployment values through environment variables, command-line arguments,
   mounted configuration, or the deployment platform. Do not commit
   credentials, private keys, tokens, or production endpoints.
3. Bind groups of application-owned settings with immutable
   `@ConfigurationProperties` records. Give each group a stable prefix and a
   clear owner. Use `@Validated` and Jakarta Bean Validation constraints for
   required values, ranges, and formats; fail startup with a clear message for
   invalid configuration.
4. Use Boot's relaxed binding and canonical kebab-case property names in
   YAML. Prefer typed values such as `Duration`, `DataSize`, `URI`, and enums
   over strings that callers must parse. Do not read application settings
   directly from `System.getenv()` or scatter `@Value` fields through
   business code.
5. Keep defaults safe for local development and explicit in the owning
   configuration type or YAML. Do not use a production secret or production
   endpoint as a default. Make required deployment settings fail fast when
   absent.
6. Use Spring profiles for broad environment-specific behavior, not for
   feature flags or arbitrary combinations of deployment options. Keep
   profile activation in the deployment environment; do not activate the
   production profile from packaged defaults.
7. Avoid duplicate keys and implicit YAML type coercion. Quote values when
   YAML could interpret them as booleans, numbers, or timestamps. Group
   related settings under their Spring Boot namespace.
8. Test configuration binding and validation at the boundary. Use a focused
   binding test for custom `@ConfigurationProperties`; keep ordinary unit
   tests independent of the Spring context. Verify active profile and
   externalized values in the target deployment configuration.

## Local `.env` files

Use `.env.example` only as a documented local-development template. Never put
real credentials or secrets in it. Add `.env` and other local secret files to
`.gitignore`, while explicitly allowing `.env.example`:

```gitignore
.env
.env.*
!.env.example
```

Docker Compose reads `.env` for Compose-file variable substitution when it is
in the Compose project directory; it can also read an explicit file through
`docker compose --env-file`. Spring Boot does not automatically load a generic
`.env` file. When launching the application directly, export the values into
the process environment, configure them in the IDE, or use the deployment
platform's environment injection. Do not add a dotenv library or custom file
loader unless the task specifically requires one. Keep the README's startup
commands consistent with how the process receives those values. If Task is
used, its native `dotenv` setting can load the documented `.env` for wrapped
commands without adding an application dependency; the template demonstrates
this in [`Taskfile.yml`](../assets/template-maven/Taskfile.yml) and
[`.env.example`](../assets/template-maven/.env.example).

```sh
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

Example typed settings:

```java
@ConfigurationProperties("app.mail")
@Validated
record MailProperties(
	@NotBlank String from,
	@NotNull Duration deliveryTimeout
) {}
```

Register the properties type with `@ConfigurationPropertiesScan` on the
application class or `@EnableConfigurationProperties(MailProperties.class)`
in its configuration. Stack-specific examples should use the same binding
and externalization rules.

## Verify

Open the YAML files and configuration property types affected by the change.
Confirm changed properties have one clear owner, secrets and deployment-only
values are externalized, profiles contain only intentional differences, and
invalid values fail startup. Report settings whose effective value depends on
deployment configuration unavailable in the repository.
