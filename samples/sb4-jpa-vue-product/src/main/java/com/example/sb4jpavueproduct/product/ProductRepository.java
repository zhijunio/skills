package com.example.sb4jpavueproduct.product;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, UUID> {
	boolean existsBySku(String sku);
	boolean existsBySkuAndIdNot(String sku, UUID id);
}
