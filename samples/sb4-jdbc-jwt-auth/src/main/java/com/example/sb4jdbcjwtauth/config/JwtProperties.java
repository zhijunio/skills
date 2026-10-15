package com.example.sb4jdbcjwtauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import org.springframework.core.io.Resource;

@ConfigurationProperties("app.jwt")
public record JwtProperties(Resource publicKey, Resource privateKey, String keyId,
                            String issuer, String audience, Duration accessTokenTtl, Duration refreshTokenTtl) {
}
