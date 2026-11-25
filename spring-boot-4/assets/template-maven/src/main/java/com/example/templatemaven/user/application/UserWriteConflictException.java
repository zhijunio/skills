package com.example.templatemaven.user.application;

public class UserWriteConflictException extends RuntimeException {

    public UserWriteConflictException(Long id) {
        super("User " + id + " was changed by another request. Reload it and retry.");
    }
}
