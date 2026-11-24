package com.example.inventoryreservation.stock;

import java.util.Optional;

import com.example.inventoryreservation.stock.persistence.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {
	private final StockRepository stock;

	public StockService(StockRepository stock) {
		this.stock = stock;
	}

	@Transactional(readOnly = true)
	public Optional<StockView> find(String sku) {
		return stock.findById(sku).map(item -> new StockView(item.sku(), item.available()));
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Allocation allocate(String sku, int quantity) {
		if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
		if (stock.takeIfAvailable(sku, quantity) == 1) return Allocation.ALLOCATED;
		return stock.existsById(sku) ? Allocation.INSUFFICIENT : Allocation.UNKNOWN_SKU;
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public void restore(String sku, int quantity) {
		if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
		if (stock.restore(sku, quantity) != 1) {
			throw new IllegalStateException("Cannot restore stock for missing SKU: " + sku);
		}
	}

	public record StockView(String sku, int available) {}

	public enum Allocation { ALLOCATED, INSUFFICIENT, UNKNOWN_SKU }
}
