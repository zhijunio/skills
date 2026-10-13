package com.example.sb4jpavueproduct.product;

import java.time.Clock;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class ProductService {

	private final ProductRepository products;
	private final Clock clock;

	ProductService(ProductRepository products, Clock clock) {
		this.products = products;
		this.clock = clock;
	}

	Page<Product> list(int page, int size) {
		var sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
		return products.findAll(PageRequest.of(page, size, sort));
	}

	Product get(UUID id) {
		return products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	@Transactional
	Product create(ProductRequest request) {
		if (products.existsBySku(request.sku())) {
			throw new ProductSkuConflictException(request.sku());
		}
		var now = clock.instant();
		return products.saveAndFlush(Product.create(UUID.randomUUID(), request.name().trim(), request.sku(),
				request.price(), request.quantity(), now));
	}

	@Transactional
	Product update(UUID id, ProductRequest request) {
		var product = get(id);
		if (products.existsBySkuAndIdNot(request.sku(), id)) {
			throw new ProductSkuConflictException(request.sku());
		}
		product.update(request.name().trim(), request.sku(), request.price(), request.quantity(), clock.instant());
		return products.saveAndFlush(product);
	}

	@Transactional
	void delete(UUID id) {
		var product = products.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
		products.delete(product);
	}
}
