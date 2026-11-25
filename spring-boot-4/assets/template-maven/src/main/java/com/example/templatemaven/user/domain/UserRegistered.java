package com.example.templatemaven.user.domain;

/** Published after a user account is created. */
public record UserRegistered(Long userId, String email, String displayName) {}
