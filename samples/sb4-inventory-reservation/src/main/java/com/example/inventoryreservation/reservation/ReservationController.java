package com.example.inventoryreservation.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/reservations")
class ReservationController {
	private final ReservationService reservations;

	ReservationController(ReservationService reservations) {
		this.reservations = reservations;
	}

	@PostMapping
	ResponseEntity<ReservationService.ReservationView> create(@Valid @RequestBody ReserveRequest request) {
		var reservation = reservations.reserve(request.sku(), request.quantity());
		var location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}")
				.buildAndExpand(reservation.id()).toUri();
		return ResponseEntity.created(location).body(reservation);
	}

	@GetMapping("/{id}")
	ResponseEntity<ReservationService.ReservationView> find(@PathVariable long id) {
		return ResponseEntity.of(reservations.find(id));
	}

	@PostMapping("/{id}/cancel")
	ResponseEntity<Void> cancel(@PathVariable long id) {
		if (!reservations.cancel(id)) return ResponseEntity.notFound().build();
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(ReservationService.InsufficientStock.class)
	ProblemDetail insufficientStock() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Insufficient stock");
	}

	@ExceptionHandler(ReservationService.UnknownSku.class)
	ProblemDetail unknownSku() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Unknown SKU");
	}

	record ReserveRequest(@NotBlank @Size(max = 40) String sku, @Positive int quantity) {}
}
