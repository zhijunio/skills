package com.example.taskmanager.task;

import java.util.UUID;

final class TaskNotFoundException extends RuntimeException {
    TaskNotFoundException(UUID id) {
        super("Task %s was not found".formatted(id));
    }
}
