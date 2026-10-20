package com.example.springbootskillvalidation.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UuidGenerator.Style;

@Entity
@Table(name = "tasks")
class Task {

    @Id
    @UuidGenerator(style = Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Task() {}

    private Task(String title, String description, Instant now) {
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
    }

    static Task create(String title, String description, Instant now) {
        return new Task(title, description, now);
    }

    void update(String title, String description, boolean completed, Instant now) {
        this.title = Objects.requireNonNull(title, "title");
        this.description = Objects.requireNonNull(description, "description");
        this.completed = completed;
        this.updatedAt = Objects.requireNonNull(now, "now");
    }

    UUID id() {
        return id;
    }

    String title() {
        return title;
    }

    String description() {
        return description;
    }

    boolean completed() {
        return completed;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }

    long version() {
        return version;
    }
}
