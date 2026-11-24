package com.example.inventoryreservation.stock.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<Stock, String> {
	@Modifying
	@Query("""
			update Stock s set s.available = s.available - :quantity
			where s.sku = :sku and s.available >= :quantity
			""")
	int takeIfAvailable(@Param("sku") String sku, @Param("quantity") int quantity);

	@Modifying
	@Query("update Stock s set s.available = s.available + :quantity where s.sku = :sku")
	int restore(@Param("sku") String sku, @Param("quantity") int quantity);
}
