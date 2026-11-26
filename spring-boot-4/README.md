# Spring Boot 4

Agent skill for creating and changing Java Spring Boot 4 applications. JSON is
Jackson 3. Follow [SKILL.md](SKILL.md) Workflow. Pins are in
[versions.json](versions.json). The Maven quick start uses
`springBoot.maven`; other build tools use `springBoot.default`.

## Quick start

Run the skeleton script from this directory. Set `--output-dir` to a workspace
outside the skill tree. It requires Node.js 18+, `unzip`, and network access to
Spring Initializr:

```bash
node scripts/create-project.mjs \
  --name inventory-api \
  --group-id com.example \
  --output-dir /absolute/path/to/workspace
```

The script creates Maven projects by default. Add `--build gradle` to create a
Gradle project. Both request `web`, `validation`, and `actuator` from Spring
Initializr. The Boot version comes from `versions.json` unless `--boot-version`
is supplied. The script creates only the skeleton; it does not apply house
defaults. Follow [SKILL.md](SKILL.md)
**Create** and its build reference to add architecture checks and OpenAPI for a
web application unless declined. Add optional stacks such as MyBatis-Plus,
Flyway, PostgreSQL, Compose, or Taskfile only when the project needs them.
For Spring Security filter chains, OAuth2/OIDC clients, and resource-server
APIs, start from the matching security routes in [SKILL.md](SKILL.md).
For a React SPA, read [React](references/react.md) and
[Frontend integration](references/frontend-integration.md). For a Next.js
frontend, read [Next.js](references/nextjs.md) and the shared integration
reference.
For runnable examples of HTTP Basic, OIDC login, JWT resource-server
authorization, and Spring Security MFA, see the optional
[template-security](assets/template-security/README.md) project. It is separate
from the base template so authentication is not enabled unless requested.

## Runnable templates

These projects are executable reference implementations for the rules in
`references/`. They are not alternatives to the project generator: generate a
new project for the requested name and package, then use only the matching
template sections and references. Do not copy a template's identity, package,
or unrelated features into a new project.

| Template | Use it to study | Matching references |
| --- | --- | --- |
| [template-maven](assets/template-maven/README.md) | Unauthenticated MVC application with PostgreSQL, MyBatis-Plus, Flyway, Redis, OpenAPI, events, email, async context propagation, and observability | [architecture](references/architecture.md), [MyBatis-Plus](references/mybatis-plus.md), [testing](references/testing.md), [async](references/async.md), [email](references/email.md), [observability](references/observability.md) |
| [template-security](assets/template-security/README.md) | Optional HTTP Basic, OIDC login, JWT resource server, and Spring Security MFA using filter chains | [security](references/security.md), [OAuth2 Client](references/oauth2-client.md), [OAuth2 Resource Server](references/oauth2-resource-server.md), [MFA](references/mfa.md) |

Each linked reference should describe the same implementation shown in its
template, and template changes should be checked against those references.
Keep security optional: select `template-security` examples only when the
application requires an authentication flow.

After changing a template or its references, run the structural consistency
check:

```bash
node scripts/check-template-links.mjs
```

The check confirms that every matching reference listed for a template links to
an existing file in that template. It also checks local Markdown targets, each
Maven template's Boot/Java pins, and managed Compose image tags against
`versions.json`. It does not compare behavior or Java examples; review those
against the executable template and run its build when changing either side.

## Verify

[SKILL.md](SKILL.md) Workflow last step: run the project's relevant test task
once and report any unavailable dependencies. Maven commands and conventions
live in [references/maven.md](references/maven.md).
