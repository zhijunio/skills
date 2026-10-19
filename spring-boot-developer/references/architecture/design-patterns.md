# Design Patterns for Spring Applications

Use this guide to choose a pattern for a concrete change point, transaction boundary, or module boundary. A pattern is not a goal: start with a direct method or service, then introduce indirection when a real variation or reliability requirement justifies it. Preserve the project's existing design.

## Quick selection

| Problem | Starting point | Read next |
| --- | --- | --- |
| A behavior has a few interchangeable implementations | Strategy with Spring-managed implementations | [Strategy](#strategy) |
| An external SDK or legacy API differs from the application's model | Adapter at the integration boundary | [Adapter](#adapter-and-anti-corruption-layer) and [HTTP clients](../integration/http-clients.md) |
| One module needs to notify another inside the same process | Spring application event, if direct orchestration would create unwanted coupling | [Application events](#application-events) and [Spring Modulith](spring-modulith.md) |
| A database change and broker publication must survive process failure together | Transactional outbox | [Outbox](#outbox-and-reliable-publication) and [Messaging](../integration/messaging.md) |
| A business process spans services and needs compensating actions | Saga only if the process is long-running and cannot fit one local transaction | [Saga](#saga-for-multi-service-workflows) and [Transactions](../persistence/transactions.md) |
| Reads need a different model from writes | Start with projections; use full CQRS only for demonstrated scaling or model needs | [CQRS](#cqrs-and-read-models) and [JPA projections](../persistence/spring-data-jpa.md#projections) |
| A feature needs stronger compile-time or runtime module boundaries | Feature packages first; consider ports/adapters or Spring Modulith as the boundary grows | [Code organization](code-organization.md), [Hexagonal architecture](hexagonal-architecture.md), [Spring Modulith](spring-modulith.md) |

## Strategy

Use Strategy when the same use case selects among behaviors by an explicit business dimension, such as payment method, pricing rule, or document format. Keep selection in one place and make the supported set explicit.

```java
public interface PaymentProcessor {
    PaymentMethod method();
    Receipt charge(PaymentRequest request);
}

@Component
class PaymentProcessors {

    private final Map<PaymentMethod, PaymentProcessor> byMethod;

    PaymentProcessors(List<PaymentProcessor> processors) {
        this.byMethod = processors.stream().collect(Collectors.toUnmodifiableMap(
                PaymentProcessor::method,
                Function.identity()));
    }

    PaymentProcessor require(PaymentMethod method) {
        var processor = byMethod.get(method);
        if (processor == null) {
            throw new UnsupportedPaymentMethodException(method);
        }
        return processor;
    }
}
```

Spring injects the implementations; no manual construction or `ApplicationContext` lookup is needed. Duplicate keys fail during map construction, which exposes an invalid configuration instead of choosing an arbitrary implementation. Add a strategy interface only when implementations really vary; one implementation behind an interface used nowhere else adds indirection without a seam.

## Factory

Use a factory when object construction has meaningful branching or validation, not merely to wrap `new`. A Spring factory can receive its dependencies through the constructor and return the selected implementation. Keep it distinct from Strategy:

- **Strategy** chooses behavior for a use case.
- **Factory** controls how an object or implementation is created.

For ordinary Spring components, let dependency injection create and manage them. Avoid static factories that reach into the application context, service-locator helpers, and reflection-based class names from request input. If a request selects an implementation, map a validated enum or allowlisted key rather than accepting a class name.

## Adapter and anti-corruption layer

Use an adapter to translate an external API, SDK, or legacy model into the application's own types. Keep vendor-specific exceptions, wire DTOs, and retry details at the integration boundary; application services should depend on a small port or client contract that expresses the needed operation.

Do not create an adapter around every class. It earns its cost when it isolates an external dependency, enables a meaningful test seam, or prevents a foreign model from spreading through the domain. See [Hexagonal architecture](hexagonal-architecture.md), [HTTP clients](../integration/http-clients.md), and [Anti-corruption layer](domain-modelling.md#anti-corruption-layer).

## Application events

Spring application events can decouple modules inside one process. Publish an immutable event after the source module completes a meaningful state transition; keep the event payload small and independent of JPA entities. See [Spring Modulith events](spring-modulith.md#events-the-way-modules-talk).

Treat ordinary Spring events as in-process coordination, not as a durable message broker. Event listeners run synchronously by default, so their work and failures can affect the publishing call and its transaction. `@TransactionalEventListener` can bind handling to a transaction phase such as `AFTER_COMMIT`, but it does not by itself close the crash window between committing the database and delivering the event. If losing the event in that window is unacceptable, use an outbox or a durable publication registry. Do not add `@Async` just to hide slow work; define its executor, failure handling, and delivery guarantees first.

## Outbox and reliable publication

Use a transactional outbox when a database commit and event publication must be coordinated: write both the business state and an outbox record in the same database transaction, then publish the record asynchronously. Expect at-least-once delivery; consumers must be idempotent, and the relay needs retry, observability, and retention behavior. Do not publish to a broker inside an open database transaction and assume the two systems commit atomically.

Use Spring Modulith's event publication support when it fits the application's module and persistence setup; otherwise implement the outbox deliberately. Do not add a broker or outbox for a local notification that can safely be retried or recomputed. See [transactional outbox](../integration/messaging.md#the-transactional-outbox) and [event publication](spring-modulith.md#events-the-way-modules-talk).

## Saga for multi-service workflows

Use a Saga when a business operation spans multiple services, lasts beyond a short request, and cannot use one local database transaction. Model each step and its compensating action explicitly, make commands idempotent, and persist enough progress to resume or reconcile after a crash. Compensation is a new business action, not a database rollback; it can fail and needs its own retry and operator-visible state.

Do not introduce a Saga for work within one service and one database. A local transaction is simpler and provides stronger atomicity. See [multi-service operations](../persistence/transactions.md#multi-service-operations).

## CQRS and read models

CQRS separates the command model from one or more query models. Start with a DTO projection or a purpose-built read query inside the same application and database; this often solves the performance or response-shape problem without a second persistence model.

Separate read stores, asynchronous projections, or independent scaling only when measured workload or domain complexity justifies eventual consistency and the additional reconciliation burden. Document freshness expectations and how projections are rebuilt. See [JPA projections](../persistence/spring-data-jpa.md#projections), [domain commands and queries](domain-modelling.md#commands-queries-and-results), and [Spring Modulith](spring-modulith.md).

## Selection checklist

Before adding a pattern, answer:

1. What concrete variation, failure mode, or boundary does it address?
2. What simpler design was considered, and why is it insufficient?
3. Who owns retries, duplicate delivery, compensation, and recovery if they apply?
4. Which test will prove both the expected path and the relevant failure path?

If these answers are unclear, keep the design direct and revisit it when the requirement appears.

## Related

- [Code organization](code-organization.md) · [Domain modelling](domain-modelling.md) · [Hexagonal architecture](hexagonal-architecture.md) · [Spring Modulith](spring-modulith.md) · [Transactions](../persistence/transactions.md) · [Messaging](../integration/messaging.md) · [Resilience](../integration/resilience.md)

## References

- [Spring Framework: Application Events](https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events)
- [Spring Framework: Transaction-bound Events](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
- [Spring Modulith: Working with Application Events](https://docs.spring.io/spring-modulith/reference/events.html)
