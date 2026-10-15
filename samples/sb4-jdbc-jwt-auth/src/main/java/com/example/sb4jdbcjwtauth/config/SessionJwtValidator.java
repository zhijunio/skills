package com.example.sb4jdbcjwtauth.config;

import com.example.sb4jdbcjwtauth.auth.UserSessionRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SessionJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_SESSION =
            new OAuth2Error("invalid_token", "The session is invalid or revoked", null);

    private final UserSessionRepository sessions;

    public SessionJwtValidator(UserSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String sessionId = jwt.getClaimAsString("sid");
        String userId = jwt.getSubject();
        if (sessionId == null || sessionId.isBlank() || userId == null || userId.isBlank()) {
            return OAuth2TokenValidatorResult.failure(INVALID_SESSION);
        }
        return sessions.isActive(sessionId, userId, Instant.now())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(INVALID_SESSION);
    }
}
