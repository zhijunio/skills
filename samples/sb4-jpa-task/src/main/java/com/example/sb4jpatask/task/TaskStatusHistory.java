package com.example.sb4jpatask.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_status_history")
class TaskStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, updatable = false)
    private TaskStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20, updatable = false)
    private TaskStatus toStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected TaskStatusHistory() {
    }

    private TaskStatusHistory(UUID taskId, TaskStatus fromStatus, TaskStatus toStatus, Instant changedAt) {
        this.taskId = taskId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedAt = changedAt;
    }

    static TaskStatusHistory created(UUID taskId, TaskStatus status, Instant changedAt) {
        return new TaskStatusHistory(taskId, null, status, changedAt);
    }

    static TaskStatusHistory changed(UUID taskId, TaskStatus from, TaskStatus to, Instant changedAt) {
        return new TaskStatusHistory(taskId, from, to, changedAt);
    }

    Long getId() { return id; }
    UUID getTaskId() { return taskId; }
    TaskStatus getFromStatus() { return fromStatus; }
    TaskStatus getToStatus() { return toStatus; }
    Instant getChangedAt() { return changedAt; }
}
