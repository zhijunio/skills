# Spring Cloud Gateway

Use a gateway when there is a concrete edge responsibility: route aggregation, shared protocol
handling, request shaping, or centrally enforced edge policy. A gateway adds a network hop and an
operational failure point; it does not replace authorization or domain validation in the service that
owns the data.

## Choose the server model

Spring Cloud Gateway Server has separate WebFlux and Web MVC variants. Choose based on the runtime
stack and blocking model; do not add WebFlux solely because a gateway example uses it.

| Variant | Fits | Key constraint |
| ------- | ---- | -------------- |
| Server WebFlux | Reactive edge applications and non-blocking downstream calls | Runs on the reactive stack; blocking filters or clients can consume event-loop threads. |
| Server Web MVC | Servlet applications using Spring MVC and WebMvc.fn | Uses servlet infrastructure; reactive-only APIs and assumptions do not apply. |

The starter names and route syntax are version-sensitive. Use the release-train BOM and the official
[Gateway reference](https://docs.spring.io/spring-cloud-gateway/reference/). The current Web MVC
starter is `org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc`; verify the
artifact against the train selected for the project.

## Route deliberately

Prefer explicit routes with stable IDs, predicates, and destinations. Discovery-based route creation
is disabled by default in Gateway; enabling it can expose every registered service unless routes are
filtered and protected. Do not publish management ports or internal-only services through a broad
catch-all route.

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: catalog-read
              uri: ${CATALOG_BASE_URL}
              predicates:
                - Path=/api/catalog/**
              filters:
                - StripPrefix=2
```

Property prefixes differ between WebFlux and Web MVC variants and across older release trains.
Treat the example as a route shape; confirm the property namespace and supported filters in the
selected version's reference.

## Security and resilience boundaries

- Authenticate requests at the edge where shared authentication is required, but authorize the
  requested action at the resource-owning service as well.
- Do not trust identity headers supplied by clients. Strip or overwrite forwarded identity headers at
  the trusted edge, and use authenticated token relay only when the downstream contract requires it.
- Set connection and response timeouts based on the downstream service's budget. Propagate request
  IDs and tracing context without logging credentials or tokens.
- Bound request body sizes, concurrent connections, and rate limits. Derive rate-limit keys from
  authenticated identity or another trusted attribute; an attacker-controlled header is not an
  identity key.
- Make retries opt-in for idempotent operations. Bound attempts and total time, and avoid retrying in
  both gateway and service clients.
- Define route-level health and fallback behavior explicitly. A fallback must not turn authorization
  failure or invalid input into a successful response.

## Verification

Test the route table and filters in the selected server variant, then exercise a real downstream test
service. Cover path rewriting, query strings, headers, authentication, denied routes, timeouts,
retry/fallback behavior, body limits, and metrics. A controller test without an HTTP hop does not
verify proxy behavior.

## Sources

- [Spring Cloud Gateway reference](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Gateway Server Web MVC starter](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/starter.html)
- [Gateway Security integration](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/spring-security.html)
- [Gateway configuration properties](https://docs.spring.io/spring-cloud-gateway/reference/configprops.html)
