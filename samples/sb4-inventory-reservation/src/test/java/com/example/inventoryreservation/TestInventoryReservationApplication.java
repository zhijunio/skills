package com.example.inventoryreservation;

import org.springframework.boot.SpringApplication;

public class TestInventoryReservationApplication {

	public static void main(String[] args) {
		SpringApplication.from(InventoryReservationApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
