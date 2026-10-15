package com.example.sb4oauth2login.user;

import java.util.List;
import java.util.Set;

public record AdminUserSummary(
        String id,
        String username,
        String displayName,
        String email,
        boolean active,
        boolean currentUser,
        boolean hasLocalPassword,
        Set<String> roles,
        List<String> externalProviders) {}
