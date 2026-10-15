package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.user.User;

import java.time.Instant;

public record UserResponse(String id, String email, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.email(), user.createdAt());
    }
}
