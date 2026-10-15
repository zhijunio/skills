package com.example.sb4mfa.auth;

/** Verifies a proof for an enrolled MFA factor. */
public interface MfaFactorVerifier {
    MfaFactor factor();

    void verify(String username, String proof);
}
