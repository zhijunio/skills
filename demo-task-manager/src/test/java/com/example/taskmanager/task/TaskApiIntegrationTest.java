package com.example.taskmanager.task;

import com.example.taskmanager.TaskManagerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.dao.OptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(classes = TaskManagerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class TaskApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine");

    @Autowired
    RestTestClient http;

    @Autowired
    TaskRepository tasks;

    @Test
    void createsTaskChangesItsStatusAndReadsItBack() {
        TaskResponse created = http.post().uri("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\":\"Prepare demo\",\"description\":\"Show the complete task flow\"}")
                .exchange().expectStatus().isCreated().returnResult(TaskResponse.class).getResponseBody();
        assertThat(created.status()).isEqualTo(TaskStatus.OPEN);
        assertThat(created.version()).isZero();
        http.get().uri("/api/tasks/{id}", created.id()).exchange().expectStatus().isOk();

        TaskResponse inProgress = http.put().uri("/api/tasks/{id}/status", created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"IN_PROGRESS\",\"expectedVersion\":0}")
                .exchange().expectStatus().isOk().returnResult(TaskResponse.class).getResponseBody();
        assertThat(inProgress.version()).isEqualTo(1);
        TaskResponse done = http.put().uri("/api/tasks/{id}/status", created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"DONE\",\"expectedVersion\":1}")
                .exchange().expectStatus().isOk().returnResult(TaskResponse.class).getResponseBody();
        assertThat(done.version()).isEqualTo(2);

        TaskResponse loaded = http.get().uri("/api/tasks/{id}", created.id()).exchange()
                .expectStatus().isOk().returnResult(TaskResponse.class).getResponseBody();
        assertThat(loaded.status()).isEqualTo(TaskStatus.DONE);
        assertThat(loaded.title()).isEqualTo("Prepare demo");
        assertThat(loaded.version()).isEqualTo(2);

        http.get().uri("/api/tasks/{id}/history", created.id()).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$[0].fromStatus").doesNotExist()
                .jsonPath("$[0].toStatus").isEqualTo("OPEN")
                .jsonPath("$[1].fromStatus").isEqualTo("OPEN")
                .jsonPath("$[1].toStatus").isEqualTo("IN_PROGRESS")
                .jsonPath("$[2].fromStatus").isEqualTo("IN_PROGRESS")
                .jsonPath("$[2].toStatus").isEqualTo("DONE");
    }

    @Test
    void rejectsBlankTaskTitleWithProblemDetail() {
        http.post().uri("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\":\"   \"}").exchange().expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.title").isEqualTo("Request validation failed");
    }

    @Test
    void reportsMissingTaskAsProblemDetail() {
        http.get().uri("/api/tasks/{id}", "00000000-0000-0000-0000-000000000001").exchange()
                .expectStatus().isNotFound().expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.title").isEqualTo("Task not found");
    }

    @Test
    void publishesOpenApiDescription() {
        http.get().uri("/v3/api-docs").exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.info.title").isEqualTo("Task Manager API")
                .jsonPath("$.paths['/api/tasks/{id}/status']").exists()
                .jsonPath("$.paths['/api/tasks/{id}/history']").exists();
    }

    @Test
    void rejectsIllegalStatusTransitionWithConflictProblem() {
        TaskResponse created = http.post().uri("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\":\"Lifecycle check\"}").exchange()
                .expectStatus().isCreated().returnResult(TaskResponse.class).getResponseBody();

        http.put().uri("/api/tasks/{id}/status", created.id()).contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"DONE\",\"expectedVersion\":0}")
                .exchange().expectStatus().is4xxClientError()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.title").isEqualTo("Invalid task status transition");
    }

    @Test
    void rejectsStaleTaskVersionAndDoesNotAppendHistory() {
        TaskResponse created = http.post().uri("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\":\"Concurrent update\"}").exchange()
                .expectStatus().isCreated().returnResult(TaskResponse.class).getResponseBody();

        http.put().uri("/api/tasks/{id}/status", created.id()).contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"IN_PROGRESS\",\"expectedVersion\":0}")
                .exchange().expectStatus().isOk();

        http.put().uri("/api/tasks/{id}/status", created.id()).contentType(MediaType.APPLICATION_JSON)
                .body("{\"status\":\"DONE\",\"expectedVersion\":0}")
                .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.type")
                .isEqualTo("https://example.com/problems/stale-task-version");

        http.get().uri("/api/tasks/{id}/history", created.id()).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$[1].toStatus").isEqualTo("IN_PROGRESS")
                .jsonPath("$[2]").doesNotExist();
    }

    @Test
    void databaseOptimisticLockRejectsAWriteThatRacesAfterTheVersionCheck() {
        TaskResponse created = http.post().uri("/api/tasks").contentType(MediaType.APPLICATION_JSON)
                .body("{\"title\":\"Database version check\"}").exchange()
                .expectStatus().isCreated().returnResult(TaskResponse.class).getResponseBody();

        Task firstRead = tasks.findById(created.id()).orElseThrow();
        Task staleRead = tasks.findById(created.id()).orElseThrow();

        try {
            firstRead.changeStatus(TaskStatus.IN_PROGRESS, Instant.now());
            tasks.saveAndFlush(firstRead);

            staleRead.changeStatus(TaskStatus.IN_PROGRESS, Instant.now());
            assertThatThrownBy(() -> tasks.saveAndFlush(staleRead))
                    .isInstanceOf(OptimisticLockingFailureException.class);
        } finally {
            tasks.deleteById(created.id());
        }
    }

    @Test
    void rejectsUnsupportedApiVersion() {
        http.get().uri("/api/tasks").header("API-Version", "2.0").exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void exposesPrometheusMetrics() {
        http.get().uri("/actuator/prometheus").exchange().expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("jvm_memory_used_bytes"));
    }
}
