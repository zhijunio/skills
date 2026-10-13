# Appendix B — Troubleshooting

When a chapter fails, capture the exact command and the relevant output. Redact credentials, tokens, personal data, and production hostnames before sharing it with the coding agent. Ask the agent to inspect the affected files, identify the cause, make a focused fix, and rerun the failing command.

## Setup issues

| Symptom | First checks |
| --- | --- |
| Environment checker reports Java missing or unsupported | Run `java --version`; check `JAVA_HOME` and the selected Boot line. The checker validates its configured compatibility range; see [version compatibility](../references/spring-boot-migration.md). |
| Environment checker reports Node/npm missing or too old | Run `node --version` and `npm --version`; use the versions required by the skill's frontend guide. Node is optional until generating or building a frontend. Reopen the terminal after changing Node versions. |
| Docker check passes but Compose cannot connect | Start Docker Desktop or the Docker daemon, then run `docker ps` and `docker compose version`. A successful `docker --version` alone does not prove the daemon is running. |
| Agent cannot find `spring-boot-developer` | Confirm the directory is in the assistant's supported skill location, restart or refresh the session if required, or provide the local `SKILL.md` path. Follow the assistant's official discovery instructions. |

## Generation / first run (Chapter 2)

| Symptom | First checks |
| --- | --- |
| Generator cannot reach Spring Initializr | Check network access, the exact generator output, and whether the selected Initializr options are supported. Preserve the output; do not retry into a non-empty target. |
| Generator refuses to create the target | Check that `--output-dir` and project name resolve to the intended empty path. The generator will not overwrite an existing project. Inspect the resolved path before removing or moving anything. |
| Build fails after generation | Run the project's wrapper and inspect the selected Java and Boot versions, dependency coordinates, and wrapper configuration. See [Maven](../references/tooling/maven.md), [Gradle](../references/tooling/gradle.md), and [version compatibility](../references/spring-boot-migration.md). |
| Port 8080 or 5432 is already in use | Identify the process or container using the port. Check Compose port mappings; do not stop an unrelated service without confirming it is safe. You can choose another configured host port if the project supports it. |
| App starts but PostgreSQL is unavailable | Check `docker compose ps`, Compose logs, datasource configuration, active profiles, and whether Spring Boot Docker Compose support is enabled. See [local development](../references/tooling/local-development.md). |
| Actuator health URL returns 404 | Confirm Actuator is on the classpath, the health endpoint is exposed for the active profile, and the management port or base path is not customized. Do not expose all management endpoints publicly to fix this. |

## Agent behavior

- **The agent changed too much:** inspect `git diff`, ask it to revert unrelated edits, and restate the narrow scope.
- **The agent added an unwanted dependency:** ask what requirement needs it and request an implementation using existing dependencies if possible.
- **The agent reports success without evidence:** request exact commands, exit status, and relevant output; run the checks yourself.
- **The agent repeats a failed fix:** provide the new failure output and ask it to compare the changed code with the original cause before editing again.
- **The agent claims a file changed but the diff is empty:** ask it to verify the working directory and run `git status --short`.
- **The agent keeps choosing the wrong framework version:** point it to the build file and ask it to follow the version actually configured, not the generator default.
- **The code differs on each attempt:** state the contract, constraints, and acceptance criteria explicitly; review one vertical slice at a time.

## Tests & build

| Symptom | First checks |
| --- | --- |
| Tables are missing | Confirm Flyway dependencies and migration locations, check migration naming and startup logs, and verify `ddl-auto` is not masking the migration strategy. See [Flyway](../references/persistence/flyway.md). |
| Flyway reports unsupported database | Check that the database-specific Flyway module is present and compatible with the selected Boot line. |
| Migration validation fails | Do not edit a migration already applied elsewhere. Add a new forward migration; only amend an unapplied local migration. |
| Testcontainers cannot start or pull PostgreSQL | Confirm Docker is running, inspect the image-pull output and registry access, and check container resource limits. See [Testcontainers](../references/testing/testcontainers.md). |
| Database test passes on H2 but fails on PostgreSQL | Use the actual PostgreSQL engine for dialect-specific queries and migrations. See [persistence slice testing](../references/testing/testing-slices-persistence.md). |
| `test` passes but integration behavior fails | Check whether integration tests run in a separate Maven Failsafe `verify` phase or a separate Gradle task. Inspect the build configuration and test naming conventions. |

## Docker & deployment

| Symptom | First checks |
| --- | --- |
| npm install fails with `ERESOLVE` or a missing lint script | Check whether the Vue scaffold normalizer ran before the first install. Follow the current [Vue guide](../references/frontend/vue.md); do not manually remove arbitrary dependencies. |
| Browser API call fails while backend works | Inspect the browser Network panel, `/api` proxy target, response status, CORS settings, and backend logs. Keep the frontend and backend contract aligned. |
| Production page is blank after packaging | Check the Vite output directory, Maven frontend plugin phase, static resource location, and browser console for asset path errors. |
| Compose configuration fails | Run `docker compose -f compose.full.yaml config -q`; inspect missing environment placeholders, indentation, file names, and service references. |
| Docker build cannot find `frontend/` | The bundled Dockerfile includes frontend build inputs. Confirm the Vue scaffold exists or follow the Docker guide for backend-only projects. |
| Docker build fails downloading Node/npm with an SSL error | Check whether a corporate proxy or VPN blocks registry access from the build container. Follow the Docker guide for build-time mirrors; keep proxy credentials out of image layers. |
| Docker image has no shell | Distroless runtime images intentionally omit a shell. Use container logs and orchestrator probes; do not expect `docker exec ... sh` to work. |
| App container starts before database readiness | Check Compose health checks and service dependencies, application retry behavior, and startup logs. Do not rely on fixed sleeps as a readiness check. |
| A JSON request returns 400 for a numeric field | Check the request payload type and validation constraints. Send `quantity` as an integer and `price` as a decimal number; do not use `null` or a string unless the API explicitly allows it. |

## JDTLS / code intelligence

If Java navigation or diagnostics stop working, check `.github/lsp.json`, whether your agent supports LSP, and whether `jdtls` is available on `PATH`. Run `jdtls --help`, then compile with the project wrapper so dependencies are available. Restart the agent's Java language server after changing the build file. See [JDTLS](../references/tooling/jdtls.md).

## Still stuck?

Record the chapter, command, redacted output, expected behavior, and files changed since the last checkpoint. Ask the agent for a read-only diagnosis before requesting another implementation attempt. For version-sensitive questions, consult the focused reference and official documentation linked at its end.
