package com.example.sb4mfa.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class MfaAuthenticationService {
    private static final Duration RECENT_MFA_TTL = Duration.ofMinutes(5);
    private static final Set<String> PRIMARY_FACTOR_AUTHORITIES =
            Set.of(FactorGrantedAuthority.PASSWORD_AUTHORITY, FactorGrantedAuthority.WEBAUTHN_AUTHORITY);
    private static final Set<String> SECOND_FACTOR_AUTHORITIES = Set.of(
            MfaFactor.EMAIL.authority(),
            MfaFactor.TOTP.authority(),
            MfaFactor.WALLET.authority(),
            MfaFactor.RECOVERY_PHRASE.authority());
    public static final String MFA_VERIFIED_AT_SESSION_ATTRIBUTE = "MFA_VERIFIED_AT";

    private final Clock clock;
    private final SecurityContextRepository contextRepository;

    public MfaAuthenticationService(Clock clock, SecurityContextRepository contextRepository) {
        this.clock = clock;
        this.contextRepository = contextRepository;
    }

    public boolean hasPrimaryFactor(Authentication authentication) {
        return isAuthenticated(authentication)
                && authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(PRIMARY_FACTOR_AUTHORITIES::contains);
    }

    public boolean isMfaComplete(Authentication authentication) {
        return hasPrimaryFactor(authentication)
                && authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(SECOND_FACTOR_AUTHORITIES::contains);
    }

    public void completeFactor(
            Authentication authentication, MfaFactor factor, HttpServletRequest request, HttpServletResponse response) {
        var authorities = new ArrayList<GrantedAuthority>(authentication.getAuthorities());
        var verifiedAuthority = new SimpleGrantedAuthority(factor.authority());
        if (!authorities.contains(verifiedAuthority)) {
            authorities.add(verifiedAuthority);
        }
        var updated = new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), null, authorities);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(updated);
        SecurityContextHolder.setContext(context);
        request.getSession().setAttribute(MFA_VERIFIED_AT_SESSION_ATTRIBUTE, clock.instant());
        contextRepository.saveContext(context, request, response);
    }

    public boolean hasRecentMfa(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object verifiedAt = session.getAttribute(MFA_VERIFIED_AT_SESSION_ATTRIBUTE);
        if (!(verifiedAt instanceof Instant instant)) {
            return false;
        }
        Duration age = Duration.between(instant, clock.instant());
        return !age.isNegative() && age.compareTo(RECENT_MFA_TTL) < 0;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
