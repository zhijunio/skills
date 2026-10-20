package com.example.springbootskillvalidation;

import com.example.springbootskillvalidation.task.TaskResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true", "TASKS_MAX_PAGE_SIZE=75"})
@AutoConfigureRestTestClient
class SpringBootSkillValidationApplicationIT {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void supportsTheTaskLifecycleOverHttp() {
        var created = restTestClient
                .post()
                .uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"title":"Run the integration test","description":"Exercise PostgreSQL"}
                        """)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .exists(HttpHeaders.LOCATION)
                .returnResult(TaskResponse.class)
                .getResponseBody();

        org.assertj.core.api.Assertions.assertThat(created.version()).isZero();

        restTestClient
                .get()
                .uri("/api/tasks/{id}", created.id())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.title")
                .isEqualTo("Run the integration test");

        restTestClient
                .put()
                .uri("/api/tasks/{id}", created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"title":"Run the integration test","description":"Exercise PostgreSQL","completed":true,"version":0}
                        """)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.completed")
                .isEqualTo(true)
                .jsonPath("$.version")
                .isEqualTo(1);

        restTestClient
                .get()
                .uri("/api/tasks?completed=true&q=integration&page=0&size=1")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.content[0].id")
                .isEqualTo(created.id().toString())
                .jsonPath("$.page.totalElements")
                .isEqualTo(1);

        restTestClient
                .get()
                .uri("/api/tasks?size=500")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.page.size")
                .isEqualTo(75);

        restTestClient
                .get()
                .uri("/api/tasks?q=%25")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.page.totalElements")
                .isEqualTo(0);

        restTestClient
                .put()
                .uri("/api/tasks/{id}", created.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"title":"Stale update","description":"Old copy","completed":false,"version":0}
                        """)
                .exchange()
                .expectStatus()
                .isEqualTo(409)
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type")
                .isEqualTo("https://errors.example.com/concurrent-modification");

        restTestClient
                .get()
                .uri("/api/tasks?sort=password,asc")
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);

        restTestClient
                .delete()
                .uri("/api/tasks/{id}", created.id())
                .exchange()
                .expectStatus()
                .isNoContent();

        restTestClient
                .get()
                .uri("/api/tasks/{id}", created.id())
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectHeader()
                .contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void publishesOpenApiContractWhenEnabled() {
        restTestClient
                .get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.info.title")
                .isEqualTo("Task Management API")
                .jsonPath("$.paths['/api/tasks'].get.summary")
                .isEqualTo("Search and page through tasks")
                .jsonPath("$.paths['/api/tasks/{id}'].put.responses['409'].description")
                .isEqualTo("Task version is stale");

        restTestClient
                .get()
                .uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML);
    }

    @Test
    void exposesOnlyMinimalHealthAndProbeInformation() {
        restTestClient
                .get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP")
                .jsonPath("$.components")
                .doesNotExist();

        restTestClient
                .get()
                .uri("/actuator/health/liveness")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP")
                .jsonPath("$.components")
                .doesNotExist();

        restTestClient
                .get()
                .uri("/actuator/health/readiness")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("UP")
                .jsonPath("$.components")
                .doesNotExist();

        for (String endpoint : List.of("env", "configprops", "mappings", "metrics")) {
            restTestClient
                    .get()
                    .uri("/actuator/{endpoint}", endpoint)
                    .exchange()
                    .expectStatus()
                    .isNotFound();
        }
    }
}
