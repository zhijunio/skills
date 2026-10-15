package com.example.sb4jdbcjwtauth.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationFilter;

public class LoginAuthenticationFilter extends AuthenticationFilter {

    public LoginAuthenticationFilter(AuthenticationManager authenticationManager,
                                     ApiAuthenticationConverter converter,
                                     LoginAuthenticationSuccessHandler successHandler,
                                     LoginAuthenticationFailureHandler failureHandler) {
        super(authenticationManager, converter);
        setRequestMatcher(request -> "POST".equals(request.getMethod())
                && "/api/auth/login".equals(request.getServletPath()));
        setSuccessHandler(successHandler);
        setFailureHandler(failureHandler);
    }
}
