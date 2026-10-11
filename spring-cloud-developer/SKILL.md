---
name: spring-cloud-developer
description: Designs and implements Spring Cloud features for Java applications. Use when working with Spring Cloud Gateway, centralized configuration, service discovery, client-side load balancing, service-to-service resilience, Spring Cloud Stream, or when diagnosing Spring Cloud compatibility and runtime behavior. Establish the project's Spring Boot and Spring Cloud versions before giving version-specific guidance.
license: MIT
metadata:
  author: Avinay Basnet
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

Preserve the project's existing dependency-management model. Prefer the compatible Spring Cloud BOM and Boot-managed dependencies; do not pin individual Spring Framework, Spring Cloud, or transitive library versions without a demonstrated need.

## Design rules

- Add only the component needed for a stated operational or architectural requirement. A monolith does not need discovery or a gateway merely because Spring Cloud offers them.
- Keep service contracts, domain behavior, persistence, and application-level security in the owning service. Cloud infrastructure routes, configures, discovers, or protects traffic; it does not replace those boundaries.
- Make network failure explicit: set timeouts, bound retries and concurrency, use idempotency where requests may be replayed, and define fallback behavior. Avoid retry amplification across gateway and service layers.
- Treat configuration sources, forwarded headers, service identities, discovery metadata, and broker messages as trust boundaries. Apply authentication and authorization at the resource that owns the data or action.
- Design for replicas and partial failure. Identify which state is local, shared, or durable, and verify that routing, sessions, caches, discovery, and messaging behave correctly across instances.
- Prefer observable, reversible rollout and configuration changes. Keep secrets out of source-controlled configuration and constrain management endpoints.

## Workflow

1. Read the project's version and deployment configuration before suggesting changes.
2. State the failure mode or capability being addressed and choose the smallest appropriate Spring Cloud component.
3. Verify dependency coordinates, properties, defaults, and APIs against documentation for the project's release train.
4. Keep configuration typed and validated where application-owned settings are bound; keep credentials external to the repository.
5. Test the real integration boundary: gateway routing and filters, config loading and refresh, discovery/load balancing, resilience behavior, or broker integration as applicable. Unit tests alone do not prove distributed behavior.
6. Build and run relevant tests with the project's wrapper, then document local dependencies and operational assumptions.

## Topic map

This entry point establishes scope and working rules. Component-specific guidance will be added as focused references and linked here:

- API gateway and edge routing: Spring Cloud Gateway.
- Externalized and distributed configuration: Spring Cloud Config.
- Service discovery and client-side load balancing.
- Service-to-service resilience and its interaction with Spring Framework resilience.
- Event-driven integration with Spring Cloud Stream and binders.
- Compatibility, upgrades, observability, security, and integration testing.

For Spring Boot application concerns, see [spring-boot-developer](../spring-boot-developer/SKILL.md). For OAuth2/OIDC and SAML login or federation, see [spring-sso-developer](../spring-sso-developer/SKILL.md).
