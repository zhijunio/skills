---
name: spring-boot-openapi
description: >
  Design, document, or implement Spring Boot HTTP APIs with OpenAPI. Use for
  contract-first generation with OpenAPI Generator or code-first springdoc
  documentation and Swagger UI.
---

# OpenAPI for Spring Boot

For version-sensitive changes, follow [shared version resolution](../spring-boot/references/version-resolution.md); if unavailable, inspect resolved dependencies and release-matched official documentation, and report missing evidence.

Preserve the existing API paths, version resolver, security and wire contracts unless an approved change requires otherwise. Compare the authoritative contract against the previous supported release, including required fields, nullability, enum changes, status codes, media types and security requirements. Evaluate compatibility separately for requests and responses; even additive response enum values may break generated clients.

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

