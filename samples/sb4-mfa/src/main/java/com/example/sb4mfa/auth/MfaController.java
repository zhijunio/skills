package com.example.sb4mfa.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MfaController {
    private final MfaAuthenticationService mfaService;

    public MfaController(MfaAuthenticationService mfaService) {
        this.mfaService = mfaService;
    }

    @GetMapping("/secure")
    Map<String, Object> secure(Authentication authentication) {
        return Map.of(
                "message",
                "MFA complete",
                "username",
                authentication.getName(),
                "authorities",
                authentication.getAuthorities());
    }

    @GetMapping("/users/me")
    Map<String, Object> currentUser(Authentication authentication) {
        return Map.of("username", authentication.getName(), "message", "Profile data available");
    }

    @GetMapping("/account/security")
    Map<String, Object> accountSecurity(Authentication authentication, HttpServletRequest request) {
        return Map.of(
                "username",
                authentication.getName(),
                "message",
                "Recent MFA verification is valid",
                "mfaVerifiedAt",
                request.getSession(false).getAttribute(MfaAuthenticationService.MFA_VERIFIED_AT_SESSION_ATTRIBUTE));
    }

    @GetMapping("/csrf")
    Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/session")
    Map<String, Object> session(Authentication authentication) {
        boolean authenticated = mfaService.hasPrimaryFactor(authentication);
        boolean mfaComplete = mfaService.isMfaComplete(authentication);
        return Map.of(
                "authenticated",
                authenticated,
                "mfaComplete",
                mfaComplete,
                "username",
                authenticated ? authentication.getName() : "",
                "authorities",
                authenticated ? authentication.getAuthorities() : List.of());
    }

    @GetMapping("/passkey-secure")
    Map<String, String> passkeySecure(Authentication authentication) {
        boolean usedPasskey = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(FactorGrantedAuthority.WEBAUTHN_AUTHORITY));
        if (!usedPasskey) {
            throw new org.springframework.security.access.AccessDeniedException("A passkey login is required");
        }
        return Map.of("message", "Authenticated with a passkey", "username", authentication.getName());
    }
}
