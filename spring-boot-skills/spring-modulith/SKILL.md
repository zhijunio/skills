---
name: spring-modulith
description: >
  Use when organizing or verifying a modular Spring Boot monolith with Spring Modulith, including module APIs, dependency rules and reliable module events. Do not restructure an application into modules for an unrelated endpoint change.
---

# Spring Modulith

For version-sensitive changes, follow [shared version resolution](../spring-boot/references/version-resolution.md). If unavailable, inspect resolved dependencies and release-matched official documentation; report missing compatibility evidence.

Check the official Boot/Modulith compatibility matrix and the resolved event registry starter. Verify listener and republication APIs against that version. Treat the pinned upstream fixture as upstream evidence only, not as a locally executed test.

## Spring Boot 4 baseline

Select a Spring Modulith 2.x release supported by the project's Boot 4 line according to the official compatibility matrix; do not infer compatibility from matching minor numbers. Do not import a Boot 3 Modulith 1.x BOM.

## Apply within the existing architecture

Inspect the application root package, module boundaries and current dependency graph.
Use direct subpackages as modules; keep their API at the module root and implementation in
internal subpackages. Expose a named interface only for an intentional shared API.
Use allowedDependencies when the architecture needs explicit dependency constraints.
Avoid creating a generic shared module that every feature depends on.

## Verify boundaries

Add spring-modulith-starter-test with test scope and run
ApplicationModules.of(Application.class).verify() in CI. It checks cycles and access to module
internals. Add ApplicationModuleTest for behavior confined to a module. A passing structure
check does not prove transactional or delivery correctness.

See [good module verification](examples/module-verification-test.java) and
[bad cross-module dependency](examples/module-internal-access.java).

## Reliable events

Use [application transactions](../spring-boot-webmvc/references/transactions.md) for local
transaction boundaries and rollback; this skill owns module event publication and recovery.

Publish immutable event payloads containing IDs and necessary facts, not managed JPA entities.
Use ApplicationModuleListener for asynchronous transactional listeners when that timing fits.
For delivery that must survive a process crash, configure a persistent event publication
registry using the chosen JDBC/JPA starter and manage its schema through migrations.
An in-memory registry and a plain after-commit callback are not durable delivery.

Make handlers idempotent: a crash after the business effect but before recording completion
can cause redelivery. Define retry/republication and retention explicitly, and test failed
listeners and application restarts. For external brokers, use supported event externalization
with persistent publication tracking; do not promise end-to-end exactly-once processing.

## Verification scope

The pinned upstream workflow fixture checks a valid public-API dependency and rejection of an internal
package dependency using real ApplicationModules verification. It uses separate minimal
fixture packages, not the application's Application class; durable event publication,
listener retries and process-restart behavior are not covered.
See the [coverage matrix](https://github.com/rrezartprebreza/spring-boot-skills/blob/b90c3dbd9699c0d0bfb81660c857ef58262168e6/verification/MATRIX.md)
for pinned versions and remaining gaps.

## Gotchas

- Agent imports a repository from another module's internal package - call its public API or consume an event.
- Agent adds Modulith to a simple unrelated task - preserve the requested scope.
- Agent assumes ApplicationModuleListener alone guarantees durability - configure persistent publication tracking.
- Agent publishes an entity with lazy associations - publish an immutable event contract.
- Agent tests only the happy path - verify cycles, module access and event redelivery.

## Official sources

- [Module verification](https://docs.spring.io/spring-modulith/reference/verification.html)
- [Events and publication registry](https://docs.spring.io/spring-modulith/reference/events.html)
- [Compatibility](https://docs.spring.io/spring-modulith/reference/appendix.html)
