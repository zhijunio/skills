package com.example.springbootskillvalidation.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 2000) String description,
        @NotNull Boolean completed,
        @NotNull Long version) {}
