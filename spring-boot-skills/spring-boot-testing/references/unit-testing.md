# Unit testing

These snippets are illustrative fragments, not a standalone compiled suite. Adapt types and
assertions to the actual domain and service contract. Use valid input when exercising a failure
that occurs after validation. Prefer direct domain tests for invariants; use mocks for service
collaboration and assert absence of writes on denied operations where required.

## Unit Tests — Services

Choose the mix by risk, not a fixed percentage. Use real database/Redis tests for concurrency,
serialization and transaction semantics that mocks cannot establish. Preserve the project's
JUnit/AssertJ and mocking conventions; changing assertion libraries is not a correctness fix.

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private InventoryService inventoryService;

    @InjectMocks private OrderService orderService;

    @Test
    void createOrder_whenItemsAvailable_shouldSaveAndReturnOrder() {
        // Given
        var request = new CreateOrderRequest("user@example.com", List.of(new OrderItemRequest(UUID.randomUUID(), 2)));
        var savedOrder = Order.create("user@example.com");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // When
        Order result = orderService.createOrder(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerEmail()).isEqualTo("user@example.com");
        verify(inventoryService).reserve(request.items());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_whenInventoryUnavailable_shouldThrowException() {
        // Given
        var request = new CreateOrderRequest("user@example.com",
            List.of(new OrderItemRequest(UUID.randomUUID(), 2)));
        doThrow(new InsufficientInventoryException("Out of stock"))
            .when(inventoryService).reserve(any());

        // When / Then
        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(InsufficientInventoryException.class)
            .hasMessage("Out of stock");
        verify(orderRepository, never()).save(any(Order.class));
    }
}
```

Use the project's existing test naming style. Descriptive method names such as
`createOrder_whenItemsAvailable_shouldSaveOrder` are an option, not a required convention.
