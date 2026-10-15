package com.example.sb4memoryjwt.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import java.io.IOException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import tools.jackson.databind.json.JsonMapper;

public class ApiAuthenticationConverter implements AuthenticationConverter {
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public ApiAuthenticationConverter(JsonMapper jsonMapper, Validator validator) {
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!"POST".equals(request.getMethod()) || !"/api/auth/login".equals(path)) {
            return null;
        }
        Credentials credentials;
        try {
            credentials = jsonMapper.readValue(request.getInputStream(), Credentials.class);
        } catch (IOException exception) {
            throw new BadCredentialsException("Invalid credentials request", exception);
        }
        if (!validator.validate(credentials).isEmpty()) {
            throw new BadCredentialsException("Invalid credentials request");
        }
        return UsernamePasswordAuthenticationToken.unauthenticated(credentials.username(), credentials.password());
    }
}
