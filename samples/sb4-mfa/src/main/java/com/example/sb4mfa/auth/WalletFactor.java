package com.example.sb4mfa.auth;

public interface WalletFactor extends MfaFactorVerifier {
    String startEnrollmentChallenge(String username, String address);

    void completeEnrollment(String username, String signature);

    String startLoginChallenge(String username);

    @Override
    default MfaFactor factor() {
        return MfaFactor.WALLET;
    }
}
