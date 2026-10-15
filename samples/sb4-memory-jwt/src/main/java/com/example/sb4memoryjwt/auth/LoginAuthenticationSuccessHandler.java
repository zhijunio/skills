package com.example.sb4memoryjwt.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenIssuer tokenIssuer;
    private final JsonMapper jsonMapper;

    public LoginAuthenticationSuccessHandler(JwtTokenIssuer tokenIssuer, JsonMapper jsonMapper) {
        this.tokenIssuer = tokenIssuer;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        UserDetails user = (UserDetails) authentication.getPrincipal();
        TokenResponse tokens = tokenIssuer.issueFor(user.getUsername());
        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), tokens);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication)
            throws IOException, ServletException {
        onAuthenticationSuccess(request, response, authentication);
    }
}
