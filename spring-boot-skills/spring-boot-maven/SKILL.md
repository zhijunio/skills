---
name: spring-boot-maven
description: >
  Use when creating or changing a Spring Boot Maven project, pom.xml, Maven Wrapper,
  dependency or plugin versions, Surefire, Spotless, JaCoCo, Enforcer, or Maven Release.
  Covers Spring Boot 4 single-module builds, build-info and Buildpacks image settings, optional
  Git revision metadata, and release safety; use multi-module-maven for reactor structure.
---

# Spring Boot Maven

Use this skill for Spring Boot 4 projects built with Maven. Read
[Maven guidance](references/maven.md) before changing `pom.xml`, the wrapper, build plugins,
coverage, or release configuration. Use the project's committed `./mvnw` and preserve its current
Boot line unless the requested change requires an upgrade.

For new projects or an explicit house-style alignment, apply the complete Maven defaults in the
reference. For other changes, check only the relevant rules and avoid adding those defaults to an
existing project unless requested.

This skill covers the Maven build for a single runnable application. For parent/child POMs,
reactor ordering, or module dependency direction, use
[multi-module-maven](../multi-module-maven/SKILL.md). For application test design and readiness,
use [spring-boot-testing](../spring-boot-testing/SKILL.md).
