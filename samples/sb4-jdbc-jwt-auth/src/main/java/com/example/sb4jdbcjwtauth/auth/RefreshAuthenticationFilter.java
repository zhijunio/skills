package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.config.ProblemResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

public class RefreshAuthenticationFilter extends AuthenticationFilter {

    public RefreshAuthenticationFilter(AuthenticationManager authenticationManager,
                                       ApiAuthenticationConverter converter,
                                       JsonMapper jsonMapper) {
        super(authenticationManager, converter);
        setRequestMatcher(request -> "PUT".equals(request.getMethod())
                && "/api/auth/refresh".equals(request.getServletPath()));
        setSuccessHandler((request, response, authentication) -> {
            var token = (RefreshTokenAuthenticationToken) authentication;
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            jsonMapper.writeValue(response.getOutputStream(), token.tokenResponse());
        });
        setFailureHandler((request, response, exception) ->
                ProblemResponses.write(jsonMapper, response, HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token", "The refresh token is invalid, expired, or has already been used."));
    }
}
