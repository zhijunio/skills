package com.example.sb4jpavueproduct.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
		UUID id,
		String name,
		String sku,
		BigDecimal price,
		int quantity,
		Instant createdAt,
		Instant updatedAt) {

	static ProductResponse from(Product product) {
		return new ProductResponse(product.id(), product.name(), product.sku(), product.price(),
				product.quantity(), product.createdAt(), product.updatedAt());
	}
}
