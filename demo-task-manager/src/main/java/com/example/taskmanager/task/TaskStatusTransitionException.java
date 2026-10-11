package com.example.taskmanager.task;

final class TaskStatusTransitionException extends RuntimeException {

    TaskStatusTransitionException(TaskStatus current, TaskStatus requested) {
        super("Cannot change task status from %s to %s".formatted(current, requested));
    }
}
