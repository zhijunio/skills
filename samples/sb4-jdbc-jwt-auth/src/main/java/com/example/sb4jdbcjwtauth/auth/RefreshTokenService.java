package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.StringIds;

import com.example.sb4jdbcjwtauth.config.JwtProperties;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository tokens;
    private final UserSessionRepository sessions;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final JwtProperties properties;
    private final ApplicationEventPublisher events;

    public RefreshTokenService(RefreshTokenRepository tokens, UserSessionRepository sessions,
                               JwtTokenIssuer jwtTokenIssuer,
                               JwtProperties properties, ApplicationEventPublisher events) {
        this.tokens = tokens;
        this.sessions = sessions;
        this.jwtTokenIssuer = jwtTokenIssuer;
        this.properties = properties;
        this.events = events;
    }

    @Transactional
    public TokenResponse createForUser(String userId, String userAgent, String ipAddress) {
        Instant now = Instant.now();
        String sessionId = StringIds.newId();
        String refreshToken = newToken();
        Instant refreshExpiresAt = now.plus(properties.refreshTokenTtl());
        sessions.create(sessionId, userId, refreshExpiresAt, userAgent, ipAddress);
        tokens.insert(refreshToken, sessionId, userId, refreshExpiresAt);
        return response(userId, sessionId, refreshToken, refreshExpiresAt);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshTokenRotation rotate(String presentedToken) {
        Instant now = Instant.now();
        if (!tokens.lockActiveSession(presentedToken, now)) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken current = tokens.findForUpdate(presentedToken)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.revokedAt() != null) {
            tokens.revokeSession(current.sessionId(), now);
            sessions.revoke(current.sessionId(), current.userId(), now);
            events.publishEvent(new AuthLifecycleEvent("refresh-token-reuse-detected", current.userId(), now));
            throw new InvalidRefreshTokenException();
        }
        if (!current.expiresAt().isAfter(now)) {
            tokens.revoke(presentedToken, now);
            throw new InvalidRefreshTokenException();
        }

        tokens.revoke(presentedToken, now);
        String nextToken = newToken();
        Instant nextExpiry = now.plus(properties.refreshTokenTtl());
        sessions.touch(current.sessionId(), now, nextExpiry);
        tokens.insert(nextToken, current.sessionId(), current.userId(), nextExpiry);
        events.publishEvent(new AuthLifecycleEvent("refresh-token-rotated", current.userId(), now));
        return new RefreshTokenRotation(current.userId(),
                response(current.userId(), current.sessionId(), nextToken, nextExpiry));
    }

    @Transactional
    public void revoke(String presentedToken) {
        Instant now = Instant.now();
        if (!tokens.lockActiveSession(presentedToken, now)) {
            return;
        }
        tokens.findForUpdate(presentedToken).ifPresent(current -> {
            tokens.revokeSession(current.sessionId(), now);
            sessions.revoke(current.sessionId(), current.userId(), now);
            events.publishEvent(new AuthLifecycleEvent("logged-out", current.userId(), now));
        });
    }

    @Scheduled(fixedDelayString = "${app.security.maintenance-interval:PT1H}")
    @Transactional
    public void cleanExpiredSessions() {
        sessions.deleteExpired(Instant.now());
    }

    private TokenResponse response(String userId, String sessionId, String refreshToken, Instant refreshExpiresAt) {
        JwtTokenIssuer.AccessToken accessToken = jwtTokenIssuer.issue(userId, sessionId);
        return new TokenResponse(sessionId, accessToken.value(), "Bearer", accessToken.expiresAt(),
                refreshToken, refreshExpiresAt);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

}
