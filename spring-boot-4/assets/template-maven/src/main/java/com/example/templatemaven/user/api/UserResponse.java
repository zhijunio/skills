package com.example.templatemaven.user.api;

import java.time.Instant;

/** A user account returned by the API. */
public record UserResponse(Long id, String email, String displayName, Integer version, Instant createdAt) {}
