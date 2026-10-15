package com.example.sb4mfa.auth;

public interface RecoveryPhraseFactor extends MfaFactorVerifier {
    String enroll(String username);

    @Override
    default MfaFactor factor() {
        return MfaFactor.RECOVERY_PHRASE;
    }
}
