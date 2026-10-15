package com.example.sb4mfa.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.web3j.crypto.MnemonicUtils;

@Service
public class RecoveryPhraseFactorService implements RecoveryPhraseFactor {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, RecoveryPhraseCredential> credentials = new ConcurrentHashMap<>();

    public String enroll(String username) {
        byte[] entropy = new byte[16];
        RANDOM.nextBytes(entropy);
        String phrase = MnemonicUtils.generateMnemonic(entropy);
        credentials.put(username, new RecoveryPhraseCredential(sha256(phrase), 0));
        return phrase;
    }

    public synchronized void verify(String username, String submittedPhrase) {
        RecoveryPhraseCredential credential = credentials.get(username);
        if (credential == null) {
            throw new IllegalStateException("Enroll a recovery phrase first");
        }

        String normalized = normalize(submittedPhrase);
        boolean matches = java.security.MessageDigest.isEqual(credential.hash(), sha256(normalized));
        if (!matches) {
            if (credential.attempts() + 1 >= 5) {
                credentials.remove(username, credential);
            } else {
                credentials.replace(
                        username,
                        credential,
                        new RecoveryPhraseCredential(credential.hash(), credential.attempts() + 1));
            }
            throw new IllegalArgumentException("Invalid or expired recovery phrase");
        }
        if (!credentials.remove(username, credential)) {
            throw new IllegalArgumentException("Recovery phrase was already used");
        }
    }

    private byte[] sha256(String value) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String normalize(String phrase) {
        return phrase == null ? "" : phrase.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private record RecoveryPhraseCredential(byte[] hash, int attempts) {}
}
