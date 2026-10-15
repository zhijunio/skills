package com.example.sb4jdbcjwtauth;

import java.security.SecureRandom;
import java.util.Base64;

public final class StringIds {

    private static final SecureRandom RANDOM = new SecureRandom();

    private StringIds() {
    }

    public static String newId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
