package com.example.sb4oauth2login.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, String issuer, String audience, Duration accessTokenTtl) {}
