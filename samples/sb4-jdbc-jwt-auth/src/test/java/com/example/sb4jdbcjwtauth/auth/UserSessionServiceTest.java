package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.StringIds;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("User session service")
class UserSessionServiceTest {

    private final UserSessionRepository sessions = mock(UserSessionRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final UserSessionService service = new UserSessionService(sessions, tokens);

    @Test
    @DisplayName("revokes an owned session and its refresh tokens")
    void revokesOwnedSessionAndItsRefreshTokens() {
        String userId = StringIds.newId();
        String sessionId = StringIds.newId();
        when(sessions.revoke(eq(sessionId), eq(userId), any())).thenReturn(true);

        assertThat(service.revoke(userId, sessionId)).isTrue();

        verify(tokens).revokeSession(eq(sessionId), any());
    }

    @Test
    @DisplayName("does not revoke tokens for an unowned or inactive session")
    void doesNotRevokeTokensWhenSessionIsNotOwnedOrAlreadyRevoked() {
        String userId = StringIds.newId();
        String sessionId = StringIds.newId();
        when(sessions.revoke(eq(sessionId), eq(userId), any())).thenReturn(false);

        assertThat(service.revoke(userId, sessionId)).isFalse();

        verify(tokens, never()).revokeSession(any(), any());
    }

    @Test
    @DisplayName("marks the current session in the session list")
    void marksTheCurrentSessionInTheList() {
        String userId = StringIds.newId();
        String currentSessionId = StringIds.newId();
        String otherSessionId = StringIds.newId();
        Instant now = Instant.now();
        when(sessions.findActiveByUser(eq(userId), any())).thenReturn(List.of(
                new UserSession(currentSessionId, "Browser", "192.0.2.1", now, null, now.plusSeconds(60)),
                new UserSession(otherSessionId, "Phone", "192.0.2.2", now, now, now.plusSeconds(60))));

        List<UserSessionResponse> result = service.list(userId, currentSessionId);

        assertThat(result).extracting(UserSessionResponse::current).containsExactly(true, false);
    }
}
