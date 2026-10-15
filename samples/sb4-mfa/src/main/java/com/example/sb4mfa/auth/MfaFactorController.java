package com.example.sb4mfa.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mfa")
public class MfaFactorController {
    private final MfaAuthenticationService mfaService;
    private final TotpFactor totpFactor;
    private final EmailFactor emailFactor;
    private final WalletFactor walletFactor;
    private final RecoveryPhraseFactor recoveryPhraseFactor;

    public MfaFactorController(
            MfaAuthenticationService mfaService,
            TotpFactor totpFactor,
            EmailFactor emailFactor,
            WalletFactor walletFactor,
            RecoveryPhraseFactor recoveryPhraseFactor) {
        this.mfaService = mfaService;
        this.totpFactor = totpFactor;
        this.emailFactor = emailFactor;
        this.walletFactor = walletFactor;
        this.recoveryPhraseFactor = recoveryPhraseFactor;
    }

    @PostMapping("/totp/enroll")
    Map<String, String> enrollTotp(Authentication authentication) {
        String secret = totpFactor.startEnrollment(authentication.getName());
        String label = URLEncoder.encode("Spring MFA Demo:" + authentication.getName(), StandardCharsets.UTF_8);
        String uri = "otpauth://totp/" + label + "?secret=" + secret + "&issuer=Spring%20MFA%20Demo";
        return Map.of(
                "secret",
                secret,
                "otpauthUri",
                uri,
                "warning",
                "Development demo: display once over TLS and do not log this secret.");
    }

    @PostMapping("/totp/confirm")
    Map<String, String> confirmTotp(@Valid @RequestBody CodeRequest body, Authentication authentication) {
        totpFactor.confirmEnrollment(authentication.getName(), body.code());
        return Map.of("status", "TOTP enabled");
    }

    @PostMapping("/totp/verify")
    Map<String, String> verifyTotp(
            @Valid @RequestBody CodeRequest body,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        verifyFactor(totpFactor, body.code(), authentication, request, response);
        return Map.of("status", "TOTP verified");
    }

    @PostMapping("/email/request")
    Map<String, String> requestEmailCode(Authentication authentication) {
        emailFactor.issueCode(authentication.getName());
        return Map.of("status", "Code sent; see application log in this development sample");
    }

    @PostMapping("/email/verify")
    Map<String, String> verifyEmail(
            @Valid @RequestBody CodeRequest body,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        verifyFactor(emailFactor, body.code(), authentication, request, response);
        return Map.of("status", "Email factor verified");
    }

    @PostMapping("/wallet/enroll/challenge")
    Map<String, String> walletEnrollmentChallenge(
            @Valid @RequestBody WalletAddressRequest body, Authentication authentication) {
        return Map.of("message", walletFactor.startEnrollmentChallenge(authentication.getName(), body.address()));
    }

    @PostMapping("/wallet/enroll/verify")
    Map<String, String> enrollWallet(@Valid @RequestBody SignatureRequest body, Authentication authentication) {
        walletFactor.completeEnrollment(authentication.getName(), body.signature());
        return Map.of("status", "Wallet enrolled");
    }

    @PostMapping("/wallet/challenge")
    Map<String, String> walletChallenge(Authentication authentication) {
        return Map.of("message", walletFactor.startLoginChallenge(authentication.getName()));
    }

    @PostMapping("/wallet/verify")
    Map<String, String> verifyWallet(
            @Valid @RequestBody SignatureRequest body,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        verifyFactor(walletFactor, body.signature(), authentication, request, response);
        return Map.of("status", "Wallet factor verified");
    }

    @PostMapping("/recovery-phrase")
    Map<String, String> verifyRecoveryPhrase(
            @Valid @RequestBody RecoveryPhraseRequest body,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        verifyFactor(recoveryPhraseFactor, body.phrase(), authentication, request, response);
        return Map.of("status", "Recovery phrase verified");
    }

    @PostMapping("/recovery-phrase/enroll")
    Map<String, String> enrollRecoveryPhrase(Authentication authentication) {
        String phrase = recoveryPhraseFactor.enroll(authentication.getName());
        return Map.of(
                "phrase", phrase, "warning", "Shown only once. This app recovery phrase is not a wallet seed phrase.");
    }

    private void verifyFactor(
            MfaFactorVerifier verifier,
            String proof,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        verifier.verify(authentication.getName(), proof);
        mfaService.completeFactor(authentication, verifier.factor(), request, response);
    }

    public record CodeRequest(@NotBlank String code) {}

    public record WalletAddressRequest(@NotBlank String address) {}

    public record SignatureRequest(@NotBlank String signature) {}

    public record RecoveryPhraseRequest(@NotBlank String phrase) {}
}
