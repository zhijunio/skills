package com.example.orders;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(LegacyOrderMigrationIT.LegacyDatabase.class)
class LegacyOrderMigrationIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void upgradesOldOrdersAndPreservesTheirHttpRepresentation() throws Exception {
		var oldOrders = Map.of(1L, "WEB-legacy", 2L, "DIRECT-legacy", 3L, "web-legacy",
				4L, "XWEB-legacy", 5L, "WEB-");
		assertThat(jdbc.queryForObject("select count(*) from orders where id <= 5", Integer.class)).isEqualTo(5);
		for (var order : oldOrders.entrySet()) {
			long id = order.getKey();
			String number = order.getValue();
			mvc.perform(get("/orders/{id}", id)).andExpect(status().isOk())
					.andExpect(content().json(orderJson(id, number), JsonCompareMode.STRICT));
			assertSource(id, number.startsWith("WEB-") ? "WEB" : "OTHER");
		}
	}

	@Test
	void newOrdersKeepTheHttpContractAndPersistBothSources() throws Exception {
		for (var order : Map.of("WEB-new", "WEB", "web-new", "OTHER", "DIRECT-new", "OTHER").entrySet()) {
			String number = order.getKey();
			var created = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
					.content(requestJson(number))).andExpect(status().isCreated())
					.andExpect(header().exists(HttpHeaders.LOCATION))
					.andReturn();
			String location = created.getResponse().getHeader(HttpHeaders.LOCATION);
			long id = jdbc.queryForObject("select id from orders where order_number = ?", Long.class, number);
			assertThat(location).endsWith("/orders/" + id);
			content().json(orderJson(id, number), JsonCompareMode.STRICT)
					.match(created);
			mvc.perform(get(location)).andExpect(status().isOk())
					.andExpect(content().json(orderJson(id, number), JsonCompareMode.STRICT));
			assertSource(id, order.getValue());
		}
	}

	@Test
	void legacyOrderNumbersStillConflictAndInvalidInputsStillFail() throws Exception {
		mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(requestJson("WEB-legacy")))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.detail").value("Order number already exists"));
		assertThat(jdbc.queryForObject("select count(*) from orders where order_number = 'WEB-legacy'", Integer.class))
				.isEqualTo(1);
		mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(requestJson(" ")))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/orders/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound());
	}

	@Test
	void databaseRejectsExplicitNullSource() {
		assertThat(jdbc.queryForObject("""
				select is_nullable from information_schema.columns
				where table_schema = 'public' and table_name = 'orders' and column_name = 'order_source'
				""", String.class)).isEqualTo("NO");
		assertThatThrownBy(() -> jdbc.update(
				"insert into orders (order_number, order_source) values ('NULL-source-probe', null)"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.rootCause().isInstanceOfSatisfying(SQLException.class, error ->
						assertThat(error.getSQLState()).isEqualTo("23502"));
	}

	private void assertSource(long id, String expected) {
		assertThat(jdbc.queryForObject("select order_source from orders where id = ?", String.class, id))
				.isEqualTo(expected);
	}

	private static String requestJson(String number) {
		return "{\"orderNumber\":\"%s\"}".formatted(number);
	}

	private static String orderJson(long id, String number) {
		return "{\"id\":%d,\"orderNumber\":\"%s\"}".formatted(id, number);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class LegacyDatabase {

		@Bean
		@ServiceConnection
		PostgreSQLContainer legacyPostgres() throws SQLException {
			var postgres = new PostgreSQLContainer("postgres:18-alpine");
			postgres.start();
			try {
				Flyway.configure()
						.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
						.locations("classpath:legacy-migrations").target("1").load().migrate();
				try (var connection = DriverManager.getConnection(
						postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
						var statement = connection.createStatement()) {
					statement.executeUpdate("""
							insert into orders (order_number)
							values ('WEB-legacy'), ('DIRECT-legacy'), ('web-legacy'), ('XWEB-legacy'), ('WEB-')
							""");
				}
				return postgres;
			}
			catch (RuntimeException | SQLException error) {
				postgres.stop();
				throw error;
			}
		}
	}
}
