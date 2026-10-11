package com.example.taskmanager.task;

import java.time.Instant;

public record TaskStatusHistoryResponse(long id, TaskStatus fromStatus, TaskStatus toStatus, Instant changedAt) {

    static TaskStatusHistoryResponse from(TaskStatusHistory history) {
        return new TaskStatusHistoryResponse(history.getId(), history.getFromStatus(), history.getToStatus(),
                history.getChangedAt());
    }
}
