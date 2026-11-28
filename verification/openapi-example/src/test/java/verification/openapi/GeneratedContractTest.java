package verification.openapi;

import com.example.api.OrdersApiDelegate;
import com.example.api.OrdersApiController;
import com.example.api.model.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedContractTest {
    // Compile-time checks: generated signatures must match the intended delegate.
    static final class Delegate implements OrdersApiDelegate {
        @Override public ResponseEntity<OrderDto> getOrderById(UUID id) {
            return ResponseEntity.ok(new OrderDto().id(id));
        }
        @Override public ResponseEntity<OrderDto> createOrder(CreateOrderRequestDto request) {
            return ResponseEntity.status(201).build();
        }
        @Override public ResponseEntity<PageOrderDto> listOrders(Integer page, Integer size) {
            return ResponseEntity.ok(new PageOrderDto().page(new PageMetadataDto()
                .number(page.longValue()).size(size.longValue()).totalElements(0L).totalPages(0L)));
        }
        @Override public ResponseEntity<Void> deleteOrder(UUID id) {
            return ResponseEntity.noContent().build();
        }
    }

    @Test void generatedControllerAcceptsDelegate() {
        var delegate = new Delegate();
        assertSame(delegate, new OrdersApiController(delegate).getDelegate());
    }

    @Test void monetaryValueRemainsDecimal() {
        BigDecimal price = new BigDecimal("123.45");
        assertEquals(price, new OrderItemDto().unitPrice(price).getUnitPrice());
    }

    @Test void delegateOverridesAllOperations() {
        var delegate = new Delegate();
        UUID id = UUID.randomUUID();
        assertEquals(id, delegate.getOrderById(id).getBody().getId());
        assertEquals(201, delegate.createOrder(new CreateOrderRequestDto()).getStatusCode().value());
        assertEquals(0L, delegate.listOrders(0, 20).getBody().getPage().getTotalElements());
        assertEquals(204, delegate.deleteOrder(id).getStatusCode().value());
    }
}
