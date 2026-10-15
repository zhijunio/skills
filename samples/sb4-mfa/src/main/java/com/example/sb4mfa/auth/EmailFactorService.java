package com.example.sb4mfa.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class EmailFactorService implements EmailFactor {
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Clock clock;
    private final Map<String, EmailChallenge> challenges = new ConcurrentHashMap<>();

    public EmailFactorService(Clock clock) {
        this.clock = clock;
    }

    public void issueCode(String username) {
        String email = "demo@example.test";
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        challenges.put(username, new EmailChallenge(email, code, clock.instant().plus(CHALLENGE_TTL), 0));
        // Demo delivery only. Replace with a mail provider; never log OTPs in production.
        System.out.printf("[MFA DEMO EMAIL] to=%s code=%s%n", email, code);
    }

    public void verify(String username, String code) {
        EmailChallenge challenge = challenges.get(username);
        if (challenge == null || !clock.instant().isBefore(challenge.expiresAt()) || challenge.attempts() >= 5) {
            challenges.remove(username);
            throw new IllegalArgumentException("Email challenge is missing, expired, or locked");
        }
        if (!java.security.MessageDigest.isEqual(
                challenge.code().getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))) {
            challenges.put(
                    username,
                    new EmailChallenge(
                            challenge.email(), challenge.code(), challenge.expiresAt(), challenge.attempts() + 1));
            throw new IllegalArgumentException("Invalid email code");
        }
        if (!challenges.remove(username, challenge)) {
            throw new IllegalArgumentException("Email code was already used");
        }
    }

    private record EmailChallenge(String email, String code, Instant expiresAt, int attempts) {}
}
