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

The script creates a minimal Maven skeleton with `web`, `validation`, and
`actuator`, then pins the Maven parent. It does not apply the skill's house
defaults. Follow [SKILL.md](SKILL.md) **Create** to add architecture checks,
Maven conventions, and OpenAPI for a web application unless declined. Add
optional stacks such as MyBatis-Plus, Flyway, PostgreSQL, Compose, or Taskfile
only when the project needs them.

## Verify

[SKILL.md](SKILL.md) Workflow last step: run the project's relevant test task
once and report any unavailable dependencies. Maven commands and conventions
live in [references/maven.md](references/maven.md).
