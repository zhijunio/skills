package com.example.sb4mfa.auth;

public interface TotpFactor extends MfaFactorVerifier {
    String startEnrollment(String username);

    void confirmEnrollment(String username, String code);

    @Override
    default MfaFactor factor() {
        return MfaFactor.TOTP;
    }
}
