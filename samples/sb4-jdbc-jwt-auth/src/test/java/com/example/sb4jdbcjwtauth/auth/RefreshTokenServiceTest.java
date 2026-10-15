package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.StringIds;

import com.example.sb4jdbcjwtauth.config.JwtProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Refresh token service")
class RefreshTokenServiceTest {

    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final UserSessionRepository sessions = mock(UserSessionRepository.class);
    private final JwtTokenIssuer jwtTokenIssuer = mock(JwtTokenIssuer.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final JwtProperties properties = new JwtProperties(null, null, "key", "issuer", "audience",
            Duration.ofMinutes(15), Duration.ofDays(30));
    private final RefreshTokenService service = new RefreshTokenService(tokens, sessions, jwtTokenIssuer,
            properties, events);

    private final String userId = StringIds.newId();
    private final String sessionId = StringIds.newId();

    @BeforeEach
    void issueAccessToken() {
        when(jwtTokenIssuer.issue(eq(userId), any(String.class)))
                .thenAnswer(invocation -> new JwtTokenIssuer.AccessToken("access", Instant.now().plusSeconds(900)));
    }

    @Test
    @DisplayName("creates a session and refresh token for a user")
    void createForUserCreatesSessionAndRefreshToken() {
        TokenResponse response = service.createForUser(userId, "Example Browser", "192.0.2.10");

        assertThat(response.sessionId()).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(response.refreshExpiresAt()).isAfter(Instant.now());
        verify(sessions).create(eq(response.sessionId()), eq(userId), eq(response.refreshExpiresAt()),
                eq("Example Browser"), eq("192.0.2.10"));
        verify(tokens).insert(response.refreshToken(), response.sessionId(), userId, response.refreshExpiresAt());
        verify(jwtTokenIssuer).issue(userId, response.sessionId());
    }

    @Test
    @DisplayName("rotates a token while retaining its session")
    void rotateRevokesPreviousTokenAndIssuesNextTokenForSameSession() {
        Instant expiry = Instant.now().plus(Duration.ofDays(1));
        when(tokens.lockActiveSession(eq("old-token"), any())).thenReturn(true);
        when(tokens.findForUpdate("old-token"))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userId, expiry, null)));

        RefreshTokenRotation rotation = service.rotate("old-token");

        assertThat(rotation.userId()).isEqualTo(userId);
        assertThat(rotation.tokens().sessionId()).isEqualTo(sessionId);
        assertThat(rotation.tokens().refreshToken()).matches("[A-Za-z0-9_-]{43}")
                .isNotEqualTo("old-token");
        verify(tokens).revoke(eq("old-token"), any());
        verify(sessions).touch(eq(sessionId), any(), eq(rotation.tokens().refreshExpiresAt()));
        verify(tokens).insert(rotation.tokens().refreshToken(), sessionId, userId,
                rotation.tokens().refreshExpiresAt());
        verify(events).publishEvent(any(AuthLifecycleEvent.class));
    }

    @Test
    @DisplayName("rejects a token belonging to an inactive session")
    void rotateRejectsInactiveSession() {
        when(tokens.lockActiveSession(eq("old-token"), any())).thenReturn(false);

        assertThatThrownBy(() -> service.rotate("old-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokens, never()).findForUpdate("old-token");
    }

    @Test
    @DisplayName("revokes the session when a used token is replayed")
    void replayedTokenRevokesItsSession() {
        Instant expiry = Instant.now().plus(Duration.ofDays(1));
        when(tokens.lockActiveSession(eq("used-token"), any())).thenReturn(true);
        when(tokens.findForUpdate("used-token"))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userId, expiry, Instant.now().minusSeconds(1))));

        assertThatThrownBy(() -> service.rotate("used-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokens).revokeSession(eq(sessionId), any());
        verify(sessions).revoke(eq(sessionId), eq(userId), any());
        verify(jwtTokenIssuer, never()).issue(any(), any());
    }

    @Test
    @DisplayName("rejects and revokes an expired refresh token")
    void expiredTokenIsRejectedAndRevoked() {
        when(tokens.lockActiveSession(eq("expired-token"), any())).thenReturn(true);
        when(tokens.findForUpdate("expired-token"))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userId, Instant.now().minusSeconds(1), null)));

        assertThatThrownBy(() -> service.rotate("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);

        verify(tokens).revoke(eq("expired-token"), any());
        verify(tokens, never()).insert(any(), any(), any(), any());
    }

    @Test
    @DisplayName("logout revokes all tokens for the session")
    void logoutRevokesAllTokensForTheSession() {
        when(tokens.lockActiveSession(eq("refresh-token"), any())).thenReturn(true);
        when(tokens.findForUpdate("refresh-token"))
                .thenReturn(Optional.of(new RefreshToken(sessionId, userId, Instant.now().plusSeconds(60), null)));

        service.revoke("refresh-token");

        verify(tokens).revokeSession(eq(sessionId), any());
        verify(sessions).revoke(eq(sessionId), eq(userId), any());
        verify(events).publishEvent(any(AuthLifecycleEvent.class));
    }
}
