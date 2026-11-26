# Spring Boot Project Guidance

## Stack

- Spring Boot 4.x; use the selected line from the repository `versions.json`
- Java 21 by default; preserve the project's selected Java version
- Keep the generated Maven or Gradle wrapper as the build entry point
- New default stack: Spring MVC, PostgreSQL, MyBatis-Plus, Flyway, Redis,
  ArchUnit, and Taikai
- Use Jackson 3 and Boot 4 modular starters
- Add Security, AI, and telemetry integrations only when the project requires them

## Commands

- Maven verification: `./mvnw verify`
- Gradle verification: `./gradlew check bootJar`
- Run focused tests with the selected wrapper and existing project conventions

## Conventions

- Read neighboring classes and dependency management before generating code.
- Keep controllers as HTTP adapters and business rules in application services.
- Use DTOs at HTTP and messaging boundaries.
- Use Boot 4 dedicated starters and Jackson 3 `tools.jackson` APIs.
- Preserve existing error, security, pagination, and observability contracts.
- Keep API documentation disabled in production unless explicitly required.

## Completion

Run the relevant tests and the selected wrapper's verification lifecycle. Report
the exact commands and any checks that could not run.
