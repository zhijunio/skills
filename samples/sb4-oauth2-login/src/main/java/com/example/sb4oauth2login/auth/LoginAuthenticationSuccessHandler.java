package com.example.sb4oauth2login.auth;

import com.example.sb4oauth2login.user.AppUser;
import com.example.sb4oauth2login.user.InMemoryUserStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final TokenService tokens;
    private final InMemoryUserStore users;
    private final JsonMapper jsonMapper;

    public LoginAuthenticationSuccessHandler(TokenService tokens, InMemoryUserStore users, JsonMapper jsonMapper) {
        this.tokens = tokens;
        this.users = users;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        AppUser user = users.findByUsername(username).orElseThrow();
        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), tokens.issue(user));
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication)
            throws IOException, ServletException {
        onAuthenticationSuccess(request, response, authentication);
    }
}
