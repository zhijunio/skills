package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.config.ProblemResponses;
import com.example.sb4jdbcjwtauth.config.SecurityUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

public class LoginAuthenticationFilter extends AuthenticationFilter {

    public LoginAuthenticationFilter(AuthenticationManager authenticationManager,
                                     ApiAuthenticationConverter converter,
                                     RefreshTokenService refreshTokens,
                                     JsonMapper jsonMapper) {
        super(authenticationManager, converter);
        setRequestMatcher(request -> "POST".equals(request.getMethod())
                && "/api/auth/login".equals(request.getServletPath()));
        setSuccessHandler((request, response, authentication) -> {
            SecurityUser user = (SecurityUser) authentication.getPrincipal();
            var tokens = refreshTokens.createForUser(user.id(), request.getHeader("User-Agent"), request.getRemoteAddr());
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            jsonMapper.writeValue(response.getOutputStream(), tokens);
        });
        setFailureHandler((request, response, exception) ->
            ProblemResponses.write(jsonMapper, response, HttpStatus.UNAUTHORIZED,
                    "Authentication failed", "The email or password is incorrect."));
    }
}
