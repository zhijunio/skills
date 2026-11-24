package com.example.inventoryreservation.reservation;

import com.example.inventoryreservation.stock.persistence.StockRepository;

public class DirectStockAccessFixture {
	public boolean stockExists(StockRepository stock, String sku) {
		return stock.existsById(sku);
	}
}
