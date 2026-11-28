---
name: spring-boot-gradle
description: >
  Use when creating or changing a Spring Boot Gradle project, build.gradle or build.gradle.kts,
  Gradle Wrapper, dependency catalog, plugins, tests, or packaging. Covers Spring Boot 4 Gradle
  conventions; use spring-boot-maven for Maven and multi-module-maven for Maven reactor structure.
---

# Spring Boot Gradle

For version-sensitive changes, follow [shared version resolution](../spring-boot/references/version-resolution.md). If that sibling reference is unavailable, inspect the project's resolved dependencies and matching official documentation; report compatibility as unverified when evidence is missing.

Use this skill for Spring Boot 4 projects built with Gradle. Read
[Gradle guidance](references/gradle.md) before changing build files, dependencies, plugins,
verification tasks, or packaging. Use the committed `./gradlew` wrapper and preserve the project's
existing DSL and conventions.

For an explicit house-style alignment, apply only approved defaults in the reference.
For new projects, preserve the generation contract and add optional tooling only when requested or required by approved policy.
For other changes, check only the rules relevant to the requested build behavior.

For Maven builds, use [spring-boot-maven](../spring-boot-maven/SKILL.md). For Maven multi-module
structure, use [multi-module-maven](../multi-module-maven/SKILL.md).
