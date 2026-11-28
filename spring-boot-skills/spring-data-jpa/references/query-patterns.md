# Query patterns

## Repositories and Query Patterns

```java
public interface OrderRepository extends JpaRepository<Order, UUID> {

    boolean existsByCustomerIdAndStatus(UUID customerId, OrderStatus status);

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findById(UUID id);

    @Query("""
        select o
        from Order o
        where o.status = :status
        order by o.createdAt desc, o.id desc
        """)
    List<Order> findRecentByStatus(OrderStatus status, Limit limit);
}
```

Use:

- Derived queries for simple filters.
- `@Query` for explicit joins, keyset pagination, and complex predicates.
- `@EntityGraph` for bounded graph loading.
- Projections for read-only API views.
- `exists...` queries instead of `find...().isPresent()` checks.

Avoid:

- `findAll()` in endpoints.
- Unjustified SQL complexity; native SQL is legitimate when database-specific behavior, query clarity or measured performance warrants it.
- Returning entities for read-only list views when a projection is enough.

## N+1 Prevention

Identify N+1 by looking for lazy association access inside loops or JSON serialization of entities.

```java
@EntityGraph(attributePaths = {"items"}) // OrderItem holds a productId, not a Product association
Optional<Order> findWithItemsById(UUID id);

public interface OrderSummary {
    UUID getId();
    UUID getCustomerId();
    OrderStatus getStatus();
    Instant getCreatedAt();
}

List<OrderSummary> findByStatus(OrderStatus status);
```

Use fetch joins and entity graphs only for bounded relationships. For list endpoints, prefer
projections to avoid loading entire aggregate graphs.

For collection loading on paged lists, prefer a bounded projection, a page of root IDs followed by a bounded association query preserving root order, or another verified fetch strategy. Do not treat collection fetch joins with pagination as safe by default: providers may paginate in memory or reject the query. Inspect generated SQL and counts; preserve totals, tenant predicates and response ordering. See [pagination](pagination.md). An entity graph is not an authorization filter.
