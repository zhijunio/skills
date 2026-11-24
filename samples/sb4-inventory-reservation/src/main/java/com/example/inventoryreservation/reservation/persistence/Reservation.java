package com.example.inventoryreservation.reservation.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservations")
public class Reservation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String sku;
	private int quantity;
	@Column(nullable = false)
	private boolean cancelled;

	protected Reservation() {}

	public Reservation(String sku, int quantity) {
		this.sku = sku;
		this.quantity = quantity;
	}

	public Long id() { return id; }
	public String sku() { return sku; }
	public int quantity() { return quantity; }
}
