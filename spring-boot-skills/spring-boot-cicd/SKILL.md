---
name: spring-boot-cicd
description: >
  Use when the user requests GitHub Actions, CI verification, Renovate dependency updates,
  Git tags, or GitHub release workflows for a Spring Boot project. Keeps workflows opt-in,
  least-privilege, and aligned with the project's build wrapper.
---

# Spring Boot CI/CD

Read [CI/CD guidance](references/cicd.md) when adding or changing GitHub Actions, Renovate, or
GitHub release automation. Do not add workflows, publish jobs, deployment jobs, or release triggers
unless requested.

Use the project's committed wrapper and existing verification task. Coordinate with
[Spring Boot Maven](../spring-boot-maven/SKILL.md) or
[Spring Boot Gradle](../spring-boot-gradle/SKILL.md) for build commands,
[Spring Boot testing](../spring-boot-testing/SKILL.md) for service-backed tests.
