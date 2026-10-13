package com.example.sb4jpatask.task;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChangeTaskStatusRequest(
        @NotNull TaskStatus status,
        @NotNull @PositiveOrZero Long expectedVersion
) {
}
