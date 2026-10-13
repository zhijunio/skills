package com.example.sb4jpatask.task;

import java.util.UUID;

class StaleTaskVersionException extends RuntimeException {

    StaleTaskVersionException(UUID taskId, long expectedVersion, Long actualVersion) {
        super("Task %s has version %s; version %s was requested".formatted(taskId, actualVersion, expectedVersion));
    }
}
