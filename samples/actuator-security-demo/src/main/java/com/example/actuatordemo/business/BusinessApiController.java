package com.example.actuatordemo.business;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class BusinessApiController {

    @GetMapping("/public/status")
    Map<String, String> publicStatus() {
        return Map.of("status", "Business API is available");
    }

    @GetMapping("/orders")
    List<OrderSummary> orders() {
        return List.of(new OrderSummary("ORD-1001", "待处理", 2), new OrderSummary("ORD-1002", "已发货", 1));
    }

    record OrderSummary(String id, String status, int itemCount) {}
}
