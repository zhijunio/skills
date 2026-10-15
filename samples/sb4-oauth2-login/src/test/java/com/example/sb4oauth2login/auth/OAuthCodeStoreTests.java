package com.example.sb4oauth2login.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sb4oauth2login.user.AppUser;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OAuthCodeStoreTests {
    @Test
    void codeCanBeConsumedOnlyOnceAndExpiresAfterSixtySeconds() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        OAuthCodeStore codes = new OAuthCodeStore(clock);
        AppUser user = new AppUser("user-1", "dex-subject", "Dex user", "user@example.test", "", true);

        String singleUseCode = codes.issue(user);
        assertThat(codes.consume(singleUseCode)).contains(user);
        assertThat(codes.consume(singleUseCode)).isEmpty();

        String expiringCode = codes.issue(user);
        clock.advance(Duration.ofSeconds(61));
        assertThat(codes.consume(expiringCode)).isEmpty();
    }

    @Test
    void codeIsExpiredAtTheExactExpiryInstant() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        OAuthCodeStore codes = new OAuthCodeStore(clock);
        AppUser user = new AppUser("user-1", "oauth-user-1", "Dex user", "user@example.test", "", true);
        String code = codes.issue(user);

        clock.advance(Duration.ofSeconds(60));

        assertThat(codes.consume(code)).isEmpty();
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void advance(Duration duration) {
            current.updateAndGet(instant -> instant.plus(duration));
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current.get();
        }
    }
}
