package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.config.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final RefreshTokenService refreshTokens;
    private final JsonMapper jsonMapper;

    public LoginAuthenticationSuccessHandler(RefreshTokenService refreshTokens, JsonMapper jsonMapper) {
        this.refreshTokens = refreshTokens;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        var tokens = refreshTokens.createForUser(user.id(), request.getHeader("User-Agent"), request.getRemoteAddr());
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), tokens);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain,
            Authentication authentication)
            throws IOException, ServletException {
        onAuthenticationSuccess(request, response, authentication);
    }
}
