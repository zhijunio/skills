package com.example.sb4oauth2login.auth;

import com.example.sb4oauth2login.user.AppUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OAuthCodeStore {
    private static final Duration CODE_TTL = Duration.ofSeconds(60);

    private record Entry(AppUser user, Instant expiresAt) {}

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public OAuthCodeStore(Clock clock) {
        this.clock = clock;
    }

    public String issue(AppUser user) {
        Instant now = clock.instant();
        entries.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        String code = UUID.randomUUID().toString() + UUID.randomUUID();
        entries.put(code, new Entry(user, now.plus(CODE_TTL)));
        return code;
    }

    public Optional<AppUser> consume(String code) {
        Entry entry = entries.remove(code);
        return entry == null || !entry.expiresAt().isAfter(clock.instant())
                ? Optional.empty()
                : Optional.of(entry.user());
    }
}
