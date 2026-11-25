package com.example.templatemaven.user.application;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("User %d was not found".formatted(id));
    }
}
