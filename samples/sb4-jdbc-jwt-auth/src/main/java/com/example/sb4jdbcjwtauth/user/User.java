package com.example.sb4jdbcjwtauth.user;

import java.time.Instant;

public record User(String id, String email, String password, Instant createdAt) {

    @Override
    public String toString() {
        return "User[id=" + id + ", email=" + email + ", createdAt=" + createdAt + "]";
    }
}
