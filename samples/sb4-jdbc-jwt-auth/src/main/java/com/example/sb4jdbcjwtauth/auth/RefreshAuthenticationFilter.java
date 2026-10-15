package com.example.sb4jdbcjwtauth.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFilter;

public class RefreshAuthenticationFilter extends AuthenticationFilter {

    public RefreshAuthenticationFilter(AuthenticationManager authenticationManager,
                                       ApiAuthenticationConverter converter,
                                       RefreshAuthenticationSuccessHandler successHandler,
                                       RefreshAuthenticationFailureHandler failureHandler) {
        super(authenticationManager, converter);
        setRequestMatcher(request -> "PUT".equals(request.getMethod())
                && "/api/auth/refresh".equals(request.getServletPath()));
        setSuccessHandler(successHandler);
        setFailureHandler(failureHandler);
    }
}
