package com.example.sb4jwt.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotNull Resource publicKey,
        @NotNull Resource privateKey,
        @NotBlank String keyId,
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotNull Duration accessTokenTtl) {

    public JwtProperties {
        if (accessTokenTtl != null && (accessTokenTtl.isZero() || accessTokenTtl.isNegative())) {
            throw new IllegalArgumentException("app.jwt.access-token-ttl must be positive");
        }
    }
}
