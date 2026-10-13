package com.example.sb4jpavueproduct;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Sb4JpaVueProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(Sb4JpaVueProductApplication.class, args);
	}

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
