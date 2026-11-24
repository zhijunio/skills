package com.example.inventoryreservation.reservation;

import java.util.Optional;

import com.example.inventoryreservation.reservation.persistence.Reservation;
import com.example.inventoryreservation.reservation.persistence.ReservationRepository;
import com.example.inventoryreservation.stock.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReservationService {
	private final ReservationRepository reservations;
	private final StockService stock;

	ReservationService(ReservationRepository reservations, StockService stock) {
		this.reservations = reservations;
		this.stock = stock;
	}

	@Transactional
	public ReservationView reserve(String sku, int quantity) {
		return switch (stock.allocate(sku, quantity)) {
			case ALLOCATED -> toView(reservations.save(new Reservation(sku, quantity)));
			case INSUFFICIENT -> throw new InsufficientStock();
			case UNKNOWN_SKU -> throw new UnknownSku();
		};
	}

	@Transactional(readOnly = true)
	public Optional<ReservationView> find(long id) {
		return reservations.findById(id).map(ReservationService::toView);
	}

	@Transactional
	public boolean cancel(long id) {
		var original = reservations.findById(id);
		if (original.isEmpty()) return false;

		if (reservations.cancelIfActive(id) == 1) {
			var reservation = original.get();
			stock.restore(reservation.sku(), reservation.quantity());
		}
		return true;
	}

	private static ReservationView toView(Reservation reservation) {
		return new ReservationView(reservation.id(), reservation.sku(), reservation.quantity());
	}

	record ReservationView(long id, String sku, int quantity) {}

	static class InsufficientStock extends RuntimeException {}
	static class UnknownSku extends RuntimeException {}
}
