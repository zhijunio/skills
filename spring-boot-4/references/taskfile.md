# Taskfile

Read this when the task adds `Taskfile.yml`, Task, or `go-task`.
The skeleton script does not add a Taskfile. Define commands through the
project's existing build tool and wrapper. For Maven-specific conventions, see
[maven.md](maven.md).

## When to use

- Add or change `Taskfile.yml`
- Expose `format`, `verify`, or `run` as Task tasks

## When not to use

- Do not replace the project's build-tool wrapper; Task should call it.

## Rules

1. Put `Taskfile.yml` at the project root. Use Taskfile version `3`.
2. Point build tasks at the committed wrapper for the selected build tool.
3. Keep plugin configuration in the build file; the Taskfile only wraps it.
4. Expose `format`, `verify`, and `run` tasks that match existing project
   commands. Add `compose-up` only when `compose.yaml` exists.

Maven example using the committed wrapper:

```yaml
version: "3"

vars:
  MVNW: '{{if eq OS "windows"}}mvnw.cmd{{else}}./mvnw{{end}}'

tasks:
  default:
    cmds:
      - task: verify
  format:
    desc: Apply Spotless
    cmds:
      - "{{.MVNW}} -q -B spotless:apply"
  verify:
    desc: Run the Maven verify lifecycle
    cmds:
      - "{{.MVNW}} -q -B verify"
  run:
    desc: Run the Spring Boot application
    cmds:
      - "{{.MVNW}} spring-boot:run"
```

With Compose:

```yaml
  compose-up:
    desc: Start local Compose services
    cmds:
      - docker compose up -d
```

## Verify

For a new Taskfile, check every rule. For an existing Taskfile, check rules
relevant to the requested task change. Open `Taskfile.yml` and the active
build configuration; do not infer. Report applicable failures.

The template's runnable Taskfile is
[`assets/template-maven/Taskfile.yml`](../assets/template-maven/Taskfile.yml).
