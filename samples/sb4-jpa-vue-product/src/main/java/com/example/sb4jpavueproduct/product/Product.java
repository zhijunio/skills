package com.example.sb4jpavueproduct.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product")
class Product {

	@Id
	private UUID id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, length = 32, unique = true)
	private String sku;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private int quantity;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Product() {
	}

	private Product(UUID id, String name, String sku, BigDecimal price, int quantity, Instant now) {
		this.id = Objects.requireNonNull(id);
		this.name = name;
		this.sku = sku;
		this.price = price;
		this.quantity = quantity;
		this.createdAt = now;
		this.updatedAt = now;
	}

	static Product create(UUID id, String name, String sku, BigDecimal price, int quantity, Instant now) {
		return new Product(id, name, sku, price, quantity, now);
	}

	void update(String name, String sku, BigDecimal price, int quantity, Instant now) {
		this.name = name;
		this.sku = sku;
		this.price = price;
		this.quantity = quantity;
		this.updatedAt = now;
	}

	UUID id() { return id; }
	String name() { return name; }
	String sku() { return sku; }
	BigDecimal price() { return price; }
	int quantity() { return quantity; }
	Instant createdAt() { return createdAt; }
	Instant updatedAt() { return updatedAt; }
}
