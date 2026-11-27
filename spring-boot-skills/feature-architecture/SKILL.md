---
name: feature-architecture
description: >
  Organize or review a Spring Boot application's packages around business capabilities,
  feature ownership, and cross-feature dependencies. Use when code is scattered by technical
  layer or a feature's change spans many unrelated packages.
---

# Feature Architecture

Use feature-first packaging to keep the code that changes together close together. This skill
defines top-level feature boundaries and package ownership; [layered architecture](../layered-architecture/SKILL.md)
defines responsibilities inside a feature, [hexagonal architecture](../hexagonal-architecture/SKILL.md)
defines ports and adapters, [ArchUnit](../archunit/SKILL.md) applies Taikai defaults and custom static
package rules, and [Spring Modulith](../spring-modulith/SKILL.md) verifies Spring module boundaries.

## Choose feature boundaries

- Name features after business capabilities or bounded contexts, such as `orders`, `catalog`, and
  `identity`, not technical roles such as `controllers` or `repositories`.
- Give each feature ownership of its rules, application behavior, persistence mapping, and
  browser/API entry points. Keep feature-specific changes inside that feature where practical.
- Split a feature when it has an independent vocabulary, lifecycle, or change cadence. Avoid a
  package for every noun and avoid turning each feature into a separate deployable service by default.
- Keep cross-cutting infrastructure such as application configuration and observability outside
  feature packages when it is genuinely shared.

## Package by feature

Start with a shallow structure. Add internal subpackages only when a feature grows enough to need
them; do not force every feature into identical scaffolding.

```text
com.example.shop/
├── orders/
│   ├── CreateOrder.java
│   ├── Order.java
│   ├── OrderRepository.java
│   ├── OrderController.java
│   └── OrderResponse.java
├── catalog/
│   ├── Product.java
│   ├── ProductRepository.java
│   └── ProductController.java
└── shared/
    └── Money.java
```

For a larger feature, group its own implementation by role:

```text
orders/
├── api/
├── application/
├── domain/
└── infrastructure/
```

Treat these as alternatives at different scales, not as two package trees to maintain at once.

## Keep dependencies local

- Make feature packages the default ownership boundary. Prefer package-private types; expose only
  the small API another feature needs.
- Let a feature call another feature through that API or a deliberate application event. Do not
  reach into its repositories, entities, or internal services.
- Avoid a catch-all `common`, `shared`, or `utils` package. Promote a type there only when multiple
  features share the same stable meaning and ownership is clear.
- Keep database relationships from silently becoming code dependencies. Cross-feature reads and
  writes should follow an explicit contract owned by the relevant features.
- If dependency cycles appear, identify the business ownership and direction first; do not hide the
  cycle behind a generic mediator or move unrelated code into `shared`.

## Change an existing application

1. Map current packages to business capabilities and identify types used across feature boundaries.
2. Choose one cohesive feature and move its code without changing runtime behavior.
3. Replace cross-feature imports with the smallest explicit API needed by callers.
4. Check component scanning, JPA entity and repository discovery, configuration imports, and tests
   after package moves. Keep existing public routes and persistence behavior stable.
5. Repeat feature by feature. Add Spring Modulith verification only when module enforcement is
   requested or needed to prevent observed boundary violations.

## Boundaries with neighboring skills

- Use [layered architecture](../layered-architecture/SKILL.md) when the question is where controller,
  service, repository, or DTO responsibilities belong.
- Use [hexagonal architecture](../hexagonal-architecture/SKILL.md) when the question is about domain
  independence, ports, or adapters.
- Use [Spring Modulith](../spring-modulith/SKILL.md) when package boundaries need executable checks,
  module documentation, or event-based integration.

## Gotchas

- Agent moves all controllers into `web/` and recreates global layer packages — keep top-level
  packages aligned with business capabilities.
- Agent creates a separate Spring module for every feature — package boundaries do not require
  separate Maven modules or services.
- Agent shares JPA entities between features — expose a contract or identifier instead of internal
  persistence types.
- Agent adds a large `shared` package — keep ownership with the feature that owns the behavior.
