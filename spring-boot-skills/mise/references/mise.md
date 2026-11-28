# mise

Use this reference when adding `mise.toml` or project tasks. mise manages tool versions and runs
named project commands; Maven or Gradle remains responsible for compilation, tests, plugins, and
packaging.

## Configure project tasks

Put shared, short tasks in the project-root `mise.toml`. Call the committed wrapper for the selected
build tool and expose only commands that the project already supports.

Maven example:

```toml
[tasks.format]
description = "Apply Spotless formatting"
run = "./mvnw -q -B spotless:apply"

[tasks.verify]
description = "Run the Maven verify lifecycle"
run = "./mvnw -q -B verify"

[tasks.run]
description = "Run the Spring Boot application"
run = "./mvnw spring-boot:run"
```

For Gradle, wrap the committed `./gradlew` and configured tasks. Add a Compose task only when the
repository has a Compose file:

```toml
[tasks.compose-up]
description = "Start local Compose services"
run = "docker compose up -d"
```

Keep one or two command tasks in TOML. Move longer scripts to file tasks and review them as code.
Avoid duplicating lifecycle logic that already belongs in the build file.

## Pin tools deliberately

- Add `[tools]` entries only for tools the project needs, such as Java or Node.js. Use the version
  already required by the Spring Boot line, Maven/Gradle toolchain, frontend, and CI configuration.
- Check that local and CI environments read the same committed `mise.toml` and install compatible
  tool versions.
- If the project enables mise's lockfile, commit the generated `mise.lock` with the tool-version
  configuration so teammates and CI resolve the same versions.
- Use `[env]` for non-secret project constants such as a Compose file path or image name. Do not put
  credentials in it.
- Leave secrets out of `mise.toml`; use the project's established secret store and environment
  injection.
- Review `mise.toml`, inherited configuration, plugins and referenced scripts before trusting it. Do not automatically run mise trust/install for an unreviewed checkout; obtain approval for tool installation or state-changing tasks. Tasks and configuration can run commands or change the
  environment when mise loads them.

## Compose tasks

When Compose is already part of the project, mise tasks can expose its lifecycle next to build
commands. Define `start`, `stop`, and `restart` only when the underlying Compose operations exist;
use task dependencies for real ordering, such as building an image before starting the application.
Keep database or other service startup commands scoped to the project's documented profile.

The [HelpDesk `mise.toml`](https://github.com/sivaprasadreddy/HelpDesk/blob/main/mise.toml) is a
working example of pinned Java and Maven tools, a committed lockfile, project environment values,
build tasks, and a Compose lifecycle. Reuse its structure as a reference, but adapt tool versions,
profiles, image names, and task dependencies to the target project.

## Verify

Inspect `mise.toml`, the build configuration, and every referenced script. Confirm task names run
the intended committed wrapper and that optional services exist before adding related tasks. Use
`mise tasks ls` to review the public task list and `mise run --dry-run <task>` to inspect an
execution plan. Run either the mise task or its underlying build command for a given verification,
not both.

For the underlying build conventions, see [Maven](../../spring-boot-maven/SKILL.md) or
[Gradle](../../spring-boot-gradle/SKILL.md). For Compose configuration, see
[Docker](../../spring-boot-docker/SKILL.md).

## Official references

- [mise tasks](https://mise.jdx.dev/tasks/)
- [TOML tasks](https://mise.jdx.dev/tasks/toml-tasks.html)
- [mise configuration](https://mise.jdx.dev/configuration/)
