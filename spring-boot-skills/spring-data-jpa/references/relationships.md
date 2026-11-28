# Relationships

## Relationships

Map the database shape first. Prefer normal foreign keys: `@ManyToOne` on the owning side and
`@OneToMany(mappedBy = ...)` only when parent-to-child navigation is actually needed.

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_item_order"))
    private Order order;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    private int quantity;
    @Embedded
    private Money unitPrice;

    static OrderItem create(Order order, UUID productId, int quantity, Money unitPrice) {
        OrderItem item = new OrderItem();
        item.order = Objects.requireNonNull(order);
        item.productId = Objects.requireNonNull(productId);
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        item.quantity = quantity;
        item.unitPrice = Objects.requireNonNull(unitPrice);
        return item;
    }

    Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
```

- Prefer deliberate lazy to-one mappings when compatible with the chosen provider and access pattern; JPA LAZY for to-one is a hint, and actual behavior can depend on mapping and enhancement. Measure the SQL rather than assuming it.
- Avoid unbounded bidirectional graphs. Add back-references only when required.
- Use `orphanRemoval = true` only when the parent truly owns the child's lifecycle.
- Avoid `@ManyToMany` for business relationships with attributes; model the join row as an entity.
- Do not serialize lazy relationships to JSON. Map to DTOs inside a transaction.

Synchronize both sides of bidirectional associations when the domain requires them. Cascade and orphan removal affect persistence lifecycle, not just navigation. Test deletion, reassignment, ownership and rollback against actual constraints. See [query patterns](query-patterns.md) for targeted fetch plans.
