package com.example.sb4jdbcjwtauth.config;

import com.example.sb4jdbcjwtauth.StringIds;

import com.example.sb4jdbcjwtauth.auth.UserSessionRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Session JWT validator")
class SessionJwtValidatorTest {

    private final UserSessionRepository sessions = mock(UserSessionRepository.class);
    private final SessionJwtValidator validator = new SessionJwtValidator(sessions);
    private final String sessionId = StringIds.newId();
    private final String userId = StringIds.newId();

    @Test
    @DisplayName("accepts a token for an active session owned by its subject")
    void acceptsTokenForAnActiveSessionOwnedByItsSubject() {
        when(sessions.isActive(eq(sessionId), eq(userId), any())).thenReturn(true);

        assertThat(validator.validate(jwt(sessionId, userId)).hasErrors()).isFalse();

        verify(sessions).isActive(eq(sessionId), eq(userId), any());
    }

    @Test
    @DisplayName("rejects a token when its session has been revoked")
    void rejectsTokenWhenSessionWasRevoked() {
        when(sessions.isActive(eq(sessionId), eq(userId), any())).thenReturn(false);

        assertThat(validator.validate(jwt(sessionId, userId)).hasErrors()).isTrue();
    }

    @Test
    @DisplayName("rejects a token with a missing or unknown session ID")
    void rejectsTokenWithMissingOrUnknownSessionId() {
        when(sessions.isActive(eq("unknown-session"), eq(userId), any())).thenReturn(false);

        assertThat(validator.validate(jwt("unknown-session", userId)).hasErrors()).isTrue();
        assertThat(validator.validate(jwt(null, userId)).hasErrors()).isTrue();

        verify(sessions).isActive(eq("unknown-session"), eq(userId), any());
        verify(sessions, never()).isActive(eq(null), any(), any());
    }

    private Jwt jwt(String sessionClaim, String subject) {
        var builder = Jwt.withTokenValue("access-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(600))
                .subject(subject);
        if (sessionClaim != null) {
            builder.claim("sid", sessionClaim);
        }
        return builder.build();
    }
}
