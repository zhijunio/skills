package com.example.sb4oauth2login.user;

import java.util.Set;

public record AppUser(
        String id,
        String username,
        String displayName,
        String email,
        String passwordHash,
        boolean active,
        Set<String> roles) {
    public AppUser {
        roles = Set.copyOf(roles);
    }

    public AppUser(String id, String username, String displayName, String email, String passwordHash, boolean active) {
        this(id, username, displayName, email, passwordHash, active, Set.of("USER"));
    }
}
