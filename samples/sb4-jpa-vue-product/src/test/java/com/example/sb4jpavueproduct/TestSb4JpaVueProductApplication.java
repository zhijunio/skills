package com.example.sb4jpavueproduct;

import org.springframework.boot.SpringApplication;

public class TestSb4JpaVueProductApplication {

	public static void main(String[] args) {
		SpringApplication.from(Sb4JpaVueProductApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
