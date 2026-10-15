package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.config.SecurityProblemDetailHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final SecurityProblemDetailHandler problemDetailHandler;

    public LoginAuthenticationFailureHandler(SecurityProblemDetailHandler problemDetailHandler) {
        this.problemDetailHandler = problemDetailHandler;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        problemDetailHandler.write(response, HttpStatus.UNAUTHORIZED,
                "Authentication failed", "The email or password is incorrect.");
    }
}
