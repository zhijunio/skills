package com.example.taskmanager.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
class Task {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Task() {
    }

    private Task(UUID id, String title, String description, Instant now) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = TaskStatus.OPEN;
        this.createdAt = now;
        this.updatedAt = now;
    }

    static Task create(String title, String description, Instant now) {
        return new Task(UUID.randomUUID(), title, description, now);
    }

    void changeStatus(TaskStatus status, Instant now) {
        if (this.status == status) {
            return;
        }
        boolean allowed = switch (this.status) {
            case OPEN -> status == TaskStatus.IN_PROGRESS;
            case IN_PROGRESS -> status == TaskStatus.DONE;
            case DONE -> false;
        };
        if (!allowed) {
            throw new TaskStatusTransitionException(this.status, status);
        }
        this.status = status;
        this.updatedAt = now;
    }

    UUID getId() { return id; }
    String getTitle() { return title; }
    String getDescription() { return description; }
    TaskStatus getStatus() { return status; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
    Long getVersion() { return version; }
}
