package com.example.templatemaven.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** User fields and version expected by a conditional update. */
public record UpdateUserRequest(
        @NotBlank(message = "{user.email.required}")
        @Email(message = "{user.email.invalid}")
        @Size(max = 254, message = "{user.email.max-size}")
        String email,

        @NotBlank(message = "{user.display-name.required}") @Size(max = 100, message = "{user.display-name.max-size}")
        String displayName,

        @NotNull(message = "{user.version.required}") @PositiveOrZero(message = "{user.version.min}")
        Integer version) {}
