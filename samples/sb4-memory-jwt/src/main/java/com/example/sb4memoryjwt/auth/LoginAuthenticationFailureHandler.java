package com.example.sb4memoryjwt.auth;

import com.example.sb4memoryjwt.config.SecurityProblemDetailHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

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
        problemDetailHandler.write(
                response, HttpStatus.UNAUTHORIZED, "Authentication failed", "The username or password is incorrect.");
    }
}
