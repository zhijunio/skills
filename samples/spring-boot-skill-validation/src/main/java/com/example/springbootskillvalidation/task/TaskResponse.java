package com.example.springbootskillvalidation.task;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        boolean completed,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    static TaskResponse from(Task task) {
        return new TaskResponse(
                task.id(),
                task.title(),
                task.description(),
                task.completed(),
                task.createdAt(),
                task.updatedAt(),
                task.version());
    }
}
