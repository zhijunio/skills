---
name: spring-boot-gradle
description: >
  Use when creating or changing a Spring Boot Gradle project, build.gradle or build.gradle.kts,
  Gradle Wrapper, dependency catalog, plugins, tests, or packaging. Covers Spring Boot 4 Gradle
  conventions; use spring-boot-maven for Maven and multi-module-maven for Maven reactor structure.
---

# Spring Boot Gradle

Use this skill for Spring Boot 4 projects built with Gradle. Read
[Gradle guidance](references/gradle.md) before changing build files, dependencies, plugins,
verification tasks, or packaging. Use the committed `./gradlew` wrapper and preserve the project's
existing DSL and conventions.

For new projects or an explicit house-style alignment, apply the full defaults in the reference.
For other changes, check only the rules relevant to the requested build behavior.

For Maven builds, use [spring-boot-maven](../spring-boot-maven/SKILL.md). For Maven multi-module
structure, use [multi-module-maven](../multi-module-maven/SKILL.md).
