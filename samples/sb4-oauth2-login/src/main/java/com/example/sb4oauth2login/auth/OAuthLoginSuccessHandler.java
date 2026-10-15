package com.example.sb4oauth2login.auth;

import com.example.sb4oauth2login.user.AppUser;
import com.example.sb4oauth2login.user.OAuthUserMapper;
import com.example.sb4oauth2login.user.OAuthUserMappingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(OAuthLoginSuccessHandler.class);

    private final OAuthUserMapper users;
    private final OAuthCodeStore codes;

    public OAuthLoginSuccessHandler(OAuthUserMapper users, OAuthCodeStore codes) {
        this.users = users;
        this.codes = codes;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        OAuth2AuthenticationToken auth = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = auth.getPrincipal();
        String registrationId = auth.getAuthorizedClientRegistrationId();
        AppUser user;
        try {
            user = users.map(registrationId, principal);
        } catch (OAuthUserMappingException exception) {
            log.warn("OAuth user mapping was rejected for provider [{}]", registrationId);
            response.sendRedirect(OAuthLoginFailureHandler.FAILURE_REDIRECT_URL);
            return;
        }
        String code = codes.issue(user);
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        response.sendRedirect("/?code=" + code);
    }
}
