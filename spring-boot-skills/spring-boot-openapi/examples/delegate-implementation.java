// GOOD — implements generated delegate, never modifies generated code, maps between models

@Service
@RequiredArgsConstructor
public class OrderApiDelegateImpl implements OrdersApiDelegate {

    private final OrderService orderService;

    @Override
    public ResponseEntity<OrderDto> getOrderById(UUID id) {
        Order order = orderService.findById(id);
        return ResponseEntity.ok(toDto(order));
    }

    @Override
    public ResponseEntity<OrderDto> createOrder(CreateOrderRequestDto request) {
        Order order = orderService.createOrder(OrderApiMapper.toCommand(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(order.getId()).toUri();
        return ResponseEntity.created(location).body(toDto(order));
    }

    @Override
    public ResponseEntity<PageOrderDto> listOrders(Integer page, Integer size) {
        Page<Order> orders = orderService.findAll(PageRequest.of(page, size));
        PageOrderDto dto = new PageOrderDto()
            .content(orders.getContent().stream().map(this::toDto).toList())
            .page(new PageMetadataDto()
                .size((long) orders.getSize())
                .number((long) orders.getNumber())
                .totalElements(orders.getTotalElements())
                .totalPages((long) orders.getTotalPages()));
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<Void> deleteOrder(UUID id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Map domain → generated DTO (never expose domain model via API)
    private OrderDto toDto(Order order) {
        return new OrderDto()
            .id(order.getId())
            .status(OrderDto.StatusEnum.fromValue(order.getStatus().name()))
            .customerEmail(order.getCustomerEmail())
            .createdAt(order.getCreatedAt());
    }
}
