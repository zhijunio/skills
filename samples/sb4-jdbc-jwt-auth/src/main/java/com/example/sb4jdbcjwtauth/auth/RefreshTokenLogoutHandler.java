package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.config.ProblemResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;

@Component
public class RefreshTokenLogoutHandler implements LogoutHandler {

    public static final String INVALID_REQUEST_ATTRIBUTE = RefreshTokenLogoutHandler.class.getName() + ".INVALID_REQUEST";

    private final JsonMapper jsonMapper;
    private final Validator validator;
    private final RefreshTokenService refreshTokens;

    public RefreshTokenLogoutHandler(JsonMapper jsonMapper, Validator validator,
                                     RefreshTokenService refreshTokens) {
        this.jsonMapper = jsonMapper;
        this.validator = validator;
        this.refreshTokens = refreshTokens;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        TokenRequest tokenRequest;
        try {
            tokenRequest = jsonMapper.readValue(request.getInputStream(), TokenRequest.class);
        } catch (IOException | RuntimeException exception) {
            writeInvalidRequest(request, response);
            return;
        }
        if (tokenRequest == null || !validator.validate(tokenRequest).isEmpty()) {
            writeInvalidRequest(request, response);
            return;
        }
        refreshTokens.revoke(tokenRequest.refreshToken());
    }

    private void writeInvalidRequest(HttpServletRequest request, HttpServletResponse response) {
        request.setAttribute(INVALID_REQUEST_ATTRIBUTE, Boolean.TRUE);
        try {
            ProblemResponses.write(jsonMapper, response, HttpStatus.BAD_REQUEST,
                    "Invalid logout request", "A valid refreshToken is required.");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
