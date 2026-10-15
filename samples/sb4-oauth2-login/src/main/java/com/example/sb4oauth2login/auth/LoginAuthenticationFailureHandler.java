package com.example.sb4oauth2login.auth;

import com.example.sb4oauth2login.config.SecurityProblemDetailHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final SecurityProblemDetailHandler problems;

    public LoginAuthenticationFailureHandler(SecurityProblemDetailHandler problems) {
        this.problems = problems;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException {
        problems.write(response, HttpStatus.UNAUTHORIZED, "invalid-credentials", "登录失败", "用户名或密码错误");
    }
}
