# Architecture

Read this when choosing deployment boundaries, defining feature modules, or
changing dependencies across features. Detailed MVC, persistence, and
integration conventions remain in their stack references.

## When to use

- Create a new application or make a structural change
- Add a feature that communicates with another feature
- Consider splitting a deployable service or introducing asynchronous
  communication

## When not to use

- A local controller, service, or Mapper change that does not affect module
  boundaries → use the matching stack reference
- PostgreSQL, Redis, SMTP, or HTTP client configuration → use that integration's
  reference

## Deployment topologies

Deployment has two basic shapes: one deployable application or multiple
independently deployable services. A single deployable can use a simple
layered monolith or a modular monolith; this skill defaults to the modular
monolith so feature boundaries are visible without adding distributed
operations. Choose multiple services only when independent deployment,
scaling, ownership, or isolation has a concrete benefit. Avoid a distributed
monolith whose services still require coordinated releases or synchronous
cross-service transactions.

## Rules

1. Default to a **modular monolith**: one Spring Boot application, one
   deployable unit, and packages organized by business feature. It is a
   modular form of a single-deployable monolith, not a separate deployment
   topology. Do not introduce microservices without a concrete deployment,
   scaling, ownership, or isolation requirement.
2. Put feature-owned HTTP, application, and persistence types together under
   the feature package. Keep each feature's public entry points small. Do not
   split the whole project into top-level `controller/`, `service/`, and
   `mapper/` packages.
3. Keep dependencies directed inward: HTTP adapters call application
   services; application services coordinate domain behavior and persistence
   ports/adapters; domain rules do not depend on Spring MVC, MyBatis-Plus,
   Redis, SMTP, or other infrastructure APIs. Use the simplest structure that
   makes an actual boundary clear; do not create interfaces for every class.
4. A feature owns its data and write operations. Another feature should call
   the owning feature's application API rather than update its tables through
   its own Mapper. Share immutable value types only when they represent a
   stable domain concept.
5. Keep transactions in application services around one business operation.
   Do not span network calls or email delivery with a database transaction.
   If a committed change must trigger reliable external work, use an outbox or
   another explicit delivery mechanism when the requirement warrants it.
6. Keep deployment configuration outside domain code. Use Spring adapters
   for infrastructure and typed configuration at application boundaries.
   Local PostgreSQL, Redis, and Mailpit services are development dependencies
   defined in [docker.md](docker.md); they do not imply separate production
   services or a microservice architecture.
7. Before adding a new module, service, event bus, or abstraction, identify the
   concrete constraint it addresses and the boundary it protects. Prefer an
   in-process call when modules share one deployable application and no
   asynchronous or independent deployment requirement exists.
8. New projects include Taikai as a test-scoped architecture check. Read its
   version from `versions.json`, declare it using the selected build tool, and
   keep its rules in normal JUnit test execution.
   Taikai brings ArchUnit core transitively; do not add a separate ArchUnit
   dependency unless the project has a concrete need for ArchUnit's own JUnit
   integration. Keep rules focused on the package and dependency boundaries
   the project actually adopts.
9. Add Spring Modulith only when module verification, isolated module tests,
   module documentation, or event publication registry features are required.
   It complements the default static checks; it does not replace them. Use the
   Spring Modulith BOM and the appropriate test starter, letting the project
   pin a Boot-compatible Modulith version.

## Example package layout

```text
com.example.app
├── order
│   ├── OrderController.java
│   ├── OrderService.java
│   ├── OrderMapper.java
│   ├── Order.java
│   └── CreateOrderRequest.java
└── customer
    ├── CustomerService.java
    ├── CustomerMapper.java
    └── Customer.java
```

Keep the layout flat while each feature is small. Add subpackages such as
`api`, `application`, `domain`, or `persistence` when the feature's size or
dependency rules make the distinction useful. These are internal code
boundaries; they do not create separate deployables.

## Architecture test dependencies

Taikai includes ArchUnit core transitively. Maven example (use the selected
build tool in the project):

```xml
<properties>
	<taikai.version>COPY_FROM_VERSIONS_JSON</taikai.version>
</properties>

<dependencyManagement>
	<dependencies>
		<dependency>
			<groupId>com.enofex</groupId>
			<artifactId>taikai</artifactId>
			<version>${taikai.version}</version>
		</dependency>
	</dependencies>
</dependencyManagement>

<dependency>
	<groupId>com.enofex</groupId>
	<artifactId>taikai</artifactId>
	<scope>test</scope>
</dependency>
```

Add a small JUnit test that invokes the selected Taikai rules against the
application's base package. Add explicit ArchUnit rules when a project has
local boundaries that the Taikai defaults do not express. Use
`spring-modulith-starter-test` only when Spring Modulith is needed; import its
BOM with the selected build tool.

## Verify

Inspect package structure and cross-feature call sites. Confirm the application
has one intended deployable unit, feature data is written through its owner,
domain logic does not depend on infrastructure, and transactions exclude
external network work. For new projects, confirm Taikai is test-scoped and
its rules run with the project's test task.
Report every boundary that cannot be established from the code.
