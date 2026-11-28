# Entity modeling

These fragments assume the selected Boot 4/Hibernate 7 APIs and illustrative domain types. Verify the actual managed versions. Do not copy annotations or schema choices mechanically. Where migrations own the schema, entity annotations do not replace versioned database constraints or indexes.

## Entity Model Rules

Use an `@Entity` only for persistent state with identity and lifecycle. Use records for DTOs,
commands, and read models. Use `@Embeddable` for values stored inside an entity table.
Compare the [Jakarta entity example](../examples/order-entity.java) with the
[legacy `javax.persistence` example](../examples/javax-persistence-entity.java) when migrating to Boot 4.

```java
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_customer_id", columnList = "customer_id"),
    @Index(name = "idx_orders_status_created", columnList = "status, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Version
    private Long version;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Embedded
    private Money total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Order create(UUID customerId) {
        Order order = new Order();
        order.customerId = Objects.requireNonNull(customerId);
        order.status = OrderStatus.DRAFT;
        order.total = Money.zero("EUR");
        return order;
    }

    public void addItem(UUID productId, int quantity, Money unitPrice) {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot edit submitted order");
        }
        OrderItem item = OrderItem.create(this, productId, quantity, unitPrice);
        Money nextTotal = total.add(item.subtotal());
        items.add(item);
        total = nextTotal;
    }

}
```

Rules:

- Use `jakarta.persistence.*`, never `javax.persistence.*`.
- Keep entities non-final with a protected no-arg constructor so Hibernate can instantiate/proxy them.
- Do not use Java records for ordinary entities. Records are good DTOs and sometimes embeddables.
- If Lombok is already used, prefer targeted annotations over broad generated setters and equality. Explicit Java constructors and methods are equally valid.
- Prefer behavior methods and static factories over public setters/constructors.
- Initialize collections inline. JPA collection fields should not be null.
- Prefer stable explicit enum representations for new mappings; preserve existing ordinal or converter mappings until an approved data migration.
- Consider optimistic locking for concurrent editable state. A nullable wrapper version supports Spring Data new-state detection; preserve existing supported version mappings unless a migration is authorized.
- Preserve existing ID type and generation. For new high-write tables compare pooled sequences, UUIDs and identity using actual database/index/workload requirements; Hibernate identity generation prevents JDBC insert batching for those entities.
- Validate request DTOs at the boundary; enforce entity invariants inside behavior methods.

## Embeddables and DTOs

```java
@Embeddable
public record Money(
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    BigDecimal amount,

    @Column(name = "currency", nullable = false, length = 3)
    String currency
) {
    public Money {
        Objects.requireNonNull(amount);
        Objects.requireNonNull(currency);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch");
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return new Money(amount.multiply(BigDecimal.valueOf(quantity)), currency);
    }
}
```

Prefer DTOs for new public HTTP contracts rather than exposing persistence entities; preserve existing contracts and plan intentional changes. Map within the required data-access scope:

```java
public record OrderResponse(UUID id, String status, BigDecimal total, Instant createdAt) {
    static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getStatus().name(),
            order.getTotal().amount(),
            order.getCreatedAt());
    }
}
```

## equals and hashCode

Do not generate entity equality with Lombok `@Data`. It includes mutable fields and associations,
which can trigger lazy loading, recursion, and hash changes.

Preferred options:

- If the entity has a stable natural key, base equality on that key and enforce a unique database
  constraint.
- If it only has a generated ID, keep default object identity unless the project already has a
  proxy-safe generated-ID pattern.
- Never include collections, mutable fields, or associations in `equals`, `hashCode`, or `toString`.
- Choose a proxy-safe equality strategy consistent with the entity hierarchy. Neither a blanket instanceof nor getClass rule works for every inheritance mapping; test symmetry, transient/persisted identity and hash stability.

```java
@Override
public boolean equals(Object other) {
    if (this == other) return true;
    return other instanceof Customer that
        && getEmail() != null
        && getEmail().equals(that.getEmail());
}

@Override
public int hashCode() {
    return getEmail() == null ? 0 : getEmail().hashCode();
}
```

The natural-key equality example assumes immutable, unique, non-null email and no conflicting entity subtype equality. Do not use email as identity when it can change. Test proxy and collection behavior for the chosen design.
