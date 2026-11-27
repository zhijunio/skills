---
name: spring-boot-api-versioning
description: >
  Use when versioning Spring MVC or WebFlux APIs in Spring Boot 4 / Spring Framework 7. Covers
  built-in mapping versions, request version resolution, defaults, supported versions, and
  deprecation headers.
---

# Spring Boot API Versioning

Spring Framework 7 provides API versioning in the mapping layer. Prefer it over hand-rolled
`/v1` prefixes, custom `HandlerMapping` implementations, or version-sniffing filters.

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
versions, for example `version = "1.2+"`.

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

Choose exactly one source: request header, query parameter, path segment, or media-type
parameter. Keep the choice consistent across the application. WebFlux uses the corresponding
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
missing or unsupported versions. For deprecation, check the configured response headers.

See the [versioned mapping example](examples/versioned-controller.java) and the
[hard-coded version anti-pattern](examples/hardcoded-api-version.java).

## Gotchas

- Agent hand-rolls `/api/v1` prefixes or a version filter - use the mapping `version` attribute.
- Agent writes `version = 1` - the value is a semantic version `String`, such as `"1.0"`.
- Agent uses `useHeader` or `useQueryParameter` - use `useRequestHeader` or `useQueryParam`.
- Agent enables versioning without a default or `versionRequired(false)` - un-versioned requests return 400.
- Agent mixes a path segment and a request header - choose one source of truth.
- Agent invents custom sunset headers - use `StandardApiVersionDeprecationHandler`.

[Spring Framework API versioning reference](https://docs.spring.io/spring/reference/7.0/web/webmvc-versioning.html)
