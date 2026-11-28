// Anti-pattern fragment: entity caching and missing invalidation/expiry policy.
// Assumes caching is enabled elsewhere when discussing cache effects.

// If caching is not enabled, annotations do not establish caching behavior.

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Cacheable("orders")                              // default key is valid for a single-scope ID
    public Order findById(UUID id) {
        return orderRepository.findById(id)
            .orElseThrow();                           // caches JPA entity with lazy fields
                                                      // lazy state/serialization failures are possible
    }

    @Cacheable("orders")
    public Order findByIdNullable(UUID id) {
        return orderRepository.findById(id)
            .orElse(null);                            // null behavior depends on cache configuration
    }

    // No invalidation on update — stale data may persist
    public Order update(UUID id, UpdateOrderRequest request) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.update(request);
        return orderRepository.save(order);           // cache still has old version
    }

    // No expiry or capacity policy can allow unbounded growth
    // Native Java serialization has compatibility and deserialization-security risks
}
