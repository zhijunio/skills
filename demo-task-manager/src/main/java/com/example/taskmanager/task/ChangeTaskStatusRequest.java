package com.example.taskmanager.task;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeTaskStatusRequest(
        @NotNull TaskStatus status,
        @NotNull @PositiveOrZero Long expectedVersion
) {
}
