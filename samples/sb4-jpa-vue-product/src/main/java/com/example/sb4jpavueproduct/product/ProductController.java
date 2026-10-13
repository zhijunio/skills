package com.example.sb4jpavueproduct.product;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
class ProductController {

	private final ProductService products;

	ProductController(ProductService products) {
		this.products = products;
	}

	@GetMapping
	ProductPageResponse list(
			@RequestParam(defaultValue = "0") @PositiveOrZero int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return ProductPageResponse.from(products.list(page, size));
	}

	@GetMapping("/{id}")
	ProductResponse get(@PathVariable UUID id) {
		return ProductResponse.from(products.get(id));
	}

	@PostMapping
	ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
		var created = products.create(request);
		return ResponseEntity.created(URI.create("/api/products/" + created.id()))
				.body(ProductResponse.from(created));
	}

	@PutMapping("/{id}")
	ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
		return ProductResponse.from(products.update(id, request));
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> delete(@PathVariable UUID id) {
		products.delete(id);
		return ResponseEntity.noContent().build();
	}
}
