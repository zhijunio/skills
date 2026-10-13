# Taskfile

[Task](https://taskfile.dev/) is a cross-platform task runner that gives a Spring Boot repository a discoverable set of named development commands. Use it when a project benefits from a small command interface; a `Makefile`, Gradle tasks, Maven profiles, or scripts are also valid choices. Keep one source of truth for each command and avoid wrapping every build-tool goal.

## Installation

Developers need the Task CLI in addition to the project's JDK and build tool. Install it using the method documented by [Task](https://taskfile.dev/installation/). `task --list` should work from the repository root.

## Design rules

- Put the root `Taskfile.yml` beside the build file and give public tasks a concise `desc`, so `task --list` documents the interface.
- Keep tasks thin: delegate compilation, testing, packaging, and formatting to Maven or Gradle. Don't duplicate build logic in shell commands.
- Use the project's wrapper (`./mvnw` or `./gradlew`) so the build-tool version is reproducible. Account for `mvnw.cmd` / `gradlew.bat` if Windows is supported.
- Give one task one clear purpose. Make `default` list available tasks or run a safe, expected action.
- Use task dependencies for setup that must precede a command. Task executes dependent tasks before the task body; independent dependencies may run concurrently, so don't use them for ordered or shared mutable work.
- Keep local infrastructure lifecycle explicit. `compose:up` may start dependencies; `compose:stop` should stop them without deleting volumes. Make data deletion a separate, clearly named task with a confirmation or other guard.
- Don't place credentials in Taskfile variables or committed dotenv files. Let tools read secrets from the environment or an ignored local file.
- Prefer Task's platform-aware command support over assuming POSIX utilities such as `sleep`, `sed`, or `rm` exist on every developer machine.
- Keep Task optional. CI and production deployment should be able to invoke the build tool directly unless Task is an intentional repository prerequisite.

## Maven example

```yaml
version: '3'

vars:
  MVNW: '{{if eq OS "windows"}}mvnw.cmd{{else}}./mvnw{{end}}'

tasks:
  default:
    desc: List available tasks
    cmds:
      - task --list

  test:
    desc: Run the test suite
    cmds:
      - '{{.MVNW}} test'

  verify:
    desc: Run verification and package checks
    cmds:
      - '{{.MVNW}} verify'

  run:
    desc: Run the application with the local profile
    cmds:
      - '{{.MVNW}} spring-boot:run -Dspring-boot.run.profiles=local'

  compose:up:
    desc: Start local dependencies
    cmds:
      - docker compose up -d

  compose:stop:
    desc: Stop local dependencies and keep their data
    cmds:
      - docker compose stop
```

For Gradle, replace the wrapper variable with `{{if eq OS "windows"}}gradlew.bat{{else}}./gradlew{{end}}` and use the repository's tasks, such as `test`, `check`, and `bootRun`. Adapt the local profile argument to the project's configuration. If Spring Boot Docker Compose support is enabled, explain that it may start services automatically and keep the explicit Compose task as a manual fallback ([local-development.md](local-development.md)).

## Verify the Taskfile

Run `task --list` to check that tasks load and are discoverable. Run the relevant task (for example, `task verify`) when adding or changing a Taskfile. Check the Taskfile schema version against the minimum Task CLI version required by any features it uses; schema version is not the Task CLI release number.

## Gotchas

- Agent assumes Task is installed - document the prerequisite and preserve direct wrapper commands.
- Agent wraps `clean`, formatting, tests, and packaging into every task - keep named tasks composable and avoid surprising side effects.
- Agent deletes Compose volumes as part of ordinary cleanup - preserve developer data by default; make destructive cleanup explicit.
- Agent relies on task dependencies for sequencing - dependencies can run in parallel, so express ordered work as task calls in `cmds`.
- Agent treats Task variables as process environment variables - use `env:` when a command needs an exported environment value.
- Agent puts secrets in a committed `.env` or Taskfile - read them from the environment or ignored local configuration.

## References

- [Task](https://taskfile.dev/)
- [Task](https://taskfile.dev/installation/)
