package com.example.sb4jdbcjwtauth.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class UserSessionService {

    private final UserSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;

    public UserSessionService(UserSessionRepository sessions, RefreshTokenRepository refreshTokens) {
        this.sessions = sessions;
        this.refreshTokens = refreshTokens;
    }

    @Transactional(readOnly = true)
    public List<UserSessionResponse> list(String userId, String currentSessionId) {
        return sessions.findActiveByUser(userId, Instant.now()).stream()
                .map(session -> UserSessionResponse.from(session, currentSessionId))
                .toList();
    }

    @Transactional
    public boolean revoke(String userId, String sessionId) {
        Instant now = Instant.now();
        if (!sessions.revoke(sessionId, userId, now)) {
            return false;
        }
        refreshTokens.revokeSession(sessionId, now);
        return true;
    }
}
