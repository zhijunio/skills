package com.example.taskmanager.task;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ProblemDetail;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping(path = "/api/tasks", version = "1.0")
public class TaskController {

    private final TaskService tasks;

    TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @PostMapping
    @Parameter(name = "API-Version", in = ParameterIn.HEADER,
            description = "Semantic API version. Defaults to 1.0.", schema = @Schema(defaultValue = "1.0"))
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody CreateTaskRequest request) {
        return tasks.create(request);
    }

    @GetMapping("/{id}")
    @Parameter(name = "API-Version", in = ParameterIn.HEADER,
            description = "Semantic API version. Defaults to 1.0.", schema = @Schema(defaultValue = "1.0"))
    public TaskResponse get(@PathVariable UUID id) {
        return tasks.get(id);
    }

    @GetMapping("/{id}/history")
    @Parameter(name = "API-Version", in = ParameterIn.HEADER,
            description = "Semantic API version. Defaults to 1.0.", schema = @Schema(defaultValue = "1.0"))
    public List<TaskStatusHistoryResponse> history(@PathVariable UUID id) {
        return tasks.history(id);
    }

    @GetMapping
    @Parameter(name = "API-Version", in = ParameterIn.HEADER,
            description = "Semantic API version. Defaults to 1.0.", schema = @Schema(defaultValue = "1.0"))
    public Page<TaskResponse> list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return tasks.list(pageable);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Change a task's status",
            description = "Allowed transitions are OPEN → IN_PROGRESS → DONE. DONE is terminal.")
    @Parameter(name = "API-Version", in = ParameterIn.HEADER,
            description = "Semantic API version. Defaults to 1.0.", schema = @Schema(defaultValue = "1.0"))
    @ApiResponse(responseCode = "409", description = "The status transition is invalid or the task version is stale",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public TaskResponse changeStatus(@PathVariable UUID id,
                                     @Valid @RequestBody ChangeTaskStatusRequest request) {
        return tasks.changeStatus(id, request.status(), request.expectedVersion());
    }
}
