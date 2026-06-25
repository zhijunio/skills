# Java Architecture Patterns

Use this reference when the target project is Java or JVM-heavy. Java
architecture is usually about module boundaries, dependency direction, explicit
transaction and concurrency ownership, and tests that protect use-case contracts
without turning every class into an interface.

## First Principles

1. Start simple. A small service does not need multi-module Maven, hexagonal
   folders, repository interfaces, or a DI framework on day one.
2. Package names are API design. Callers read `com.example.orders.OrderService`;
   names should reflect domain or capability, not `util` or `common`.
3. Prefer domain or feature packages over mechanical layers. A package should
   explain what it does, not just where it sits in a diagram.
4. Put ports (interfaces) at the boundary that owns the need: application code
   defines `OrderRepository`; infrastructure implements it.
5. Keep framework annotations and transport types out of the domain core when
   invariants matter. Spring/Jakarta types belong at adapters, not inside pure
   domain rules.
6. Threads and async work need owners. Every `Executor`, virtual-thread task,
   reactive pipeline, or scheduled job needs cancellation, shutdown, error
   propagation, and backpressure rules.
7. Exceptions are not control flow for expected business outcomes. Use typed
   domain/application exceptions or result types at boundaries; map to HTTP/RPC
   responses in transport adapters.
8. Use tests to protect contracts. Prefer use-case tests with fakes,
   `@DataJpaTest` / slice tests, and Testcontainers at adapter boundaries over
   deep Mockito hierarchies.

## Pattern Selection

| Project shape | Useful model | Boundary lesson |
| --- | --- | --- |
| Tiny library or CLI | Single module, few packages | Do not scaffold modules before complexity exists. |
| Multiple deployables | Maven/Gradle multi-module or multi-project | Each module exposes a narrow public API; hide internals. |
| Spring Boot HTTP service | domain + application + adapter modules | Controllers stop at transport; domain stays framework-free when possible. |
| API gateway or model router | transport, auth/accounting, routing, provider adapters | Provider quirks stay behind adapters; public schema stays stable. |
| Batch or scheduled jobs | job runner, use cases, adapters | Job scheduling is not business logic; idempotency is explicit. |
| Event-driven service | outbox, consumer, projection, reconciler | Source of truth, ordering, retry, and idempotency are contracts. |
| CQRS / event-sourced domain | command model, event store, projections | Separate write model from read models; replay is testable. |
| Android app | UI layer, ViewModel/use cases, data adapters | Platform lifecycle and UI state ownership are explicit. |
| Shared platform library | published API jar plus internal modules | Versioning and binary compatibility are part of the contract. |
| Reactive service | Reactor pipeline with bounded elastic / virtual threads | Blocking IO and reactive chains do not mix without an explicit boundary. |

## Mature Java Reference Models

Use mature projects as pattern evidence, not directory templates. Always write
both `borrow` and `do_not_copy`.

### Application Composition: Spring Boot

Borrow:
- Use a composition root (`@SpringBootApplication`, `@Configuration`) to wire
  adapters, not to hold business rules.
- Keep auto-configuration as adapter discovery: external systems plug in behind
  interfaces or well-scoped beans.
- Use `@ControllerAdvice`, filters, or handler resolvers to map exceptions to
  HTTP at the transport boundary.
- Slice tests (`@WebMvcTest`, `@DataJpaTest`) validate adapter boundaries without
  booting the whole world.

Do not copy:
- `@Autowired` field injection and `@Component` on every class as default
  architecture.
- Anemic domain models where all logic lives in `@Service` classes.
- `@Transactional` on controllers or domain entities by default.
- Auto-config magic without naming the canonical config source of truth.

Sources:
- https://docs.spring.io/spring-boot/reference/using/structuring-your-code.html
- https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test.html

### Minimal Service Shell: Dropwizard

Borrow:
- Explicit `Application` lifecycle: bootstrap config, register resources,
  health checks, metrics, and managed objects with startup/shutdown.
- Keep Jackson/Jersey types at the HTTP boundary.
- Make operational surfaces (health, metrics) first-class from the start.

Do not copy:
- Jersey-centric layout into Spring MVC projects without reason.
- A metrics/health stack when the product is a library with no runtime.

Sources:
- https://www.dropwizard.io/en/stable/manual/core.html
- https://www.dropwizard.io/en/stable/manual/internals.html

### Ports And Adapters: Hexagonal / Clean Java

Borrow:
- Dependency direction points inward: domain does not depend on Spring, JPA,
  Kafka, or HTTP.
- Application services orchestrate use cases and own transaction boundaries.
- Infrastructure modules implement ports and translate external DTOs/entities.
- MapStruct or explicit mappers keep persistence/transport records out of
  domain entities when invariants would weaken.

Do not copy:
- Mandatory `domain/application/infrastructure` folders for a five-class CRUD
  app.
- A repository interface per table when the use case needs aggregate or
  transaction behavior.
- DDD tactical patterns everywhere when records plus JDBC/MyBatis are clearer.

Sources:
- https://alistair.cockburn.us/hexagonal-architecture/
- https://www.amazon.com/Implementing-Domain-Driven-Design-Vaughn-Vernon/dp/0321834577

### Event Sourcing And CQRS: Axon

Borrow:
- Command handling, event store, sagas, and projections are separate
  boundaries with explicit lifecycle.
- Event handlers stay cheap; heavy work belongs in processors with retry and
  idempotency policy.
- Replay and fixture tests validate projections without a live message bus.

Do not copy:
- Full event sourcing when the product only needs audit logs or outbox delivery.
- Saga complexity for workflows that a state table plus worker can handle.

Sources:
- https://docs.axoniq.io/
- https://developer.axoniq.io/axon-framework/overview

### Stream Processing: Kafka / Kafka Streams

Borrow:
- Producers, consumers, stream topologies, and state stores are adapter
  boundaries with explicit serdes, retry, DLQ, and offset/commit policy.
- Treat topic schemas and consumer contracts as versioned public API.
- Keep stream processing logic free of HTTP and UI concerns.

Do not copy:
- Kafka Streams state stores for simple request/response services.
- Multiple consumer groups updating the same authoritative state without one
  transition owner.

Sources:
- https://kafka.apache.org/documentation/
- https://docs.confluent.io/platform/current/clients/streams.html

### Cloud-Native JVM: Micronaut / Quarkus

Borrow:
- Compile-time DI and explicit bean scopes reduce runtime reflection surprises.
- Native-image constraints make side effects, reflection, and config boundaries
  visible early.
- Health, metrics, and config are adapter concerns with test hooks.

Do not copy:
- Framework migration purely for startup time when Spring boundaries are already
  clear and tests are green.
- GraalVM/native constraints into ordinary server apps without a deployment need.

Sources:
- https://docs.micronaut.io/latest/guide/
- https://quarkus.io/guides/getting-started

### Durable Workflows: Temporal Java SDK

Borrow:
- Workflow code stays deterministic; activities own IO, retries, and provider
  calls.
- Separate workflow definitions, worker runtime, and client stubs.
- Use test workflow environment for replay-friendly contract tests.

Do not copy:
- Temporal for cron-sized jobs that a table plus worker already models cleanly.
- Business logic split across workflow, activity, and controller with no single
  use-case owner.

Sources:
- https://docs.temporal.io/develop/java
- https://docs.temporal.io/develop/java/testing-suite

### Resilience At Boundaries: Resilience4j

Borrow:
- Timeouts, retries, circuit breakers, and bulkheads belong on adapter calls,
  not scattered through domain code.
- Retry policy must pair with idempotency keys or deduplication at the effect
  owner.

Do not copy:
- Retries on non-idempotent POST paths without an explicit billing or command
  contract.
- Circuit breakers that hide user-visible failures without diagnostics.

Sources:
- https://resilience4j.readme.io/docs/getting-started

## Module And Package Layout

Borrow:
- Use Maven multi-module or Gradle multi-project when modules have different
  release cycles, dependency graphs, or test boundaries.
- Common split: `domain` (pure), `application` (use cases), `adapter-*`
  (web, persistence, messaging), `bootstrap` (main/composition root).
- Use Java Platform Module System (`module-info.java`) only when strong
  encapsulation or JPMS deployment requirements exist.
- Keep `internal` or non-exported packages for implementation details even
  without JPMS.

Do not copy:
- `com.company.project.controller/service/repository/dto/entity/util` for every
  feature regardless of size.
- A shared `common` module that becomes a junk drawer.
- Multi-module splits before there is a stable API, separate owner, or compile
  isolation need.

Sources:
- https://maven.apache.org/guides/mini/guide-multiple-modules.html
- https://docs.gradle.org/current/userguide/multi_project_builds.html

## Interfaces, DI, And API Boundaries

Borrow:
- Constructor injection for required dependencies; keep composition in config
  modules or `@Configuration` classes.
- Define port interfaces in the application/domain side; adapters implement them.
- Prefer concrete application services until a second implementation or test fake
  actually needs an interface.
- Use records for immutable commands, queries, and transport DTOs when fit.

Do not copy:
- Interface-per-class reflex for Mockito convenience.
- `@Service` on every class including mappers, validators, and one-off helpers.
- Leaking `Optional` through every layer as a nullable substitute.

Sources:
- https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html

## HTTP, Service, Repository, And Transactions

Borrow:
- Controllers/resources translate HTTP to application commands and map responses.
- Application services own `@Transactional` boundaries when multiple adapter
  operations must commit or roll back together.
- Repositories express aggregate or use-case persistence operations, not only
  generated CRUD when invariants span entities.
- JPA entities, JDBC rows, and JSON DTOs stay in adapter/infrastructure layers
  unless the app is purely infrastructural.
- Map domain/application exceptions to HTTP/RPC at the transport layer.

Do not copy:
- `@Transactional` on every repository method by default.
- Controllers calling repositories directly for non-trivial workflows.
- Entity graphs exposed directly as public API responses without a boundary DTO.

Sources:
- https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html
- https://jakarta.ee/specifications/persistence/

## API Gateways, Model Routers, And Provider Adapters

Borrow:
- Keep public API schema separate from provider-specific request/response shapes.
- Routing, account selection, pricing, and capability policy belong in
  application code, not in controllers or generated clients alone.
- Provider adapters own auth, translation, retry, error classification, and
  telemetry.
- OpenAPI, AsyncAPI, SQL seeds, and generated docs need an explicit
  source-of-truth order.

Do not copy:
- Downstream provider fields leaking into the public request schema.
- Generated client stubs treated as domain models.
- A generic adapter framework before at least two real providers share a
  contract.

## Concurrency, Virtual Threads, And Reactive Code

Borrow:
- Name the owner of each executor, scheduler, or reactive pipeline.
- Use virtual threads for blocking IO when the stack is servlet-style or
  blocking JDBC; do not block inside reactive operators.
- Project Reactor pipelines need explicit rules for subscribe, cancel, error
  propagation, backpressure, and shutdown.
- `@Async` and `@Scheduled` jobs need idempotency, lock ownership, metrics, and
  failure visibility.

Do not copy:
- `parallelStream()` or unbounded thread pools without backpressure and shutdown.
- Mixing blocking JDBC inside `Mono`/`Flux` chains without a bounded scheduler
  boundary.
- Fire-and-forget async without recording failure or retry policy.

Sources:
- https://openjdk.org/jeps/444
- https://projectreactor.io/docs/core/release/reference/

## Workflow, Billing, And Reconciliation

Borrow:
- Model async work as explicit state transitions with idempotency keys, owner,
  deadline/TTL, retry policy, and terminal states.
- Use transactional outbox when message publish or provider calls must survive
  process crashes.
- Keep projection/read-model status separate from execution logs, but define
  which is authoritative for each operator question.
- Reconciliation jobs need bounded scans, lock ownership, metrics, and tests for
  stuck, running, and partial-failure states.

Do not copy:
- Silent state repair without audit trail.
- Billing logic hidden in controllers because HTTP was implemented first.
- Multiple schedulers updating the same state without one transition API owner.

## Errors, Diagnostics, And Observability

Borrow:
- Use domain/application exception types when callers or mappers need to act.
- Wrap infrastructure exceptions at adapter boundaries with operation context.
- Keep user response messages, logs, metrics, and traces separate from low-level
  exception types.
- Use Micrometer/OpenTelemetry at adapter and runtime boundaries with consistent
  naming.

Do not copy:
- Parsing exception messages for business decisions.
- Logging and rethrowing the same exception at every layer.
- Returning raw stack traces or SQL errors to public API clients.

Sources:
- https://micrometer.io/docs
- https://opentelemetry.io/docs/languages/java/

## Testing And Verification

Borrow:
- Unit-test domain and application use cases with fakes/in-memory adapters.
- Use slice tests for web, JPA, and messaging adapters.
- Use Testcontainers for real DB/broker behavior at integration boundaries.
- Test transaction rollback, idempotency, scheduler overlap, and shutdown paths
  for services with async work.

Common verification commands (adapt to the repo):

```bash
mvn -q -DskipTests compile
mvn -q test
./gradlew test
./gradlew check
```

Do not copy:
- `@SpringBootTest` for every test when a narrower slice or pure unit test fits.
- Mockito-only tests that encode implementation details instead of contracts.
- Integration tests as the only proof of domain behavior.

Sources:
- https://docs.spring.io/spring-boot/reference/testing/index.html
- https://www.testcontainers.org/

## Java Design Rules For Architecture Specs

1. State the module dependency graph before drawing layers.
2. Name packages by domain or capability, not generic technical categories.
3. Put ports at the application boundary and keep them small.
4. Keep Spring/Jakarta/HTTP/JPA types out of pure domain code when invariants
   matter.
5. Make transaction ownership explicit at the use-case/application layer.
6. Name thread, scheduler, and reactive pipeline owners with shutdown semantics.
7. Errors should say who can act on them: caller, transport mapper, operator, or
   developer.
8. For existing services, name the source of truth for public contracts,
   workflow state, billing state, generated docs, and SQL/config seeds.
9. Mark compatibility paths as bridge, legacy, or accidental fork; require a
   deletion or convergence condition before adding another path.
10. Avoid new modules until there is a stable concept, separate owner,
    side-effect boundary, or test boundary.
11. Prefer boring, readable Java over framework-shaped architecture.
12. When comparing to Spring examples, always list `do_not_copy` alongside
    `borrow`.
