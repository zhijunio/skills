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
2. Put feature-owned HTTP, application, and persistence types under the
   business feature package. When a feature has both HTTP and persistence
   adapters, separate its `api`, `application`, and `persistence` packages as
   shown below; put framework-independent business rules in `domain` when the
   feature has meaningful domain behavior. Do not place controllers, services,
   and mappers together in one flat package for a feature that crosses these
   boundaries. A flat feature package is appropriate only while it has a
   single small boundary and the flat layout does not mix adapters and
   persistence. Do not split the whole project into top-level `controller/`,
   `service/`, and `mapper/` packages.
3. Place application-wide Spring `@Configuration` classes in a top-level
   `config` package below the application base package (for example,
   `com.example.app.config`). Put feature-specific configuration inside that
   feature package, preferably in its `config` subpackage. Keep configuration
   out of `domain`. In a regular application, do not call this package
   `autoconfigure`: reserve `@AutoConfiguration` and the `autoconfigure`
   package for reusable Boot starters, and register those classes through
   `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
   Keep the `@SpringBootApplication` bootstrap class at the base package so
   component scanning covers the application. Put cross-application wiring
   such as `@MapperScan` in the relevant root `config` class, not in the
   bootstrap class or an unrelated feature's persistence package. Keep the
   bootstrap class limited to `@SpringBootApplication`; place feature-enabling
   annotations such as `@EnableCaching` on purpose-specific configuration
   classes in `config`.
4. Keep dependencies directed inward: HTTP adapters call application
   services; application services coordinate domain behavior and persistence
   ports/adapters; domain rules do not depend on Spring MVC, MyBatis-Plus,
   Redis, SMTP, or other infrastructure APIs. Use the simplest structure that
   makes an actual boundary clear; do not create interfaces for every class.
5. A feature owns its data and write operations. Another feature should call
   the owning feature's application API rather than update its tables through
   its own Mapper. Share immutable value types only when they represent a
   stable domain concept.
6. Keep transactions in application services around one business operation.
   Do not span network calls or email delivery with a database transaction.
   If a committed change must trigger reliable external work, use an outbox or
   another explicit delivery mechanism when the requirement warrants it.
7. Keep deployment configuration outside domain code. Use Spring adapters
   for infrastructure and typed configuration at application boundaries.
   Local PostgreSQL, Redis, and Mailpit services are development dependencies
   defined in [docker.md](docker.md); they do not imply separate production
   services or a microservice architecture.
8. Before adding a new module, service, event bus, or abstraction, identify the
   concrete constraint it addresses and the boundary it protects. Prefer an
   in-process call when modules share one deployable application and no
   asynchronous or independent deployment requirement exists.
   For lightweight in-process notifications, use Spring application events
   and follow the transaction and durability rules in [async.md](async.md).
9. New projects include Taikai as a test-scoped architecture check. Read its
   version from `versions.json`, declare it using the selected build tool, and
   keep its rules in normal JUnit test execution.
   Taikai brings ArchUnit core transitively; do not add a separate ArchUnit
   dependency unless the project has a concrete need for ArchUnit's own JUnit
   integration. For features using the `api` / `application` / `persistence`
   layout, add focused ArchUnit rules to the existing architecture test so
   controllers cannot depend directly on persistence and domain classes cannot
   depend on application or infrastructure packages. Keep rules focused on
   the package and dependency boundaries the project actually adopts.
10. Add Spring Modulith only when module verification, isolated module tests,
   module documentation, or event publication registry features are required.
   It complements the default static checks; it does not replace them. Use the
   Spring Modulith BOM and the appropriate test starter, letting the project
   pin a Boot-compatible Modulith version.

## Example package layout

```text
com.example.app
├── AppApplication.java
├── config                         # Application-wide @Configuration
│   ├── CacheConfiguration.java
│   └── OpenApiConfiguration.java
├── order                          # Feature boundary
│   ├── api                         # HTTP adapters and request/response DTOs
│   │   ├── OrderController.java
│   │   ├── CreateOrderRequest.java
│   │   └── OrderResponse.java
│   ├── application                 # Use cases and transaction boundary
│   │   └── OrderService.java
│   ├── domain                      # Framework-independent domain rules
│   │   └── Order.java
│   └── persistence                 # MyBatis mapper, record, feature DB config
│       ├── OrderMapper.java
│       └── OrderEntity.java
└── customer
    ├── api
    ├── application
    ├── domain
    └── persistence
```

Use the shown feature subpackages for a feature that crosses HTTP,
application, and persistence boundaries. Do not create an empty `domain`
package when there are no domain rules. Application-wide configuration remains
in the root `config`; feature-owned configuration stays inside its feature.
These are internal code boundaries; they do not create separate deployables.
Keep REST DTOs in the feature's `api` package, never in a global `dto` package.
The runnable Maven counterpart is in
[`assets/template-maven/src/main/java/com/example/templatemaven`](../assets/template-maven/src/main/java/com/example/templatemaven).
Its [`ArchitectureTests`](../assets/template-maven/src/test/java/com/example/templatemaven/ArchitectureTests.java)
runs Taikai's Spring rules, checks the API-to-persistence and
domain-to-infrastructure boundaries with ArchUnit, and proves the API rule
rejects a known illegal dependency using a test-only fixture at
[`IllegalApiFixture`](../assets/template-maven/src/test/java/com/example/templatemaven/architecturefixture/api/IllegalApiFixture.java).

## Naming and project documentation

For new features, use stable role-based names: `{Feature}Controller`,
`{Feature}Service`, `{Feature}Mapper`, `{Feature}Entity`,
`Create{Feature}Request`, `Update{Feature}Request`, and `{Feature}Response`.
Use a Java record for immutable request and response DTOs unless generated
property documentation requires a JavaBean. Keep one public top-level Java
type per file. Do not add `Impl` suffixes or duplicate the role in package and
class names (for example, avoid `task.TaskServiceImpl`).

For a new runnable application, give its README the same section order:

1. Purpose and selected stack
2. Prerequisites
3. Local startup and shutdown
4. Authentication or explicit access boundary
5. API examples and documentation URLs, when applicable
6. Configuration variables, defaults, and secret handling
7. Build, test, formatting, and generated report commands

Omit sections that do not apply. Keep examples aligned with checked-in
configuration and do not claim runtime behavior that has not been verified.

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

For a new project or structural change, inspect package structure and
cross-feature call sites. Confirm application-wide `@Configuration` classes
are in root `config`, feature configuration is feature-local, and reusable
starter `@AutoConfiguration` is not confused with ordinary app configuration.
Confirm features that cross HTTP and persistence boundaries use the documented
feature subpackages, and their architecture test rejects controller-to-mapper
and domain-to-infrastructure dependencies. Confirm feature data is written
through its owner, domain logic does not depend on infrastructure, and
transactions exclude external network work. For new projects, confirm Taikai
is test-scoped and its rules run with the project's test task. For a local
feature change, inspect only the affected feature boundaries. Report
boundaries that cannot be established from the code.
