package com.example.taskmanager.task;

import com.example.taskmanager.config.ApiExceptionHandler;
import com.example.taskmanager.config.ApiVersionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(TaskController.class)
@Import({ApiExceptionHandler.class, TaskExceptionHandler.class, ApiVersionConfig.class})
class TaskControllerWebTest {

    @Autowired
    MockMvcTester mvc;

    @MockitoBean
    TaskService tasks;

    @Test
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
    void rejectsBlankTitleBeforeCallingService() {
        assertThat(mvc.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \"}"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.title").isEqualTo("Request validation failed");
    }

    @Test
    void rejectsUnknownJsonFieldsInsteadOfSilentlyIgnoringTypos() {
        assertThat(mvc.post().uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Write docs\",\"titel\":\"misspelled\"}"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.status").isEqualTo(400);
    }
}
