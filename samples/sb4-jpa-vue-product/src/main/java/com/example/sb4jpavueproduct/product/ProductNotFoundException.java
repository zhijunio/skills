package com.example.sb4jpavueproduct.product;

import java.util.UUID;

final class ProductNotFoundException extends RuntimeException {
	ProductNotFoundException(UUID id) {
		super("Product " + id + " was not found");
	}
}
