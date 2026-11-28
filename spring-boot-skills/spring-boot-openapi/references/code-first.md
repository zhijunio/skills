# Code-first OpenAPI

Use this workflow when Spring MVC handlers and DTOs define the API and
springdoc publishes the document.

## Dependencies and versions

When the user requests OpenAPI documentation for a new web project, add the
documentation dependency. For an existing project, preserve a compatible
springdoc version. Read the effective Spring Boot version and consult the
[official springdoc compatibility matrix](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).
Verify the selected artifact version against published release metadata before adding
springdoc or replacing an incompatible version. Choose the servlet MVC or reactive WebFlux starter according to the actual stack, and API-only or UI support according to the requested capability. Do not install both web stacks merely to publish documentation.

If JavaDoc extraction is selected, use Therapi runtime JavaDoc support and its compiler annotation processor,
using compatible published versions of both artifacts; preserve existing compatible pins.
Consult the [Therapi project documentation](https://github.com/dnault/therapi-runtime-javadoc)
when introducing or upgrading them. Merge the processor path with
existing compiler configuration rather than replacing it.

## Document the API

Write JavaDoc on controller handler methods and response types. The operation
comment describes the behavior and @return describes the successful response.
Verify those descriptions in the generated OpenAPI document.

Do not assume a global exception handler adds its error responses to each
operation. Document known validation, not-found, and conflict responses with
`@ApiResponse` and the actual error media type/schema, then verify the generated
document includes those status codes. For example:

```java
@ApiResponse(
    responseCode = "404",
    description = "Product not found",
    content = @Content(
        mediaType = "application/problem+json",
        schema = @Schema(implementation = ProblemDetail.class)))
@GetMapping("/{id}")
public ProductResponse findById(@PathVariable Long id) { ... }
```

Therapi does not expose record-component or explicit record-accessor JavaDoc as
schema property descriptions. When a generated property requires a description,
use a JavaBean DTO with documented fields. Add @Schema only for metadata that
JavaDoc cannot express.

Make production documentation exposure an explicit deployment decision. Disable both by default for internal-only documentation; for an approved public or authenticated documentation endpoint, preserve its access policy and avoid exposing internal operations or example secrets. Example for internal-only documentation:

```yaml
# application-prod.yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

## Verify

Inspect the active dependency and profile configuration. Retrieve the generated
OpenAPI document and confirm paths, request and response schemas, JavaDoc
descriptions, error responses, and production endpoint disabling. Missing
JavaDoc metadata can fail silently, so verify the published document itself. Compare it against actual HTTP responses, including security-filter errors, validation and ProblemDetail extension fields; verify exposure through the actual deployed route and active profiles.
