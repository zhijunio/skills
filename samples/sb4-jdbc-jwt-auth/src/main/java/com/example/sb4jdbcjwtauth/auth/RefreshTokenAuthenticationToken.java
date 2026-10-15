package com.example.sb4jdbcjwtauth.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

public final class RefreshTokenAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final String credentials;
    private final TokenResponse tokenResponse;

    private RefreshTokenAuthenticationToken(Object principal, String credentials, boolean authenticated,
                                            TokenResponse tokenResponse) {
        super(List.of());
        this.principal = principal;
        this.credentials = credentials;
        this.tokenResponse = tokenResponse;
        super.setAuthenticated(authenticated);
    }

    public static RefreshTokenAuthenticationToken unauthenticated(String token) {
        return new RefreshTokenAuthenticationToken("refresh-token", token, false, null);
    }

    public static RefreshTokenAuthenticationToken authenticated(RefreshTokenRotation rotation) {
        return new RefreshTokenAuthenticationToken(rotation.userId(), null, true, rotation.tokens());
    }

    public TokenResponse tokenResponse() {
        return tokenResponse;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public String getCredentials() {
        return credentials;
    }
}
