// Illustrative anti-pattern — HTTP coupling and an exposed persistence model.
// Other choices (ID type, missing-result policy, pagination, factories) depend on the contract.

@Service
public class OrderService {

    @Autowired                                   // field injection instead of constructor
    private OrderRepository orderRepository;

    @Autowired
    private HttpServletRequest httpRequest;       // HTTP concern in service layer

    public ResponseEntity<Order> createOrder(CreateOrderRequest request) {  // returns HTTP type
        String token = httpRequest.getHeader("Authorization");              // reads HTTP headers in service
        if (request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().build();                     // HTTP status logic in service
        }
        Order order = new Order();                                          // construction style is not itself a layering violation
        order.setEmail(request.getEmail());
        order.setStatus("PENDING");                                         // state representation depends on domain contract
        Order saved = orderRepository.save(order);                          // inspect the actual use-case atomicity requirement
        return ResponseEntity.ok(saved);                                    // returns entity directly
    }

    @Transactional
    public Order findById(Long id) {             // preserve the existing ID contract
        return orderRepository.findById(id)
            .orElse(null);                       // preserve documented missing-result semantics
    }

    public List<Order> findAll() {               // consider query bounds and transaction needs
        return orderRepository.findAll();        // unbounded query
    }
}
