package com.example.auth.config;

import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.net.URI;

public final class SecurityErrorHandler {
    private static final URI ERROR_TYPE = URI.create("https://errors.example.com/");

    private final JsonMapper jsonMapper;

    public SecurityErrorHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, failure) -> write(
            request, response, HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication is required.");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, failure) -> write(
            request, response, HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to perform this action.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
            HttpStatus status, String title, String detail) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(ERROR_TYPE.resolve(status == HttpStatus.UNAUTHORIZED ? "unauthorized" : "forbidden"));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(status.value());
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        jsonMapper.writeValue(response.getOutputStream(), problem);
    }
}
