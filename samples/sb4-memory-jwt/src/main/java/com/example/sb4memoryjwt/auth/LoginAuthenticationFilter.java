package com.example.sb4memoryjwt.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFilter;

public class LoginAuthenticationFilter extends AuthenticationFilter {
    public LoginAuthenticationFilter(
            AuthenticationManager authenticationManager,
            ApiAuthenticationConverter converter,
            LoginAuthenticationSuccessHandler successHandler,
            LoginAuthenticationFailureHandler failureHandler) {
        super(authenticationManager, converter);
        setRequestMatcher(request -> "POST".equals(request.getMethod())
                && (request.getContextPath() + "/api/auth/login").equals(request.getRequestURI()));
        setSuccessHandler(successHandler);
        setFailureHandler(failureHandler);
    }
}
