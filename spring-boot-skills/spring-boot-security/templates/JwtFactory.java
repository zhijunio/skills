package com.example.auth.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class JwtFactory {
    private final JwtEncoder encoder;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtFactory(
            JwtEncoder encoder,
            @org.springframework.beans.factory.annotation.Value("${app.jwt.access-token-expiration:900000}") long accessTokenExpiration,
            @org.springframework.beans.factory.annotation.Value("${app.jwt.refresh-token-expiration:604800000}") long refreshTokenExpiration) {
        if (accessTokenExpiration <= 0 || refreshTokenExpiration <= 0) {
            throw new IllegalArgumentException("JWT lifetimes must be positive");
        }
        this.encoder = encoder;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken("access", userDetails, accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken("refresh", userDetails, refreshTokenExpiration);
    }

    private String buildToken(String type, UserDetails userDetails, long expiration) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .subject(userDetails.getUsername())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiresAt(now.plusMillis(expiration))
            .claim("type", type)
            .claim("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList())
            .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
