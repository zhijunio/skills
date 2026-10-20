# mise

[mise](https://mise.jdx.dev/) can select the project JDK and provide a discoverable set of development commands from one committed `mise.toml`. Keep tasks thin: delegate builds and formatting to the Maven or Gradle Wrapper, which remains the source of truth for build-tool versions.

## Project configuration

The generator writes the selected Temurin Java major under `[tools]` and wrapper-backed tasks under `[tasks.*]`. Developers install mise using its [official installation guide](https://mise.jdx.dev/getting-started.html), then run `mise install` to install configured tools and `mise tasks` to list commands. `mise run verify` invokes the project wrapper. Direct wrapper commands remain available, and CI should continue to select its JDK explicitly.

Example:

```toml
[tools]
java = "temurin-25"

[tasks.verify]
description = "Run verification and package checks"
run = "./mvnw verify"
run_windows = "mvnw.cmd verify"
```

Use `run_windows` for Windows command differences. Keep infrastructure lifecycle tasks explicit; stopping Compose services should preserve data, and volume deletion should have a separate clearly named task and guard. Never put credentials in committed mise configuration. mise configuration may execute tasks, so review it as executable project configuration and trust the repository when prompted.

## Verify the configuration

Run `mise tasks` to confirm tasks are discovered, `mise ls --current` to inspect the selected JDK, and an appropriate wrapper-backed task when changing a task definition. The Maven or Gradle Wrapper remains usable without mise; mise is the documented local convenience for consistent JDK selection and task discovery.

## References

- [mise configuration](https://mise.jdx.dev/configuration.html)
- [mise TOML tasks](https://mise.jdx.dev/tasks/toml-tasks.html)
- [mise installation](https://mise.jdx.dev/getting-started.html)
