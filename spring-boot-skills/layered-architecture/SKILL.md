---
name: layered-architecture
description: >
  Design or review controller, service, data-access and DTO boundaries in a Spring Boot
  application that uses layered architecture, or when the user requests that architecture.
---

# Layered Architecture

Use the existing application's architecture and package conventions. Apply these responsibilities
when layered architecture is chosen; do not reorganize an unrelated feature or introduce a new
architecture merely to satisfy a template. Layered responsibilities can coexist with feature-first
packaging. Use [feature architecture](../feature-architecture/SKILL.md) for feature ownership and
[hexagonal architecture](../hexagonal-architecture/SKILL.md) when ports and adapters are chosen.

## Responsibilities and dependency direction

| Responsibility | Owns |
| --- | --- |
| Controller / HTTP adapter | Request binding, transport validation, response and error mapping |
| Application service | Use-case coordination, authorization integration, transaction boundaries |
| Domain behavior (where needed) | Business invariants, calculations and legal state transitions |
| Repository / data adapter | Queries, persistence mapping and data access |
| Configuration | Bean assembly, infrastructure and application wiring |

Keep protocol-specific concerns out of business behavior and persistence. Keep business decisions
out of HTTP adapters and data-access glue. Simple CRUD can use a small service and persistence
model; introduce richer domain objects only when the behavior warrants them. Do not move existing
entity invariants into a service simply because the application uses layers.

## Controller and application service

- Validate transport inputs at the boundary and enforce business invariants in the appropriate
  domain or application service, including calls that bypass HTTP.
- Delegate business orchestration to an application service. One service call is often a clear
  command boundary, not a required count for every endpoint. Multiple independent read operations
  can be appropriate; coordinated writes belong in an explicit use case.
- Use DTOs for new public HTTP contracts rather than serializing persistence entities or lazy
  associations. Preserve existing public contracts; review any intentional contract migration.
- Prefer constructor injection for new required collaborators; use existing project conventions
  and do not introduce Lombok solely for injection.
- Keep multi-step atomic operations in a Spring-managed application service. Use
  [transaction guidance](../spring-boot-webmvc/references/transactions.md) for propagation,
  rollback, proxy boundaries and commit-dependent effects. Repository-local transactions are
  legitimate; they do not replace a use-case transaction spanning multiple operations.
- Organize services by cohesive responsibility or use case, not a mandatory one-per-aggregate
  rule. Avoid unrelated responsibilities in one service, but allow cross-aggregate coordination
  where the business operation requires it.
- Keep authentication, ownership and permission checks at the relevant enforced boundary;
  use [security](../spring-boot-security/SKILL.md). Never rely on controller-only validation
  or authorization when another entry point can invoke the same operation.

For MVC binding, errors, pagination and response semantics, use
[Web MVC](../spring-boot-webmvc/SKILL.md). For retries or concurrency controls, use
[resilience](../resilience-retry/SKILL.md) and the actual framework version and existing retry
policy. Do not add or replace a retry framework solely because this skill is loaded.

## Persistence and mapping

Preserve the selected persistence technology. Use [JPA](../spring-data-jpa/SKILL.md),
[MyBatis-Plus](../spring-boot-mybatis-plus/SKILL.md) or the project's established JDBC/data-access
approach. Choose derived queries, projections, explicit SQL or another supported query mechanism
according to the contract, maintainability and database behavior. Keep business policy separate
from query mechanics without discarding useful database constraints or atomic operations.

Separate request and response models when their validation, security or compatibility needs
differ. Prefer records for suitable immutable DTOs if the project uses them. Validate domain
invariants independently of DTO annotations. Map responses within the appropriate data access
scope; do not trigger lazy loads accidentally during serialization. Simple mapping may remain
local; extract a mapper when complexity or reuse justifies it. Use readable loops, streams or
existing mapping tools without introducing dependencies by default.

The [layered service example](examples/layered-service.java) and
[mixed-boundary example](examples/service-mixed-boundaries.java) illustrate responsibilities,
not mandatory class names or coding styles. The request, response and mapper
[templates](templates/CreateOrderRequest.java) are adaptable fragments; also see
[OrderResponse](templates/OrderResponse.java) and [OrderMapper](templates/OrderMapper.java).
Choose one mapping approach rather than duplicating it.

## Configuration and cross-cutting concerns

Keep configuration focused on assembly and policy. Configuration may depend on application beans
when that is necessary to wire the chosen design; avoid circular dependencies and business side
effects during bean creation. Use [configuration guidance](../spring-boot-configuration/SKILL.md)
for typed properties and secrets. Follow existing logging and exception-handling conventions;
preserve sensitive data boundaries and use centralized mapping for shared HTTP error contracts
without requiring exactly one advice class. Use version-compatible APIs and libraries.

## Review and verify

Identify an actual responsibility violation before moving code. Preserve routes, response shape,
persistence selection, package scanning, security and transaction semantics. Test the affected
behavior and failure paths, including rollback and denied operations where relevant. Use
[testing](../spring-boot-testing/SKILL.md) for the appropriate scope and
[ArchUnit](../archunit/SKILL.md) only when static enforcement is relevant. Report actual checks
and remaining gaps; a package diagram or passing architecture check alone does not prove behavior.
