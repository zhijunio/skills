package com.example.inventoryreservation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class ReservationHttpTest {

	@LocalServerPort
	int port;
	@Autowired
	DataSource dataSource;
	@Autowired
	JdbcTemplate jdbc;

	private final HttpClient http = HttpClient.newHttpClient();

	@Test
	void onlyOneRequestCanReserveTheLastUnit() throws Exception {
		try (http) {
			assertThat(available("LAST-001")).isEqualTo(1);
			var responses = raceWhileHoldingRowLock(reserve("LAST-001", 1),
					"select 1 from stock where sku = ? for update", "LAST-001", "update stock %");
			var left = responses.get(0);
			var right = responses.get(1);
			assertThat(new int[] { left.statusCode(), right.statusCode() }).containsExactlyInAnyOrder(201, 409);

			var success = left.statusCode() == 201 ? left : right;
			var conflict = left.statusCode() == 409 ? left : right;
			String location = success.headers().firstValue("Location").orElseThrow();
			var saved = http.send(HttpRequest.newBuilder(URI.create(location)).timeout(Duration.ofSeconds(5)).build(),
					HttpResponse.BodyHandlers.ofString());
			assertThat(saved.statusCode()).isEqualTo(200);
			assertThat((String) JsonPath.read(saved.body(), "$.sku")).isEqualTo("LAST-001");
			assertThat((Integer) JsonPath.read(saved.body(), "$.quantity")).isEqualTo(1);
			assertThat(conflict.headers().firstValue("Content-Type")).hasValueSatisfying(value ->
					assertThat(value).startsWith("application/problem+json"));
			assertThat((String) JsonPath.read(conflict.body(), "$.detail")).isEqualTo("Insufficient stock");
			assertThat(available("LAST-001")).isZero();
		}
	}

	@Test
	void invalidAndUnknownRequestsDoNotChangeStock() throws Exception {
		try (http) {
			var invalid = http.send(reserve("PEN-100", 0), HttpResponse.BodyHandlers.ofString());
			var unknown = http.send(reserve("UNKNOWN", 1), HttpResponse.BodyHandlers.ofString());
			assertThat(invalid.statusCode()).isEqualTo(400);
			assertThat(unknown.statusCode()).isEqualTo(404);
			assertThat(available("PEN-100")).isEqualTo(5);
		}
	}

	@Test
	void overlappingAndRepeatedCancellationReturnsStockOnlyOnce() throws Exception {
		try (http) {
			jdbc.update("insert into stock (sku, available) values ('CANCEL-RACE', 5)");
			var created = http.send(reserve("CANCEL-RACE", 2), HttpResponse.BodyHandlers.ofString());
			assertThat(created.statusCode()).isEqualTo(201);
			long id = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
			assertThat(available("CANCEL-RACE")).isEqualTo(3);

			var responses = raceWhileHoldingRowLock(cancel(id),
					"select 1 from reservations where id = ? for update", id, "update reservations %");
			assertThat(responses).extracting(HttpResponse::statusCode).containsExactly(204, 204);
			assertThat(http.send(cancel(id), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(204);
			assertThat(http.send(cancel(Long.MAX_VALUE), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(404);
			assertThat(available("CANCEL-RACE")).isEqualTo(5);
			var saved = http.send(HttpRequest.newBuilder(url("/reservations/" + id)).build(),
					HttpResponse.BodyHandlers.ofString());
			assertThat(saved.statusCode()).isEqualTo(200);
			assertThat(saved.body()).isEqualTo(created.body());
		}
	}

	@Test
	void failedReservationInsertRollsBackStockDebit() throws Exception {
		try (http) {
			jdbc.update("insert into stock (sku, available) values ('REJECT-INSERT', 2)");
			jdbc.execute("alter table reservations add constraint test_reject_insert check (sku <> 'REJECT-INSERT')");
			try {
				var failed = http.send(reserve("REJECT-INSERT", 1), HttpResponse.BodyHandlers.discarding());
				assertThat(failed.statusCode()).isEqualTo(500);
				assertThat(available("REJECT-INSERT")).isEqualTo(2);
			}
			finally {
				jdbc.execute("alter table reservations drop constraint test_reject_insert");
			}
		}
	}

	@Test
	void failedStockRestoreRollsBackCancellationAndAllowsRetry() throws Exception {
		try (http) {
			jdbc.update("insert into stock (sku, available) values ('REJECT-RESTORE', 1)");
			var created = http.send(reserve("REJECT-RESTORE", 1), HttpResponse.BodyHandlers.ofString());
			assertThat(created.statusCode()).isEqualTo(201);
			long id = ((Number) JsonPath.read(created.body(), "$.id")).longValue();
			jdbc.execute("alter table stock add constraint test_reject_restore check (sku <> 'REJECT-RESTORE' or available = 0)");
			try {
				assertThat(http.send(cancel(id), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(500);
				assertThat(available("REJECT-RESTORE")).isZero();
				assertThat(jdbc.queryForObject("select cancelled from reservations where id = ?", Boolean.class, id))
						.isFalse();
			}
			finally {
				jdbc.execute("alter table stock drop constraint test_reject_restore");
			}
			assertThat(http.send(cancel(id), HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(204);
			assertThat(available("REJECT-RESTORE")).isEqualTo(1);
		}
	}

	private List<HttpResponse<String>> raceWhileHoldingRowLock(
			HttpRequest request, String lockSql, Object key, String waitingUpdate) throws Exception {
		try (var blocker = dataSource.getConnection(); var lock = blocker.prepareStatement(lockSql)) {
			blocker.setAutoCommit(false);
			lock.setObject(1, key);
			try (var row = lock.executeQuery()) {
				assertThat(row.next()).isTrue();
			}
			var first = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
			var second = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
			try {
				await().atMost(Duration.ofSeconds(8)).until(() -> jdbc.queryForObject("""
						select count(*) from pg_stat_activity
						where datname = current_database() and wait_event_type = 'Lock' and lower(query) like ?
						""", Integer.class, waitingUpdate) >= 2);
			}
			finally {
				blocker.rollback();
			}
			return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
		}
	}

	private int available(String sku) throws Exception {
		var request = HttpRequest.newBuilder(url("/inventory/" + sku)).timeout(Duration.ofSeconds(5)).build();
		var response = http.send(request, HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(200);
		return JsonPath.read(response.body(), "$.available");
	}

	private HttpRequest reserve(String sku, int quantity) {
		String body = """
				{"sku":"%s","quantity":%d}
				""".formatted(sku, quantity);
		return HttpRequest.newBuilder(url("/reservations"))
				.timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();
	}

	private HttpRequest cancel(long id) {
		return HttpRequest.newBuilder(url("/reservations/" + id + "/cancel"))
				.timeout(Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.noBody()).build();
	}

	private URI url(String path) {
		return URI.create("http://localhost:" + port + path);
	}
}
