# Pagination

## Pagination

Use `Pageable` for normal list screens:

```java
Page<Order> findByStatus(OrderStatus status, Pageable pageable);
```

Consider keyset pagination for deep or infinite-scroll lists when the API can support cursor semantics. Preserve existing offset contracts unless a change is approved. Large offsets may require scanning and discarding rows; measure on the actual database.

```java
@Query("""
    select o
    from Order o
    where o.status = :status
      and (o.createdAt < :lastCreatedAt
           or (o.createdAt = :lastCreatedAt and o.id < :lastId))
    order by o.createdAt desc, o.id desc
    """)
List<Order> findNextPage(OrderStatus status, Instant lastCreatedAt, UUID lastId, Limit limit);
```

The `(createdAt, id)` tuple keeps the cursor stable when timestamps collide. Back it with an index
like `(status, created_at desc, id desc)`.

The UUID comparison in this illustrative JPQL cursor requires provider/database support and a consistent ordering. Verify the actual generated SQL and use a compatible tie-breaker if necessary. Define first-page behavior, null ordering, sort direction, cursor validation and filter/tenant binding; use a stable unique tie-breaker. A cursor does not authorize access. Test equal timestamps, page boundaries, missing/duplicate rows and concurrent modifications according to the contract; keyset pagination is not a snapshot guarantee.
