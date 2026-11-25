package com.example.templatemaven.user.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "{user.email.required}")
        @Email(message = "{user.email.invalid}")
        @Size(max = 254, message = "{user.email.max-size}")
        String email,

        @NotBlank(message = "{user.display-name.required}") @Size(max = 100, message = "{user.display-name.max-size}")
        String displayName) {}
