package com.example.config;

import java.net.URI;
import java.time.Duration;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.client")
public record ClientProperties(@NotNull URI baseUrl, @NotNull Duration timeout,
        @Min(1) @Max(5) int maxAttempts) {
    public ClientProperties {
        if (baseUrl != null && (!"https".equalsIgnoreCase(baseUrl.getScheme())
                || baseUrl.getHost() == null || baseUrl.getUserInfo() != null
                || baseUrl.getFragment() != null)) {
            throw new IllegalArgumentException("app.client.base-url must be an HTTPS endpoint without user info or fragment");
        }
        if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
            throw new IllegalArgumentException("app.client.timeout must be positive");
        }
    }
}
