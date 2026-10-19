---
name: spring-cloud-developer
description: Designs and implements Spring Cloud features for Java applications. Use when working with Spring Cloud Gateway, centralized configuration, service discovery, service-to-service HTTP clients and resilience, Spring Cloud Stream, contract testing, or when diagnosing Spring Cloud compatibility and runtime behavior. Establish the project's Spring Boot and Spring Cloud versions before giving version-specific guidance.
license: MIT
metadata:
  author: Zhijunio
  version: '1.0'
---

# Spring Cloud Developer

Use this skill for distributed-system capabilities provided by Spring Cloud. Use [spring-boot-developer](../spring-boot-developer/SKILL.md) for application-level Spring Boot structure and features; use this skill for Spring Cloud components and the boundaries between services.

## Establish the project

Before proposing a dependency or API, inspect the build files, dependency management, configuration, and deployment shape. Establish:

1. Spring Boot and Java versions from the project, not the installed toolchain.
2. The Spring Cloud release train managed by the project and whether it is compatible with that Boot line. Check the official [Spring Cloud release train compatibility table](https://spring.io/projects/spring-cloud#overview) and the project's BOM; do not infer compatibility from a familiar version number.
3. Which Cloud components are already present and the concrete problem each should solve.
4. The runtime topology: single process, multiple service instances, gateway, config server, discovery system, broker, and deployment platform as applicable.
5. Which service or platform owns each concern, including service identity, authorization, configuration, message delivery, and health reporting.

Preserve the project's existing dependency-management model. Prefer the compatible Spring Cloud BOM and Boot-managed dependencies; do not pin individual Spring Framework, Spring Cloud, or transitive library versions without a demonstrated need.

## Design rules

- Add only the component needed for a stated operational or architectural requirement. A monolith does not need discovery or a gateway merely because Spring Cloud offers them.
- Keep service contracts, domain behavior, persistence, and application-level security in the owning service. Cloud infrastructure routes, configures, discovers, or protects traffic; it does not replace those boundaries.
- Make network failure explicit: set timeouts, bound retries and concurrency, use idempotency where requests may be replayed, and define fallback behavior. Avoid retry amplification across gateway and service layers.
- For service-to-service HTTP, prefer the project's established client. Use Boot's HTTP Service Client/`RestClient` for straightforward blocking calls; add Spring Cloud LoadBalancer or OpenFeign only for service-name resolution, client-side instance selection, or a useful declarative client boundary.
- For messaging, define delivery, ordering, replay, duplicate handling, and dead-letter ownership explicitly. A binder abstraction does not make broker guarantees identical or provide exactly-once business effects.
- Treat configuration sources, forwarded headers, service identities, discovery metadata, and broker messages as trust boundaries. Apply authentication and authorization at the resource that owns the data or action.
- Design for replicas and partial failure. Identify which state is local, shared, or durable, and verify that routing, sessions, caches, discovery, and messaging behave correctly across instances.
- Prefer observable, reversible rollout and configuration changes. Keep secrets out of source-controlled configuration and constrain management endpoints.
- Use constructor injection for required dependencies and avoid field injection. Do not add Lombok by default; follow an existing project's convention when it already uses Lombok.
- Preserve the application's established web stack. For new servlet applications, Spring MVC is the default; choose Gateway WebFlux or Web MVC from the gateway's actual execution model and do not mix blocking work into reactive event loops.
- Prefer Spring Boot's managed starters, auto-configuration, validated `@ConfigurationProperties`, and built-in test support before adding custom infrastructure.

## Workflow

1. Read the project's version and deployment configuration before suggesting changes.
2. State the failure mode or capability being addressed and choose the smallest appropriate Spring Cloud component.
3. Verify dependency coordinates, properties, defaults, and APIs against documentation for the project's release train.
4. Keep configuration typed and validated where application-owned settings are bound; keep credentials external to the repository.
5. Choose the narrowest useful test: unit tests for policy and mapping, binder/test-binder tests for message functions, and integration tests for routing, Config Data, discovery, load balancing, remote failures, broker behavior, and serialization. Cover success and rejection/failure paths. A test binder or mocked client does not prove the real network or broker boundary.
6. Build and run relevant tests with the project's wrapper. Report commands and actual results, then document local dependencies and operational assumptions.

## Topic map

Use the focused references below after establishing the project's exact Spring Boot, Java, and Spring Cloud release-train versions:

- Compatibility and support windows: [version-compatibility.md](references/version-compatibility.md).
- API gateway and edge routing: [gateway.md](references/gateway.md).
- Externalized and distributed configuration: [config.md](references/config.md).
- Service discovery and registry selection: [service-discovery.md](references/service-discovery.md).
- Service-to-service HTTP clients and client-side load balancing: [http-clients.md](references/http-clients.md).
- Timeouts, retries, circuit breakers, and bulkheads: [resilience.md](references/resilience.md).
- Functional messaging, binders, consumer groups, failure handling, and testing: [stream.md](references/stream.md).
- Producer/consumer HTTP and messaging contracts: [contract-testing.md](references/contract-testing.md).
- Observability, security, and integration testing are cross-cutting concerns; apply the matching Spring Boot and Spring Security guidance and test the real remote boundary.

For Spring Boot application concerns, see [spring-boot-developer](../spring-boot-developer/SKILL.md). For OAuth2/OIDC and SAML login or federation, see [spring-sso-developer](../spring-sso-developer/SKILL.md).
