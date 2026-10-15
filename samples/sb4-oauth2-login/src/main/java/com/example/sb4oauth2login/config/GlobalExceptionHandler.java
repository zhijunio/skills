package com.example.sb4oauth2login.config;

import com.example.sb4oauth2login.oauth.OAuthClientAlreadyExistsException;
import com.example.sb4oauth2login.oauth.OAuthClientNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final URI ERROR_TYPE = URI.create("https://example.com/problems/");

    @ExceptionHandler(OAuthClientAlreadyExistsException.class)
    ProblemDetail handleAlreadyExists(OAuthClientAlreadyExistsException exception) {
        return problem(
                HttpStatus.CONFLICT, "OAuth client already exists", exception.getMessage(), "oauth-client-exists");
    }

    @ExceptionHandler(OAuthClientNotFoundException.class)
    ProblemDetail handleNotFound(OAuthClientNotFoundException exception) {
        return problem(
                HttpStatus.NOT_FOUND, "OAuth client not found", exception.getMessage(), "oauth-client-not-found");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleInvalidConfiguration(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage(), "invalid-request");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(ERROR_TYPE.resolve(type));
        return problem;
    }
}
