package com.example.sb4mfa.auth;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

@Service
public class TotpFactorService implements TotpFactor {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base32 BASE32 = new Base32();

    private final Clock clock;
    private final Map<String, TotpCredential> credentials = new ConcurrentHashMap<>();

    public TotpFactorService(Clock clock) {
        this.clock = clock;
    }

    public String startEnrollment(String username) {
        byte[] secret = new byte[20];
        RANDOM.nextBytes(secret);
        String encoded = BASE32.encodeToString(secret).replace("=", "");
        credentials.put(username, new TotpCredential(encoded, false, -1));
        return encoded;
    }

    public void confirmEnrollment(String username, String code) {
        TotpCredential credential = requireCredential(username);
        long step = matchingStep(credential.secret(), code);
        if (step < 0) {
            throw new IllegalArgumentException("Invalid TOTP code");
        }
        credentials.put(username, new TotpCredential(credential.secret(), true, step));
    }

    public void verify(String username, String code) {
        TotpCredential credential = requireCredential(username);
        if (!credential.enabled()) {
            throw new IllegalStateException("Confirm TOTP enrollment first");
        }
        long step = matchingStep(credential.secret(), code);
        if (step < 0 || step <= credential.lastAcceptedStep()) {
            throw new IllegalArgumentException("Invalid, expired, or reused TOTP code");
        }
        if (!credentials.replace(username, credential, new TotpCredential(credential.secret(), true, step))) {
            throw new IllegalArgumentException("TOTP code was already used");
        }
    }

    private TotpCredential requireCredential(String username) {
        TotpCredential credential = credentials.get(username);
        if (credential == null) {
            throw new IllegalStateException("Enroll TOTP first");
        }
        return credential;
    }

    private long matchingStep(String encodedSecret, String submittedCode) {
        if (submittedCode == null || !submittedCode.matches("\\d{6}")) {
            return -1;
        }
        byte[] key = BASE32.decode(encodedSecret);
        long current = clock.instant().getEpochSecond() / 30;
        for (long step = current - 1; step <= current + 1; step++) {
            if (java.security.MessageDigest.isEqual(
                    totp(key, step).getBytes(StandardCharsets.US_ASCII),
                    submittedCode.getBytes(StandardCharsets.US_ASCII))) {
                return step;
            }
        }
        return -1;
    }

    private String totp(byte[] key, long step) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] digest =
                    mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(step).array());
            int offset = digest[digest.length - 1] & 0x0f;
            int binary = ((digest[offset] & 0x7f) << 24)
                    | ((digest[offset + 1] & 0xff) << 16)
                    | ((digest[offset + 2] & 0xff) << 8)
                    | (digest[offset + 3] & 0xff);
            return "%06d".formatted(binary % 1_000_000);
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("TOTP is unavailable", ex);
        }
    }

    private record TotpCredential(String secret, boolean enabled, long lastAcceptedStep) {}
}
