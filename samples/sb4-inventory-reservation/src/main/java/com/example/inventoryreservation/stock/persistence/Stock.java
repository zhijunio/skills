package com.example.inventoryreservation.stock.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stock")
public class Stock {

	@Id
	private String sku;
	private int available;

	protected Stock() {}

	public String sku() {
		return sku;
	}

	public int available() {
		return available;
	}
}
