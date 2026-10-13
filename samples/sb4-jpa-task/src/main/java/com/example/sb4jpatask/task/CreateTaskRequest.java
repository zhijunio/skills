package com.example.sb4jpatask.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String description) {
}
