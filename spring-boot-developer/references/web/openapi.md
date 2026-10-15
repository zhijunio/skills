# OpenAPI

Two approaches, and the choice is about **who owns the contract**.

| | Code-first (springdoc) | Spec-first (generator) |
|---|---|---|
| Source of truth | The Java code | `openapi.yaml` |
| Spec produced by | Runtime introspection | You, by hand |
| Drift | Impossible - it is generated from the code | Possible; the build must enforce it |
| Use when | One team owns both sides; the API serves your own clients | Multiple teams or languages consume it; the contract is negotiated before implementation |

**Default to code-first.** Spec-first is worth its cost when the contract is a real interface between teams, and a liability when it is ceremony around a single team's API.

## Code-first with springdoc

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version><!-- check for the release matching your Boot line --></version>
</dependency>
```

springdoc is not managed by Boot's BOM, so you set the version - and the major line must match your Boot/Framework generation. Verify against springdoc's compatibility matrix rather than copying a version.

The spec appears at `/v3/api-docs` and Swagger UI at `/swagger-ui.html` with no further configuration. Most of what you need beyond that is metadata:

```java
@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI orderServiceApi(BuildProperties buildProperties) {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version(buildProperties.getVersion())
                        .description("Ordering and fulfilment."))
                .components(new Components().addSecuritySchemes("bearer-jwt",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

Taking the version from `BuildProperties` keeps the published spec in step with the deployed artifact - see [maven.md](../tooling/maven.md) for generating it.

### What springdoc infers, and what it cannot

It reads your mappings, records, and Jakarta Validation constraints - `@NotBlank` becomes `required`, `@Size(max = 100)` becomes `maxLength`. That is a strong reason to keep validation on the request record ([validation.md](validation.md)): it documents the API for free.

It cannot infer intent. Annotate only what is genuinely not derivable:

```java
@Operation(summary = "Cancel an order",
           description = "Only permitted while the order is PENDING.")
@ApiResponse(responseCode = "204", description = "Cancelled")
@ApiResponse(responseCode = "409", description = "Already shipped",
             content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
@PostMapping("/{id}/cancel")
ResponseEntity<Void> cancel(@PathVariable OrderId id) { … }
```

Resist annotating every parameter and field. A controller where the annotations outweigh the code is harder to read and no better documented - springdoc already knew the types.

### Javadoc support

For code-first APIs, springdoc can use Javadocs as documentation instead of repeating descriptions in Swagger annotations:

- A controller method's Javadoc becomes the OpenAPI operation description.
- The method's `@return` text becomes the response description.
- A DTO field's Javadoc becomes that schema property's description.

This requires Therapi at both compile time and runtime. Add its annotation processor and runtime reader; the current springdoc documentation uses Therapi `0.15.0`:

```xml
<properties>
    <therapi.version>0.15.0</therapi.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.github.therapi</groupId>
        <artifactId>therapi-runtime-javadoc</artifactId>
        <version>${therapi.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>com.github.therapi</groupId>
                        <artifactId>therapi-runtime-javadoc-scribe</artifactId>
                        <version>${therapi.version}</version>
                    </path>
                    <!-- Keep any other annotation processors already used by the project. -->
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Gradle equivalent:

```kotlin
dependencies {
    implementation("com.github.therapi:therapi-runtime-javadoc:0.15.0")
    annotationProcessor("com.github.therapi:therapi-runtime-javadoc-scribe:0.15.0")
}
```

Write useful contract documentation at the API boundary:

```java
/**
 * Moves a task to the next allowed lifecycle state.
 *
 * @return the task after the state change
 */
@PutMapping("/{id}/status")
TaskResponse changeStatus(@PathVariable UUID id, @RequestBody ChangeTaskStatusRequest request) { … }
```

Keep Javadoc focused on externally visible behavior: allowed transitions, sorting or paging defaults, units, nullability, and relevant failure conditions. Do not repeat what the Java type or validation annotation already says. Javadoc on Java record components can vary with compiler and processor support; inspect the generated schema, and use an explicit `@Schema(description = …)` for a property whose description must be guaranteed. If both Javadoc and a Swagger annotation provide the same description, the Swagger annotation takes precedence.

The processor setup is easy to omit and springdoc may then generate a spec with no error but no Javadocs. Test the generated `/v3/api-docs` for representative operation, response, and schema descriptions. Modern `springdoc-openapi-starter-*` modules include Javadoc integration; do not add the removed legacy `springdoc-openapi-javadoc` module.
See [springdoc's Javadoc support documentation](https://springdoc.org/#javadoc-support) for the current integration details.

Document error responses as `ProblemDetail` so consumers see the real shape ([error-handling.md](error-handling.md)).

Framework 7's native API-version resolver does not necessarily appear in springdoc's inferred operation parameters. When versioning through a request header, explicitly document that header on the affected operations (for example with `@Parameter(name = "API-Version", in = ParameterIn.HEADER, required = false, ...)`) or add it through a tested OpenAPI customiser. Match `required` to the configured default-version behavior, and assert the generated `/v3/api-docs` includes it.

### Value objects in the schema

A value object with `@JsonValue` serialises as a scalar but springdoc may still document it as an object. Register the mapping once:

```java
static {
    SpringDocUtils.getConfig().replaceWithClass(OrderId.class, UUID.class);
}
```

Or put `@Schema(type = "string", format = "uuid")` on the record. Without this the published schema contradicts the actual payload.

### Locking the spec in CI

Code-first cannot drift from the code, but it can drift from what consumers expect. If anyone depends on your API, commit the generated spec and diff it in CI - springdoc's Maven plugin can write `/v3/api-docs` to a file during `integration-test`. A failing diff forces the breaking change to be a deliberate act, and tells you when to version ([api-versioning.md](api-versioning.md)).

### Production

Ship the spec, not the UI:

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: false      # in production
```

Swagger UI in production is an unnecessary attack surface and an interactive client for your API. If it must be exposed, put it behind authentication - and make sure `/v3/api-docs` is not accidentally `permitAll` in your security config ([security-fundamentals.md](../security/security-fundamentals.md)).

## Spec-first with the generator

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <version><!-- check the current release --></version>
    <executions>
        <execution>
            <goals><goal>generate</goal></goals>
            <configuration>
                <inputSpec>${project.basedir}/src/main/resources/openapi.yaml</inputSpec>
                <generatorName>spring</generatorName>
                <apiPackage>com.example.app.api</apiPackage>
                <modelPackage>com.example.app.api.model</modelPackage>
                <output>${project.build.directory}/generated-sources/openapi</output>
                <configOptions>
                    <delegatePattern>true</delegatePattern>
                    <skipDefaultInterface>true</skipDefaultInterface>
                    <useSpringBoot4>true</useSpringBoot4>
                    <useJackson3>true</useJackson3>
                    <useTags>true</useTags>
                    <dateLibrary>java8</dateLibrary>
                    <openApiNullable>false</openApiNullable>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- `useSpringBoot4=true` and `useJackson3=true` together target Boot 4. `useSpringBoot3` is the Boot 3 line; the generator turns it off when `useSpringBoot4` is set. Without `useJackson3`, generated code still imports Jackson 2 (`com.fasterxml.jackson.databind`) and does not compile on Boot 4. Use a generator release that provides both options.
- `delegatePattern=true` generates an interface you implement, leaving the generated controller untouched.
- `skipDefaultInterface=true` - otherwise the generated interface has default methods returning 501 and a missing implementation compiles silently.
- Generate into `target/`, never into `src/`. Add it to `.gitignore` and let the build produce it.

The generator's Spring templates lag Boot releases. Verify the generated code compiles against your Boot version before committing to this approach.

### Implement the delegate

```java
@Service
class OrdersApiDelegateImpl implements OrdersApiDelegate {

    private final OrderService orderService;

    OrdersApiDelegateImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ResponseEntity<OrderResponse> createOrder(CreateOrderRequest request) {
        var id = orderService.place(OrderApiMapper.toCommand(request));
        return ResponseEntity.created(URI.create("/api/orders/" + id.value()))
                .body(OrderApiMapper.toResponse(orderService.getById(id)));
    }
}
```

**Generated models are not domain models.** Map at this boundary. Letting a generated class reach the service means regenerating the spec refactors your domain.

### Keeping the spec honest

Spec-first only works if the spec is authoritative. Enforce it:

- Lint it in CI (Spectral or the generator's `validate` goal) before generating.
- Never edit generated code - the next build discards it.
- Review spec changes like API changes, because they are.

### Generated clients for frontend consumers

When a browser application consumes this API, an OpenAPI-generated TypeScript client can keep request and response shapes aligned. It is optional; a small single-team application may reasonably use a handwritten client. If generation is used:

- Treat the published OpenAPI document as the client contract. Include success responses, status codes, validation errors, and `ProblemDetail` schemas; verify the generated document rather than assuming annotations were inferred correctly.
- Pin the generator version and configuration. Generate into a dedicated directory and never hand-edit generated output.
- Regenerate after contract changes, then run the frontend type check. In CI, generate into a temporary or clean directory and fail when committed generated output differs, if generated code is committed.
- Fix the Java schema or OpenAPI definition when generated types are wrong; generator flags cannot restore wire semantics missing from the schema.
- Keep generated wire types out of domain and UI state. Map them once in the client/service boundary so regeneration does not spread through the application.
- Check Java-to-TypeScript representations for large `Long` values, exact `BigDecimal` values, date-only values, timestamps, nullability, and omitted fields. See [json-and-jackson.md](json-and-jackson.md).

The generator target follows the client transport and project needs, not whether the frontend uses Vue, React, or another framework. A handwritten client should still consume the same documented contract and handle the same status and body semantics.

## Gotchas

- Agent adopts spec-first for a single-team API - code-first with springdoc is less machinery and cannot drift
- Agent pins a springdoc version that does not match the Boot line - check the compatibility matrix; it is not BOM-managed
- Agent annotates every field with `@Schema` - springdoc infers types and validation constraints already
- Agent expects Javadocs to work without the Therapi runtime library and annotation processor - support can fail silently; test the generated spec
- Agent adds the legacy `springdoc-openapi-javadoc` module to a modern starter - Javadoc integration is included in the starter; add Therapi reader and processor instead
- Agent relies on record-component Javadocs without checking processor output - verify `/v3/api-docs` and use `@Schema` for descriptions that must be present
- Agent documents errors as a bespoke object - document `ProblemDetail`
- Agent leaves Swagger UI enabled in production - ship the spec, disable the UI
- Agent adds `/v3/api-docs` to `permitAll` without thinking - that publishes the full API surface anonymously
- Agent lets a `@JsonValue` value object document as an object - the schema then contradicts the payload
- Agent assumes springdoc discovers Framework 7's API-version header automatically - declare the header explicitly or use a tested OpenAPI customiser, then inspect the generated spec
- Agent sets `useSpringBoot3` for a Boot 4 project, or sets `useSpringBoot4` without `useJackson3` - generated code stays on Jackson 2 and does not compile
- Agent omits `skipDefaultInterface=true` - a missing implementation compiles and returns 501 at runtime
- Agent commits generated sources - generate into `target/` and gitignore them
- Agent edits generated controllers - implement the delegate; edits are lost on the next build
- Agent passes generated models into the service layer - map at the delegate

## Related

- [rest-controllers.md](rest-controllers.md) · [validation.md](validation.md) · [error-handling.md](error-handling.md) · [api-versioning.md](api-versioning.md) · [security-fundamentals.md](../security/security-fundamentals.md)

## References

- [springdoc's Javadoc support documentation](https://springdoc.org/#javadoc-support)
