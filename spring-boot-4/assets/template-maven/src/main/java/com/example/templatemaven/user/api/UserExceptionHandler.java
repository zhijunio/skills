package com.example.templatemaven.user.api;

import com.example.templatemaven.user.application.UserAlreadyExistsException;
import com.example.templatemaven.user.application.UserNotFoundException;
import com.example.templatemaven.user.application.UserWriteConflictException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class UserExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected org.springframework.http.ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = validationProblem(status, exception.getBindingResult().getFieldErrors());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected org.springframework.http.ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, "One or more request values are invalid.");
        problem.setTitle("Invalid request");
        problem.setProperty(
                "errors",
                exception.getParameterValidationResults().stream()
                        .flatMap(result -> result.getResolvableErrors().stream()
                                .map(error -> Map.of(
                                        "field",
                                        result.getMethodParameter().getParameterName() == null
                                                ? "request"
                                                : result.getMethodParameter().getParameterName(),
                                        "message",
                                        error.getDefaultMessage() == null
                                                ? "Invalid value"
                                                : error.getDefaultMessage())))
                        .toList());
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail userNotFound(UserNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("User not found");
        return problem;
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    ProblemDetail userAlreadyExists(UserAlreadyExistsException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("User already exists");
        return problem;
    }

    @ExceptionHandler(UserWriteConflictException.class)
    ProblemDetail userWriteConflict(UserWriteConflictException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("User update conflict");
        return problem;
    }

    private ProblemDetail validationProblem(HttpStatusCode status, List<FieldError> fieldErrors) {
        var problem = ProblemDetail.forStatusAndDetail(status, "One or more request values are invalid.");
        problem.setTitle("Invalid request");
        problem.setProperty(
                "errors",
                fieldErrors.stream()
                        .map(error -> Map.of(
                                "field",
                                error.getField(),
                                "message",
                                error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                        .toList());
        return problem;
    }
}
