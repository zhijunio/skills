package com.example.sb4jpavueproduct.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	MockMvcTester mockMvc;

	@MockitoBean
	ProductService products;

	@Test
	void rejectsInvalidProductAndReturnsProblemDetails() {
		assertThat(mockMvc.post().uri("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":" ","sku":"bad sku","price":-1,"quantity":-2}
						"""))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.type").isEqualTo("https://errors.example.com/products/validation");
	}

	@Test
	void rejectsOutOfRangePaginationWithProblemDetails() {
		assertThat(mockMvc.get().uri("/api/products?page=-1&size=0"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.errorCode").isEqualTo("REQUEST_VALIDATION_FAILED");
	}

	@Test
	void createsProductWithLocationHeader() {
		var id = UUID.randomUUID();
		var product = Product.create(id, "Desk Lamp", "LAMP-01", new BigDecimal("29.95"), 8,
				Instant.parse("2026-10-13T00:00:00Z"));
		given(products.create(new ProductRequest("Desk Lamp", "LAMP-01", new BigDecimal("29.95"), 8)))
				.willReturn(product);

		assertThat(mockMvc.post().uri("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Desk Lamp","sku":"LAMP-01","price":29.95,"quantity":8}
						"""))
				.hasStatus(HttpStatus.CREATED)
				.hasHeader("Location", "/api/products/" + id);
	}
}
