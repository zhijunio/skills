package com.example.sb4jdbcjwtauth.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
public class ApiAuthenticationConverter implements AuthenticationConverter {

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public ApiAuthenticationConverter(JsonMapper jsonMapper, Validator validator) {
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        try {
            if ("/api/auth/login".equals(request.getServletPath())) {
                LoginRequest login = readValid(request, LoginRequest.class);
                var authentication = UsernamePasswordAuthenticationToken.unauthenticated(
                        AuthService.normalizeEmail(login.email()), login.password());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                return authentication;
            }
            if ("/api/auth/refresh".equals(request.getServletPath())) {
                TokenRequest token = readValid(request, TokenRequest.class);
                return RefreshTokenAuthenticationToken.unauthenticated(token.refreshToken());
            }
            throw new AuthenticationServiceException("Unsupported authentication request");
        } catch (AuthenticationServiceException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new AuthenticationServiceException("Invalid authentication request", exception);
        }
    }

    private <T> T readValid(HttpServletRequest request, Class<T> type) throws IOException {
        T body = jsonMapper.readValue(request.getInputStream(), type);
        if (body == null || !validator.validate(body).isEmpty()) {
            throw new AuthenticationServiceException("Invalid authentication request");
        }
        return body;
    }
}
