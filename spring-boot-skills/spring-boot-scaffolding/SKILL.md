---
name: spring-boot-scaffolding
description: >
  Generate a new Spring Boot 4 skeleton from Spring Initializr with the requested build tool, Java version, and starter dependencies.
---

# Spring Boot 4 Scaffolding

Create only the new project the user requested. Do not add a fixed stack, business
features, CI, or other project files by habit. Target Boot 4; never silently switch
major versions because a template is easier to obtain.

## Establish the generation contract

Confirm a new destination, project coordinates/package, Java level, Maven or Gradle, and
the capabilities the user requested. Ask only for missing consequential choices. Preserve
the requested language, web stack, database and dependencies. Do not add unrequested
dependencies, application code, CI, or deployment files.

Read [the generation procedure](references/generation.md) before fetching or extracting an
archive. Use the official Spring Initializr endpoint; do not send credentials or private
source code.

## Fast path

When the destination, coordinates, build tool, Java version, Boot release and requested
capabilities are clear, proceed without clarification or extra research. If the user supplied
an exact Boot release, request that version directly; fetch metadata only when the version
must be selected or a requested Initializr capability is unclear. For a straightforward
request, batch generation and the small output checks into one tool call. Do not inspect
unrelated references or repeat checks already established by the Initializr response.

## Version-specific checks

Select an advertised stable Boot 4 release and compatible Java/build-tool options. Use the
published release version for generation and build coordinates. Initializr metadata may expose
an identifier such as `4.2.0.RELEASE` while displaying the published version as `4.2.0`;
use the displayed version as the generation parameter and confirm the generated Maven parent
or Gradle plugin uses that published coordinate. Do not assume `.RELEASE` is a valid artifact
version. If the generated coordinate is unavailable from its repository, stop and report it.

Never select an RC, milestone or snapshot unless explicitly requested. When selecting from
metadata, distinguish its option id from the published release version; do not assume stable
releases end with `.RELEASE`.
Check the effective build, including the JVM actually running the wrapper, before compiling.

## Generate the requested project

Keep the generated wrapper, dependency management and source layout. Retain Initializr's
application class, default test and generated files. Do not implement features or add
configuration, documentation, tests, CI, or local services unless the user asks for them.

## Finish and report

Check that the destination contains the generated project and that its selected build file
uses the requested, published Spring Boot version. Do not run a build, tests or local startup
unless the user requests verification. Report the destination, selected versions and any
requested capability that Initializr could not provide.

[Good plan](examples/requested-scope-plan.md) and [bad plan](examples/scope-overreach.md) illustrate scope.
This workflow is guidance, not a bundled generator or a guarantee that remote generation
will remain available.

## Official sources

- [Initializr metadata and generation](https://docs.spring.io/initializr/docs/current/reference/html/)
- [Boot build systems](https://docs.spring.io/spring-boot/reference/using/build-systems.html)

## Gotchas

- Agent extracts into an existing checkout - use a new staging directory and refuse destination collisions.
- Agent hard-codes a stale Initializr version - inspect advertised capabilities and report unavailable versions.
- Agent generates both build tools or both web stacks - preserve the user's selected architecture.
- Agent passes an Initializr metadata alias as a Maven/Gradle version - use the published release coordinate and check the generated build file.
