# Resolve framework and library versions

These skills target Spring Boot 4, not a single permanently fixed minor or patch.
The application's resolved dependency graph is authoritative for implementation;
examples and this collection's packaging POM are not the application's baseline.

## Establish evidence before using version-sensitive APIs

1. Inspect the application wrapper, Java toolchain, parent/BOM, Gradle plugins,
   version catalogs, dependency constraints and explicit overrides.
2. Resolve dependencies in the affected module and relevant compile, runtime or
   test configuration. For Maven use the committed wrapper with
   `help:effective-pom` and `dependency:tree`; for Gradle use `dependencies`
   and `dependencyInsight --dependency <module> --configuration <configuration>`
   with the committed wrapper. Preserve existing profiles and build options.
3. Identify the actual versions of Boot and the relevant libraries, including
   Framework, Spring Data, Hibernate, Jackson and Testcontainers as applicable.
   Distinguish requested versions from resolved versions and server versions
   from client library versions.
4. Consult official documentation or source for those versions, then check
   imports, signatures, artifact coordinates, configuration properties and
   behavioral assumptions. Compile and run focused tests before claiming compatibility.

## Official starting points

These links are discovery entry points, not a pinned compatibility matrix.
Select the documentation matching the resolved release, not simply the newest page.

| Component | Source |
| --- | --- |
| Spring Boot dependency management and migration | https://docs.spring.io/spring-boot/ |
| Spring Framework | https://docs.spring.io/spring-framework/reference/ |
| Spring Data JPA | https://docs.spring.io/spring-data/jpa/reference/ |
| Spring Data Redis | https://docs.spring.io/spring-data/redis/reference/ |
| Hibernate ORM | https://hibernate.org/orm/documentation/ |
| Jackson | https://github.com/FasterXML/jackson |
| Testcontainers for Java | https://java.testcontainers.org/ |

## Compatibility boundaries

- Prefer Boot-managed dependency versions. Do not add independent version pins,
  change BOM precedence, switch Boot lines or upgrade Java without a justified,
  authorized change. Check unmanaged dependencies separately.
- Do not assume Boot, Framework, Spring Data, Hibernate or Jackson minor numbers
  correspond. Check the resolved artifacts individually.
- For Jackson, check the actual major version and package names before selecting
  serializers or mappers; verify stored and wire-format compatibility.
- For test support, check the resolved test starters, annotation packages,
  Testcontainers modules and container APIs before copying examples.
- For persistence, verify provider capabilities, Jakarta API compatibility and
  actual database/server behavior. A successful compilation is not proof of
  transaction, query or serialization semantics.
- For new projects, resolve a supported baseline against the approved technology
  choices and official compatibility information before selecting examples.
- If resolution or matching documentation is unavailable, report the missing
  evidence, avoid speculative APIs, and label compatibility unverified.
  Do not silently replace the project's stack or claim tests were run.

Record the affected versions, evidence source, relevant overrides, checks run
and remaining uncertainty when compatibility materially affects the change.
