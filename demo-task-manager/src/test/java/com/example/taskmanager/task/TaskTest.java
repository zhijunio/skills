package com.example.taskmanager.task;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskTest {

    @Test
    void permitsForwardTransitionsAndTreatsRepeatedStatusAsNoOp() {
        Instant createdAt = Instant.parse("2026-10-11T00:00:00Z");
        Task task = Task.create("Demo", null, createdAt);

        task.changeStatus(TaskStatus.IN_PROGRESS, createdAt.plusSeconds(10));
        task.changeStatus(TaskStatus.IN_PROGRESS, createdAt.plusSeconds(20));
        task.changeStatus(TaskStatus.DONE, createdAt.plusSeconds(30));

        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getUpdatedAt()).isEqualTo(createdAt.plusSeconds(30));
    }

    @Test
    void rejectsSkippingOrReversingLifecycleSteps() {
        Task task = Task.create("Demo", null, Instant.now());

        assertThatThrownBy(() -> task.changeStatus(TaskStatus.DONE, Instant.now()))
                .isInstanceOf(TaskStatusTransitionException.class);
        task.changeStatus(TaskStatus.IN_PROGRESS, Instant.now());
        assertThatThrownBy(() -> task.changeStatus(TaskStatus.OPEN, Instant.now()))
                .isInstanceOf(TaskStatusTransitionException.class);
    }
}
