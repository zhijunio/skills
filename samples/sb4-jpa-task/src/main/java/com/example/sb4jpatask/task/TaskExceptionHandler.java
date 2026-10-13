package com.example.sb4jpatask.task;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

@RestControllerAdvice(basePackageClasses = TaskService.class)
class TaskExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleNotFound(TaskNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("https://example.com/problems/task-not-found"));
        problem.setTitle("Task not found");
        return problem;
    }

    @ExceptionHandler(TaskStatusTransitionException.class)
    ProblemDetail handleInvalidTransition(TaskStatusTransitionException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("https://example.com/problems/invalid-task-status-transition"));
        problem.setTitle("Invalid task status transition");
        return problem;
    }

    @ExceptionHandler({StaleTaskVersionException.class, OptimisticLockingFailureException.class})
    ProblemDetail handleStaleVersion(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The task changed since it was read. Fetch the latest task and retry.");
        problem.setType(URI.create("https://example.com/problems/stale-task-version"));
        problem.setTitle("Task version conflict");
        return problem;
    }
}
