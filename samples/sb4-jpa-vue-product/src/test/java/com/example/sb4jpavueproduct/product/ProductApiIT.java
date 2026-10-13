package com.example.sb4jpavueproduct.product;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.sb4jpavueproduct.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class ProductApiIT {

	@Autowired
	RestTestClient client;

	@Test
	void completesProductCrudOverHttpAndPostgreSql() {
		var sku = "LAMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		var createBody = """
				{"name":"Desk Lamp","sku":"%s","price":29.95,"quantity":8}
				""".formatted(sku);

		var created = client.post().uri("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.body(createBody)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().exists("Location")
				.expectBody(ProductResponse.class)
				.returnResult().getResponseBody();

		assertThat(created).isNotNull();
		assertThat(created.sku()).isEqualTo(sku);
		assertThat(created.price()).isEqualByComparingTo(new BigDecimal("29.95"));

		client.get().uri("/api/products/{id}", created.id())
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.name").isEqualTo("Desk Lamp");

		client.get().uri("/api/products?size=10")
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.content[?(@.sku == '%s')]".formatted(sku)).exists();

		var updateBody = """
				{"name":"Desk Lamp Pro","sku":"%s","price":39.50,"quantity":5}
				""".formatted(sku);
		client.put().uri("/api/products/{id}", created.id())
				.contentType(MediaType.APPLICATION_JSON)
				.body(updateBody)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.name").isEqualTo("Desk Lamp Pro")
				.jsonPath("$.quantity").isEqualTo(5);

		client.delete().uri("/api/products/{id}", created.id())
				.exchange()
				.expectStatus().isNoContent();

		client.get().uri("/api/products/{id}", created.id())
				.exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.expectBody()
				.jsonPath("$.errorCode").isEqualTo("PRODUCT_NOT_FOUND");
	}

	@Test
	void rejectsDuplicateSkuWithConflict() {
		var sku = "DUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		var body = """
				{"name":"First","sku":"%s","price":1.00,"quantity":1}
				""".formatted(sku);
		for (int i = 0; i < 2; i++) {
			var expected = i == 0 ? 201 : 409;
			client.post().uri("/api/products")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.exchange()
					.expectStatus().isEqualTo(expected);
		}
	}
}
