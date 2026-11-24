package com.example.inventoryreservation.stock;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class StockController {
	private final StockService stock;

	StockController(StockService stock) {
		this.stock = stock;
	}

	@GetMapping("/inventory/{sku}")
	ResponseEntity<StockService.StockView> find(@PathVariable String sku) {
		return ResponseEntity.of(stock.find(sku));
	}
}
