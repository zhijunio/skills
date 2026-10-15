# Java in Spring

This guide records Spring-specific Java choices: which application types should be records or classes, how to handle optional values at framework boundaries, and where proxying constrains `final` classes.

## The record/bean decision, made

Choose the type form by the framework contract and apply the decision consistently:

| Kind | Form | Why |
|---|---|---|
| HTTP request / response payload | **record** | Immutable snapshot of what crossed the wire; Jackson binds records natively |
| Command / query / result | **record** | Values passed into and out of a service |
| Domain event | **record** | A fact that happened; must never change |
| Value object, including IDs | **record** | Validate in the compact constructor |
| `@ConfigurationProperties` | **record** | Constructor binding; immutable configuration |
| Projection / read model | **record** or interface | Both supported by Spring Data |
| `@Embeddable` | **record** | Supported by Hibernate 6+; on Jakarta Persistence 3.2 the annotation is optional |
| **`@Entity`** | **class** | Mutable, identity-based, needs a no-arg constructor and non-final fields for proxying. A record cannot be an entity |
| **Spring bean** (`@Service`, `@Component`, `@Configuration`) | **class** | Holds collaborators, must be proxyable |

Use records for immutable data that crosses a boundary, and classes for persistence entities and Spring-managed collaborators, which hold dependencies and may be proxied by the container.

## No Lombok

Write the code. Each annotation has a direct replacement:

| Lombok | Write instead |
|---|---|
| `@RequiredArgsConstructor` | The constructor. See [spring-proxies-and-di.md](../architecture/spring-proxies-and-di.md) |
| `@Getter` on a DTO | A record - the accessors come free |
| `@Getter` on an entity | Explicit accessors, only for the fields callers genuinely need |
| `@Setter` | Nothing. Name the operation: `order.cancel()`, not `order.setStatus(CANCELLED)` |
| `@Data` | Nothing - it generates setters and an `equals` over mutable fields. See [spring-data-jpa.md](../persistence/spring-data-jpa.md) for why that is dangerous on an entity |
| `@Value` (Lombok's) | A record |
| `@Builder` | A static factory for each legitimate construction path; use a builder only when callers genuinely need staged construction |
| `@Slf4j` | `private static final Logger log = LoggerFactory.getLogger(OrderService.class);` |
| `@SneakyThrows` | Handle the exception, or wrap it in a domain exception |
| `@EqualsAndHashCode` | A record, or nothing - see the entity equality rules |

```java
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

Three lines instead of two annotations, and no annotation processor editing the AST.

If the project already uses Lombok, **match the surrounding code and raise it** - a file half-converted is worse than either style. Removing Lombok is its own change, on its own commit.

## Optional at the Spring boundaries

Use `Optional` for public return types that may be empty, not for fields or ordinary method parameters. Spring adds three specifics.

**Repositories return `Optional`.** Spring Data supports it directly:

```java
Optional<Order> findByReference(String reference);

default Order getByReference(String reference) {
    return findByReference(reference)
            .orElseThrow(() -> new OrderNotFoundException(reference));
}
```

The `getBy…` default method beside the `findBy…` is the idiom worth adopting - callers that require the row get an exception mapped to a 404 by [error-handling.md](../web/error-handling.md), and callers that can handle absence get the `Optional`.

**Constructor parameters are the exception.** `Optional<T>` as a constructor parameter is Spring's protocol for an optional dependency - see [spring-proxies-and-di.md](../architecture/spring-proxies-and-di.md). This is the one sanctioned violation.

**Never as a record component of a payload.** Jackson serialises `Optional` awkwardly and it produces a nullable field either way. Use a nullable component and mark it, or model absence with a domain type ([domain modelling](../architecture/domain-modelling.md)).

## var in Spring code

Use `var` where the initializer makes the type clear, and keep explicit types where the framework contract matters:

```java
var order = orderRepository.getById(id);              // obvious
var response = OrderResponse.from(order);             // obvious

// Keep the type when the value is a framework type whose shape matters
ResponseEntity<OrderResponse> entity = ResponseEntity.created(uri).body(response);
```

Never `var` for a `@Bean` method return type - the declared return type *is* the bean's registered type, and inference would change what the container publishes.

For sealed domain states, exhaustive pattern matching, and persistence trade-offs, see [domain modelling](../architecture/domain-modelling.md). Keep API response types explicit; polymorphic JSON needs deliberate Jackson configuration ([JSON and Jackson](../web/json-and-jackson.md)).

## Immutability and `final`

Prefer `final` classes where possible. Spring imposes two exclusions:

| Type | `final`? |
|---|---|
| DTO, value object, event, command (records) | Final by definition |
| `@Entity` | **No** - Hibernate subclasses it to proxy |
| Advised Spring bean | **No** - CGLIB subclasses it. See [spring-proxies-and-di.md](../architecture/spring-proxies-and-di.md) |
| Everything else | Yes |

Bean *fields* stay `final` regardless - constructor injection makes that free.

## Gotchas

- Agent makes a JPA entity a record - impossible; entities need a no-arg constructor and mutable, non-final fields
- Agent makes a DTO a class with getters and setters - use a record
- Agent generates any Lombok annotation - see the replacement table
- Agent hand-writes `equals`/`hashCode` on a DTO - use a record; on an entity, see [spring-data-jpa.md](../persistence/spring-data-jpa.md)
- Agent marks an `@Entity` or an advised `@Service` `final` - breaks proxying
- Agent uses `var` for a `@Bean` method return type - the declared type is the registered bean type
- Agent puts `Optional` in a request or response record - Jackson handles it poorly; use a nullable component
- Agent returns `null` from a repository-backed lookup - return `Optional`, with a `getBy…` default that throws
- Agent uses sealed types for a JPA entity hierarchy - Hibernate needs entities it can proxy and subclass

## Related

- [spring-proxies-and-di.md](../architecture/spring-proxies-and-di.md) · [spring-data-jpa.md](../persistence/spring-data-jpa.md) · [database-schema-and-sql.md](../persistence/database-schema-and-sql.md) · [json-and-jackson.md](../web/json-and-jackson.md) · [validation.md](../web/validation.md) · [domain-modelling.md](../architecture/domain-modelling.md)

## References

- [Java SE 25 Documentation](https://docs.oracle.com/en/java/javase/25/)
- [Spring Framework Reference](https://docs.spring.io/spring-framework/reference/)
