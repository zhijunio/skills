---
name: spring-boot-openapi
description: >
  Design, document, or implement Spring Boot HTTP APIs with OpenAPI. Use for
  contract-first generation with OpenAPI Generator or code-first springdoc
  documentation and Swagger UI.
---

# OpenAPI for Spring Boot

Choose the API's existing source of truth or the workflow requested by the user.
Use one workflow for each API:

- [Contract-first](references/contract-first.md) when an OpenAPI document
  generates server interfaces, DTOs, or clients.
- [Code-first](references/code-first.md) when Spring MVC handlers and DTOs
  define the API and springdoc publishes its document.

For contract-first APIs, review the [OpenAPI template](templates/openapi.yaml)
and verify the generated contract against the implemented delegate. Define
operations in the OpenAPI document and implement the generated delegate; do not
add handwritten controller mappings for those operations or edit generated
controllers.
Compare the [delegate implementation](examples/delegate-implementation.java) with the
[generated-controller edit](examples/generated-controller-edit.java) anti-pattern.
For code-first APIs, inspect the runtime-generated OpenAPI document and active
profiles. Do not assume annotations or JavaDoc appeared in the published spec.

