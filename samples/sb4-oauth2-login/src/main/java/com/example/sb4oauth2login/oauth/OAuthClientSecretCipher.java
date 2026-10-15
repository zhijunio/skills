package com.example.sb4oauth2login.oauth;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class OAuthClientSecretCipher {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    OAuthClientSecretCipher(@Value("${app.oauth2.client-secret-encryption-key}") String encodedKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "OAUTH2_CLIENT_SECRET_ENCRYPTION_KEY must be standard Base64 from `openssl rand -base64 32`; "
                            + "replace the .env.sample placeholder",
                    exception);
        }
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "OAUTH2_CLIENT_SECRET_ENCRYPTION_KEY must decode to exactly 32 bytes; generate it with `openssl rand -base64 32`");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "v1:"
                    + Base64.getEncoder()
                            .encodeToString(ByteBuffer.allocate(nonce.length + ciphertext.length)
                                    .put(nonce)
                                    .put(ciphertext)
                                    .array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt OAuth client secret", exception);
        }
    }

    String decrypt(String encrypted) {
        try {
            if (encrypted == null || !encrypted.startsWith("v1:")) {
                throw new IllegalArgumentException("Unsupported OAuth client secret ciphertext format");
            }
            byte[] value = Base64.getDecoder().decode(encrypted.substring(3));
            if (value.length <= NONCE_LENGTH) {
                throw new IllegalArgumentException("Invalid OAuth client secret ciphertext");
            }
            ByteBuffer buffer = ByteBuffer.wrap(value);
            byte[] nonce = new byte[NONCE_LENGTH];
            buffer.get(nonce);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not decrypt OAuth client secret", exception);
        }
    }
}
