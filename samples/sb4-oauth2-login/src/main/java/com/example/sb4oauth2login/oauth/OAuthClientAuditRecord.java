package com.example.sb4oauth2login.oauth;

import java.time.Instant;

public record OAuthClientAuditRecord(
        long id,
        String registrationId,
        String action,
        String actor,
        String changedFields,
        boolean clientSecretChanged,
        Instant occurredAt) {}
