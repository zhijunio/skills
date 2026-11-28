# Persistence testing

Use the project's selected data-access technology; @DataJpaTest is for JPA. Choose a compatible
real database when SQL dialect, constraints, transaction isolation, migrations or locking determine
correctness. A lightweight embedded database may be sufficient for a narrower database-independent
check, but it cannot establish PostgreSQL-specific behavior. Do not introduce JPA to a MyBatis or
JDBC project. For JPA mapping assertions flush and clear the persistence context where appropriate;
for database constraints, force the write and inspect the expected failure at its actual boundary.
A test-managed rollback is not proof that a production application transaction rolled back.
Use independent transactions and bounded synchronization for concurrent tests.

## Slice Tests — Repositories (@DataJpaTest)

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE) // use real DB (Testcontainers)
@Import(TestcontainersConfig.class)
class OrderRepositoryTest {

    @Autowired OrderRepository orderRepository;

    @Test
    void findByStatus_shouldReturnMatchingOrders() {
        // Given
        var order1 = orderRepository.save(Order.create("a@example.com"));
        var order2 = orderRepository.save(Order.create("b@example.com"));
        order2.ship(); // change status
        orderRepository.save(order2);

        // When
        List<Order> pending = orderRepository.findByStatus(OrderStatus.PENDING, Pageable.unpaged()).getContent();

        // Then
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getCustomerEmail()).isEqualTo("a@example.com");
    }
}
```

The fragment assumes an existing application model and a shared disposable database configuration;
see [integration testing](integration-testing.md). Use the actual entity state and expected query
results. Add query-count and pagination assertions when those are the behavior under test.
