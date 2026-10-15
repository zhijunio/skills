package com.example.sb4mfa.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.MnemonicUtils;
import org.web3j.crypto.Sign;

class MfaAuthenticationServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-15T00:00:00Z");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void acceptsTotpOnceAndGrantsSessionFactor() throws Exception {
        MutableClock clock = new MutableClock(NOW);
        MfaAuthenticationService service = service(clock);
        TotpFactor factor = new TotpFactorService(clock);
        var authentication = authentication();
        String secret = factor.startEnrollment("demo");
        String code = totp(secret, NOW.getEpochSecond() / 30);

        factor.confirmEnrollment("demo", code);
        clock.advanceSeconds(30);
        String verifiedCode = totp(secret, clock.instant().getEpochSecond() / 30);
        factor.verify(authentication.getName(), verifiedCode);
        completeFactor(service, authentication, MfaFactor.TOTP);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("FACTOR_TOTP");
        assertThatThrownBy(() -> factor.verify(authentication().getName(), verifiedCode))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reused");
    }

    @Test
    void walletSignatureIsBoundToAndConsumesItsChallenge() {
        MfaAuthenticationService service = service(Clock.fixed(NOW, ZoneOffset.UTC));
        WalletFactor factor = new WalletFactorService(Clock.fixed(NOW, ZoneOffset.UTC));
        Credentials credentials = Credentials.create("1");
        String address = credentials.getAddress();
        String enrollmentMessage = factor.startEnrollmentChallenge("demo", address);
        String enrollmentSignature = signature(enrollmentMessage, credentials);

        factor.completeEnrollment("demo", enrollmentSignature);
        String loginMessage = factor.startLoginChallenge("demo");
        factor.verify(authentication().getName(), signature(loginMessage, credentials));
        completeFactor(service, authentication(), MfaFactor.WALLET);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("FACTOR_WALLET");
        assertThatThrownBy(() -> factor.verify(authentication().getName(), signature(loginMessage, credentials)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing or expired");
    }

    @Test
    void recoveryPhraseIsGeneratedAsBip39AndCanBeUsedOnlyOnce() {
        MfaAuthenticationService service = service(Clock.fixed(NOW, ZoneOffset.UTC));
        RecoveryPhraseFactor factor = new RecoveryPhraseFactorService();
        String phrase = factor.enroll("demo");

        assertThat(MnemonicUtils.validateMnemonic(phrase)).isTrue();
        assertThat(phrase.split(" ")).hasSize(12);

        factor.verify(authentication().getName(), phrase.toUpperCase());
        completeFactor(service, authentication(), MfaFactor.RECOVERY_PHRASE);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .contains("FACTOR_RECOVERY_PHRASE");
        assertThatThrownBy(() -> factor.verify(authentication().getName(), phrase))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Enroll a recovery phrase first");
    }

    @Test
    void removesRecoveryPhraseAfterFiveInvalidAttempts() {
        RecoveryPhraseFactor factor = new RecoveryPhraseFactorService();
        factor.enroll("demo");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> factor.verify(authentication().getName(), "wrong phrase"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid or expired");
        }

        assertThatThrownBy(() -> factor.verify(authentication().getName(), "wrong phrase"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Enroll a recovery phrase first");
    }

    @Test
    void requiresPrimaryAndIndependentSecondFactorToCompleteMfa() {
        MfaAuthenticationService service = service(Clock.fixed(NOW, ZoneOffset.UTC));
        var passwordOnly = authentication();
        var passwordAndTotp = new UsernamePasswordAuthenticationToken(
                "demo",
                null,
                List.of(new SimpleGrantedAuthority("FACTOR_PASSWORD"), new SimpleGrantedAuthority("FACTOR_TOTP")));
        var passkeyAndEmail = new UsernamePasswordAuthenticationToken(
                "demo",
                null,
                List.of(new SimpleGrantedAuthority("FACTOR_WEBAUTHN"), new SimpleGrantedAuthority("FACTOR_EMAIL")));

        assertThat(service.hasPrimaryFactor(passwordOnly)).isTrue();
        assertThat(service.isMfaComplete(passwordOnly)).isFalse();
        assertThat(service.isMfaComplete(passwordAndTotp)).isTrue();
        assertThat(service.isMfaComplete(passkeyAndEmail)).isTrue();
        assertThat(service.isMfaComplete(null)).isFalse();
    }

    private static UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                "demo", null, List.of(new SimpleGrantedAuthority("FACTOR_PASSWORD")));
    }

    private static MfaAuthenticationService service(Clock clock) {
        return new MfaAuthenticationService(
                clock, new org.springframework.security.web.context.HttpSessionSecurityContextRepository());
    }

    private static void completeFactor(
            MfaAuthenticationService service, UsernamePasswordAuthenticationToken authentication, MfaFactor factor) {
        service.completeFactor(
                authentication,
                factor,
                new org.springframework.mock.web.MockHttpServletRequest(),
                new org.springframework.mock.web.MockHttpServletResponse());
    }

    private static String signature(String message, Credentials credentials) {
        Sign.SignatureData data = Sign.signPrefixedMessage(
                message.getBytes(java.nio.charset.StandardCharsets.UTF_8), credentials.getEcKeyPair());
        byte[] result = new byte[65];
        System.arraycopy(data.getR(), 0, result, 0, 32);
        System.arraycopy(data.getS(), 0, result, 32, 32);
        System.arraycopy(data.getV(), 0, result, 64, 1);
        return org.web3j.utils.Numeric.toHexString(result);
    }

    private static String totp(String secret, long step) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(new Base32().decode(secret), "HmacSHA1"));
        byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        return "%06d".formatted(binary % 1_000_000);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}
