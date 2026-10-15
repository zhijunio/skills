package com.example.sb4jdbcjwtauth.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenAuthenticationProvider implements AuthenticationProvider {

    private final RefreshTokenService refreshTokens;

    public RefreshTokenAuthenticationProvider(RefreshTokenService refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        var request = (RefreshTokenAuthenticationToken) authentication;
        try {
            RefreshTokenRotation rotation = refreshTokens.rotate(request.getCredentials());
            return RefreshTokenAuthenticationToken.authenticated(rotation);
        } catch (InvalidRefreshTokenException exception) {
            throw new BadCredentialsException("Invalid refresh token", exception);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
