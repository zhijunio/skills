package com.example.sb4jpavueproduct.product;

import java.util.List;

import org.springframework.data.domain.Page;

public record ProductPageResponse(
		List<ProductResponse> content,
		int page,
		int size,
		long totalElements,
		int totalPages) {

	static ProductPageResponse from(Page<Product> products) {
		return new ProductPageResponse(products.map(ProductResponse::from).getContent(),
				products.getNumber(), products.getSize(), products.getTotalElements(), products.getTotalPages());
	}
}
