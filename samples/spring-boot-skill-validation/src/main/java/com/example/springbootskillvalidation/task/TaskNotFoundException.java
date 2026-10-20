package com.example.springbootskillvalidation.task;

import java.util.UUID;

class TaskNotFoundException extends RuntimeException {

    TaskNotFoundException(UUID id) {
        super("Task %s was not found".formatted(id));
    }
}
