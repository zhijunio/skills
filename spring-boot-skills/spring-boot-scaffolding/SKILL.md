---
name: spring-boot-scaffolding
description: >
  Use when creating a new Spring Boot 4 application from Spring Initializr. Select a version allowed by this repository's versions.json, preserve the requested build tool and capabilities, apply project defaults, and verify the result. Not for changing or migrating an existing application.
---

# Spring Boot 4 Scaffolding

Create a runnable application for the requested use case. Use the repository's
version policy and house defaults, while honoring explicit requirements and
exclusions.

## Establish the generation contract

Confirm that the destination is new. Establish the project name, coordinates,
Java level, Maven or Gradle, web stack, database, and requested capabilities.
Ask only for missing choices that materially change the generated project.
Never overwrite an existing project.

Read [the generation procedure](references/generation.md) before using Spring
Initializr. Use its official UI or metadata/generation endpoints. This skill
does not require or invoke a project-generation script. Do not send credentials
or private source code.

## Version policy

Read the shared [versions.json](../versions.json). Use
`springBoot.defaultByBuildTool.maven` for Maven and
`springBoot.defaultByBuildTool.gradle` for Gradle unless the user selects
another version on a line listed in `springBoot.allowedLines`. Check that the
exact stable version and Java option are currently advertised by Initializr.

If the requested version is outside the allowed lines or is unavailable, stop
and report the mismatch. Do not silently substitute a release. Do not select an
RC, milestone, or snapshot. Check the JVM that will run the selected wrapper
before building.

## Apply new-project defaults

Use Java 21, Spring MVC, and Maven when the user has not selected a Java level,
web stack, or build tool. For the default persistence stack, use PostgreSQL,
MyBatis-Plus, Flyway, and Redis. Include ArchUnit and Taikai for architecture
tests. Enable OpenAPI for web applications, use JavaDoc descriptions, and keep
API documentation disabled in production. Spring Modulith is opt-in.

Add Security, JWT issuance, AI providers, and external telemetry only when
requested. Preserve explicit choices over defaults. Do not add the new-project
defaults to an existing project.

If no business feature was requested, keep the generated application and its
context-load test; do not invent an Order, User, or payment domain. For a
requested feature, implement one complete slice with success and failure
coverage. Adapt [AGENTS.md](templates/AGENTS.md) or
[CLAUDE.md](templates/CLAUDE.md) only when project-local guidance is useful;
keep commands consistent with the generated build.

## Verify and report

Inspect the generated build and configuration against the selected Boot/Java
versions, requested capabilities, defaults, and exclusions. Run the selected
wrapper's configured verification lifecycle. When feasible, start the app on
loopback using an unused port and verify an actual readiness signal or requested
endpoint.

Report exact Boot, Java, and build-tool versions; commands and results; executed
test counts; and checks that could not run. A successful download or compile
alone does not complete scaffolding.

[Good plan](examples/good-plan.md) and [bad plan](examples/bad-plan.md)
illustrate scope. This workflow does not bundle a generation script. If
Initializr is unavailable or the requested version is not advertised, report
that blocker rather than using a stale version or unapproved source.

## Official sources

- [Spring Initializr metadata and generation](https://docs.spring.io/initializr/docs/current/reference/html/)
- [Spring Boot build systems](https://docs.spring.io/spring-boot/reference/using/build-systems.html)
- [Spring Boot testing](https://docs.spring.io/spring-boot/reference/testing/index.html)

## Gotchas

- Destination already contains a project: choose a new output directory; do not merge or overwrite it.
- Initializr no longer advertises the pinned Boot release: stop and report the unavailable version.
- User selected Gradle or WebFlux: preserve that choice instead of changing it to Maven or MVC.
- User declined Security: keep authentication dependencies and configuration out of the generated project.
- Project generation succeeded but tests or startup were not checked: report those checks as not run.
