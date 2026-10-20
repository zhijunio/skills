package com.example.springbootskillvalidation.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @Test
    void createsTaskAndReturnsLocation() throws Exception {
        var id = UUID.fromString("018f0f4a-7b2c-7abc-8def-0123456789ab");
        given(taskService.create(new CreateTaskRequest("Review the skill", "Check defaults")))
                .willReturn(new TaskResponse(
                        id, "Review the skill", "Check defaults", false, Instant.EPOCH, Instant.EPOCH, 0));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Review the skill","description":"Check defaults"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/tasks/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void rejectsBlankTitleWithProblemDetails() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  ","description":"invalid"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("https://errors.example.com/validation"))
                .andExpect(jsonPath("$.violations[0].field").value("title"));
    }

    @Test
    void returnsProblemDetailsWhenTaskDoesNotExist() throws Exception {
        var id = UUID.fromString("018f0f4a-7b2c-7abc-8def-0123456789ab");
        given(taskService.findById(id)).willThrow(new TaskNotFoundException(id));

        mockMvc.perform(get("/api/tasks/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://errors.example.com/task-not-found"));
    }

    @Test
    void searchesAndPagesTasks() throws Exception {
        var page = PageRequest.of(1, 5, Sort.by(Sort.Order.desc("createdAt")));
        var task = new TaskResponse(UUID.randomUUID(), "One", "", true, Instant.EPOCH, Instant.EPOCH, 2);
        given(taskService.findAll(eq(true), eq("One"), any())).willReturn(new PageImpl<>(List.of(task), page, 6));

        mockMvc.perform(get("/api/tasks")
                        .param("completed", "true")
                        .param("q", "One")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("One"))
                .andExpect(jsonPath("$.content[0].version").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(6))
                .andExpect(jsonPath("$.page.number").value(1));
    }

    @Test
    void updatesAndDeletesTask() throws Exception {
        var id = UUID.fromString("018f0f4a-7b2c-7abc-8def-0123456789ab");
        given(taskService.update(id, new UpdateTaskRequest("Done", "", true, 2L)))
                .willReturn(new TaskResponse(id, "Done", "", true, Instant.EPOCH, Instant.EPOCH, 3));
        doNothing().when(taskService).delete(id);

        mockMvc.perform(put("/api/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Done","description":"","completed":true,"version":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.version").value(3));

        mockMvc.perform(delete("/api/tasks/{id}", id)).andExpect(status().isNoContent());
        verify(taskService).delete(id);
    }

    @Test
    void rejectsUnknownSortFields() throws Exception {
        given(taskService.findAll(isNull(), isNull(), any())).willThrow(new InvalidTaskSortException());

        mockMvc.perform(get("/api/tasks").param("sort", "password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://errors.example.com/invalid-sort"));
    }

    @Test
    void mapsStaleTaskVersionToConflict() throws Exception {
        var id = UUID.fromString("018f0f4a-7b2c-7abc-8def-0123456789ab");
        given(taskService.update(id, new UpdateTaskRequest("Done", "", true, 0L)))
                .willThrow(new TaskVersionConflictException());

        mockMvc.perform(put("/api/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Done","description":"","completed":true,"version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://errors.example.com/concurrent-modification"));
    }
}
