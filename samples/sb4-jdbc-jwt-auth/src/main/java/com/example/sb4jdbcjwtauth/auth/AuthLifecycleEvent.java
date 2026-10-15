package com.example.sb4jdbcjwtauth.auth;

import java.time.Instant;

public record AuthLifecycleEvent(String action, String userId, Instant occurredAt) {
}
