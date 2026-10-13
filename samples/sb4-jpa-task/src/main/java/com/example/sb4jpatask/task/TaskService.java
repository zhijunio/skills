package com.example.sb4jpatask.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final TaskStatusHistoryRepository statusHistory;

    TaskService(TaskRepository tasks, TaskStatusHistoryRepository statusHistory) {
        this.tasks = tasks;
        this.statusHistory = statusHistory;
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCOPE_tasks:write')")
    public TaskResponse create(CreateTaskRequest request) {
        Task task = Task.create(request.title().trim(), request.description(), Instant.now());
        Task saved = tasks.save(task);
        statusHistory.save(TaskStatusHistory.created(saved.getId(), saved.getStatus(), saved.getCreatedAt()));
        return TaskResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SCOPE_tasks:read')")
    @Cacheable(cacheNames = "tasks", key = "#id", sync = true)
    public TaskResponse get(UUID id) {
        return TaskResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SCOPE_tasks:read')")
    public Page<TaskResponse> list(Pageable pageable) {
        return tasks.findAll(pageable).map(TaskResponse::from);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCOPE_tasks:write')")
    @CacheEvict(cacheNames = "tasks", key = "#id")
    public TaskResponse changeStatus(UUID id, TaskStatus status, long expectedVersion) {
        Task task = find(id);
        if (!Objects.equals(task.getVersion(), expectedVersion)) {
            throw new StaleTaskVersionException(id, expectedVersion, task.getVersion());
        }
        TaskStatus previousStatus = task.getStatus();
        Instant changedAt = Instant.now();
        task.changeStatus(status, changedAt);
        if (previousStatus != status) {
            statusHistory.save(TaskStatusHistory.changed(id, previousStatus, status, changedAt));
            tasks.flush();
        }
        return TaskResponse.from(task);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('SCOPE_tasks:read')")
    public List<TaskStatusHistoryResponse> history(UUID id) {
        find(id);
        return statusHistory.findAllByTaskIdOrderByIdAsc(id).stream()
                .map(TaskStatusHistoryResponse::from)
                .toList();
    }

    private Task find(UUID id) {
        return tasks.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }
}
