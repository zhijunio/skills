package com.example.sb4jpatask.task;

import com.example.sb4jpatask.config.GlobalExceptionHandler;
import com.example.sb4jpatask.config.ApiVersionConfig;
import com.example.sb4jpatask.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(properties = {
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://identity.example.test/issuer",
        "spring.security.oauth2.resourceserver.jwt.audiences=https://api.example.test"
}, controllers = TaskController.class)
@Import({GlobalExceptionHandler.class, TaskExceptionHandler.class, ApiVersionConfig.class, SecurityConfig.class})
class TaskControllerWebTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    TaskService tasks;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    @WithMockUser(authorities = "SCOPE_tasks:write")
    void createsTaskAndReturnsItsJsonContract() {
        UUID id = UUID.randomUUID();
        given(tasks.create(new CreateTaskRequest("Write docs", "Document API")))
                .willReturn(new TaskResponse(id, "Write docs", "Document API", TaskStatus.OPEN,
                        Instant.parse("2026-10-11T00:00:00Z"), Instant.parse("2026-10-11T00:00:00Z"), 0));

        assertThat(mvc.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Write docs\",\"description\":\"Document API\"}"))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.id").isEqualTo(id.toString());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_tasks:write")
    void rejectsBlankTitleBeforeCallingService() {
        assertThat(mvc.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \"}"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Request validation failed");
    }

    @Test
    @WithMockUser(authorities = "SCOPE_tasks:write")
    void rejectsUnknownJsonFieldsInsteadOfSilentlyIgnoringTypos() {
        assertThat(mvc.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Write docs\",\"titel\":\"misspelled\"}"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.status").isEqualTo(400);
    }

    @Test
    void requiresAuthenticationForTaskApi() {
        assertThat(mvc.get().uri("/api/tasks"))
                .hasStatus(401)
                .hasHeader("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Unauthorized");
    }
}
