---
name: mise
description: >
  Add or change mise configuration for a Spring Boot project, including pinned tool versions and
  shared developer tasks. Use mise tasks instead of introducing Taskfile/go-task for new workflows.
---

# mise

Use mise to pin project tools and expose shared commands through `mise.toml`. Keep Maven or Gradle
as the source of truth for build behavior; mise tasks should call the committed wrapper and compose
commands the build already supports.

Read [mise guidance](references/mise.md) when adding or changing `mise.toml`, tool versions, or
project tasks.

For Maven conventions, use [Spring Boot Maven](../spring-boot-maven/SKILL.md). For Gradle, use
[Spring Boot Gradle](../spring-boot-gradle/SKILL.md). For Compose services, use
[Spring Boot Docker](../spring-boot-docker/SKILL.md).
