package com.example.sb4jpavueproduct;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class Sb4JpaVueProductApplicationIT {

	@Test
	void contextLoads() {
	}

}
