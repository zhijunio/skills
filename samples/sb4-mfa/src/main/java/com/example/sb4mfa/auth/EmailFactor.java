package com.example.sb4mfa.auth;

public interface EmailFactor extends MfaFactorVerifier {
    void issueCode(String username);

    @Override
    default MfaFactor factor() {
        return MfaFactor.EMAIL;
    }
}
