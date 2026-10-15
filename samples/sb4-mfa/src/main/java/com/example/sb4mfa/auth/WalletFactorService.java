package com.example.sb4mfa.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

@Service
public class WalletFactorService implements WalletFactor {
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Clock clock;
    private final Map<String, WalletChallenge> challenges = new ConcurrentHashMap<>();
    private final Map<String, String> walletAddresses = new ConcurrentHashMap<>();

    public WalletFactorService(Clock clock) {
        this.clock = clock;
    }

    public String startEnrollmentChallenge(String username, String address) {
        return createChallenge(username, normalizeAddress(address));
    }

    public String startLoginChallenge(String username) {
        String address = walletAddresses.get(username);
        if (address == null) {
            throw new IllegalStateException("Enroll a wallet first");
        }
        return createChallenge(username, address);
    }

    private String createChallenge(String username, String address) {
        String nonce = randomToken(24);
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(CHALLENGE_TTL);
        String message = "example.com wants you to sign in with your Ethereum account:\n"
                + address + "\n\n"
                + "Sign in to the Spring Boot MFA demonstration.\n\n"
                + "URI: https://example.com\nVersion: 1\nChain ID: 1\nNonce: " + nonce
                + "\nIssued At: " + issuedAt + "\nExpiration Time: " + expiresAt;
        challenges.put(username, new WalletChallenge(address, message, expiresAt));
        return message;
    }

    public void completeEnrollment(String username, String signature) {
        WalletChallenge challenge = requireChallenge(username);
        String signer = recoverAddress(challenge.message(), signature);
        if (!signer.equalsIgnoreCase(challenge.address())) {
            throw new IllegalArgumentException("Wallet signature does not match the challenged address");
        }
        if (!challenges.remove(username, challenge)) {
            throw new IllegalArgumentException("Wallet challenge was already used");
        }
        walletAddresses.put(username, signer);
    }

    public void verify(String username, String signature) {
        WalletChallenge challenge = requireChallenge(username);
        String expected = walletAddresses.get(username);
        String signer = recoverAddress(challenge.message(), signature);
        if (expected == null || !signer.equalsIgnoreCase(expected) || !signer.equalsIgnoreCase(challenge.address())) {
            throw new IllegalArgumentException("Invalid wallet signature");
        }
        if (!challenges.remove(username, challenge)) {
            throw new IllegalArgumentException("Wallet challenge was already used");
        }
    }

    private WalletChallenge requireChallenge(String username) {
        WalletChallenge challenge = challenges.get(username);
        if (challenge == null || !clock.instant().isBefore(challenge.expiresAt())) {
            challenges.remove(username);
            throw new IllegalArgumentException("Wallet challenge is missing or expired");
        }
        return challenge;
    }

    private String recoverAddress(String message, String signatureHex) {
        try {
            byte[] signature = Numeric.hexStringToByteArray(signatureHex);
            if (signature.length != 65) {
                throw new IllegalArgumentException("Expected a 65-byte EIP-191 signature");
            }
            byte[] r = Arrays.copyOfRange(signature, 0, 32);
            byte[] s = Arrays.copyOfRange(signature, 32, 64);
            byte[] v = Arrays.copyOfRange(signature, 64, 65);
            String address = "0x"
                    + Keys.getAddress(Sign.signedPrefixedMessageToKey(
                            message.getBytes(StandardCharsets.UTF_8), new Sign.SignatureData(v, r, s)));
            return normalizeAddress(address);
        } catch (Exception ex) {
            if (ex instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalArgumentException("Invalid EIP-191 signature", ex);
        }
    }

    private String normalizeAddress(String address) {
        if (address == null || !address.matches("(?i)^0x[0-9a-f]{40}$")) {
            throw new IllegalArgumentException("Expected an Ethereum address");
        }
        return "0x" + address.substring(2).toLowerCase(Locale.ROOT);
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return java.util.HexFormat.of().formatHex(value);
    }

    private record WalletChallenge(String address, String message, Instant expiresAt) {}
}
