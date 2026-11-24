package com.example.inventoryreservation.reservation.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	@Modifying
	@Query("update Reservation r set r.cancelled = true where r.id = :id and r.cancelled = false")
	int cancelIfActive(@Param("id") long id);
}
