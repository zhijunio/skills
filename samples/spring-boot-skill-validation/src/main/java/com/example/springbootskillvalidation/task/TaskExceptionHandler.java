package com.example.springbootskillvalidation.task;

import java.net.URI;
import java.util.Objects;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class TaskExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleTaskNotFound(TaskNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://errors.example.com/task-not-found"));
        problem.setTitle("Task Not Found");
        return problem;
    }

    @ExceptionHandler({TaskVersionConflictException.class, OptimisticLockingFailureException.class})
    ProblemDetail handleConcurrentModification(RuntimeException exception) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The task changed since it was read. Reload it before updating.");
        problem.setType(URI.create("https://errors.example.com/concurrent-modification"));
        problem.setTitle("Concurrent Modification");
        return problem;
    }

    @ExceptionHandler(InvalidTaskSortException.class)
    ProblemDetail handleInvalidSort(InvalidTaskSortException exception) {
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Sort by title, createdAt, updatedAt, or completed.");
        problem.setType(URI.create("https://errors.example.com/invalid-sort"));
        problem.setTitle("Invalid Sort Field");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed.");
        problem.setType(URI.create("https://errors.example.com/validation"));
        problem.setTitle("Validation Failed");
        problem.setProperty(
                "violations",
                exception.getBindingResult().getFieldErrors().stream()
                        .map(this::toViolation)
                        .toList());
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    private FieldViolation toViolation(FieldError error) {
        return new FieldViolation(
                error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"));
    }

    private record FieldViolation(String field, String message) {}
}
