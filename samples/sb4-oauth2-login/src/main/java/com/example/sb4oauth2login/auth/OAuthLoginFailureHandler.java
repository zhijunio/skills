package com.example.sb4oauth2login.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {
    static final String FAILURE_REDIRECT_URL = "/?oauthError=1";

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        response.sendRedirect(FAILURE_REDIRECT_URL);
    }
}
