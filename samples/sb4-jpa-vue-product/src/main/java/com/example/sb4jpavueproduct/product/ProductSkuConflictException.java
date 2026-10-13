package com.example.sb4jpavueproduct.product;

final class ProductSkuConflictException extends RuntimeException {
	ProductSkuConflictException(String sku) {
		super("SKU " + sku + " is already in use");
	}
}
