package com.example.sb4oauth2login.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import tools.jackson.databind.json.JsonMapper;

public class LoginAuthenticationConverter implements AuthenticationConverter {
    private final JsonMapper jsonMapper;

    public LoginAuthenticationConverter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod()) || !"/api/auth/login".equals(request.getServletPath()))
            return null;
        if (request.getContentType() == null
                || !request.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            throw new BadCredentialsException("Invalid credentials");
        }
        try {
            LoginRequest body = jsonMapper.readValue(request.getInputStream(), LoginRequest.class);
            if (body.username() == null
                    || body.username().isBlank()
                    || body.password() == null
                    || body.password().isBlank()) {
                throw new BadCredentialsException("Invalid credentials");
            }
            return UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password());
        } catch (IOException e) {
            throw new BadCredentialsException("Invalid credentials", e);
        }
    }
}
