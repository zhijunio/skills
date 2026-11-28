---
name: spring-boot-api-versioning
description: >
  Use when versioning Spring MVC or WebFlux APIs in Spring Boot 4 / Spring Framework 7. Covers
  built-in mapping versions, request version resolution, defaults, supported versions, and
  deprecation headers.
---

# Spring Boot API Versioning

For version-sensitive changes, follow [shared version resolution](../spring-boot/references/version-resolution.md); if unavailable, inspect resolved dependencies and release-matched official documentation, and report missing evidence.

Spring Framework 7 provides API versioning in the mapping layer. Prefer it for new versioning designs when it meets the approved contract. Preserve existing `/v1` paths, gateways, custom resolvers and client behavior unless an explicit migration is approved.

## Declare versions on mappings

```java
@RestController
@RequestMapping("/api/orders")
class OrderController {

    @GetMapping(path = "/{id}", version = "1.0")
    OrderV1 getV1(@PathVariable UUID id) { ... }

    @GetMapping(path = "/{id}", version = "1.2")
    OrderV2 getV2(@PathVariable UUID id) { ... }
}
```

Use semantic version strings. A `+` suffix means the mapping handles that version and newer
supported versions, for example `version = "1.2+"`. This does not imply automatic support for arbitrary future versions. Verify supported-version detection and selection when fixed and baseline mappings overlap.

## Configure one request resolution strategy

```java
@Configuration
class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .useRequestHeader("API-Version")
            .setDefaultVersion("1.0")
            .addSupportedVersions("1.0", "1.2");
    }
}
```

For a new API choose one authoritative source: request header, query parameter, path segment, or media-type
parameter. Keep the choice consistent across the application. For an existing API that accepts multiple sources, preserve its documented precedence or rejection behavior; test conflicting values. WebFlux uses the corresponding
`WebFluxConfigurer` hook. For path resolution, configure the version segment index
and declare that segment as a URI variable on versioned routes. For example, use
index 1 with `/api/{version}/orders`.

## Required behavior and deprecation

Versioning is required unless a default is configured or `setVersionRequired(false)` is used.
Missing or unsupported versions should be covered by the API error contract. Use
`StandardApiVersionDeprecationHandler` for `Deprecation`, `Sunset`, and `Link` response headers.

Boot properties can provide defaults under `spring.mvc.apiversion.*` or
`spring.webflux.apiversion.*`, but keep the resolution strategy in Java configuration when its
behavior must be explicit.

## Clients and tests

Configure the client version inserter and version to match the server resolver.
Check supported versions, their response shapes, and the API's error contract for
missing, malformed, conflicting or unsupported versions. Test fixed/baseline selection and unversioned endpoints. Include the resolved version in relevant application and gateway cache keys; for cacheable header-based responses configure the appropriate Vary header and verify intermediary caching. Coordinate OpenAPI documents, generated clients, security rules and deprecation policy. Do not remove a supported version without an approved migration. For deprecation, check the configured response headers.

See the [versioned mapping example](examples/versioned-controller.java) and the
[hard-coded version anti-pattern](examples/hardcoded-api-version.java).

## Gotchas

- Agent replaces a working versioned path without approval - preserve the contract; use built-in mapping versions for new designs where appropriate.
- Agent writes `version = 1` - the value is a semantic version `String`, such as `"1.0"`.
- Agent uses `useHeader` or `useQueryParameter` - use `useRequestHeader` or `useQueryParam`.
- Agent enables versioning without a default or `versionRequired(false)` - un-versioned requests return 400.
- Agent mixes a path segment and a request header - choose one source of truth.
- Agent invents custom sunset headers - use `StandardApiVersionDeprecationHandler`.

[Spring Framework API versioning reference](https://docs.spring.io/spring/reference/7.0/web/webmvc-versioning.html)
