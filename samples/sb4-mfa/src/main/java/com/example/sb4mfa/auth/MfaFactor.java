package com.example.sb4mfa.auth;

public enum MfaFactor {
    EMAIL("FACTOR_EMAIL"),
    TOTP("FACTOR_TOTP"),
    WALLET("FACTOR_WALLET"),
    RECOVERY_PHRASE("FACTOR_RECOVERY_PHRASE");

    private final String authority;

    MfaFactor(String authority) {
        this.authority = authority;
    }

    public String authority() {
        return authority;
    }
}
