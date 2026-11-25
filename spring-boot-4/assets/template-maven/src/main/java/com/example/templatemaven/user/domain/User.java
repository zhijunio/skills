package com.example.templatemaven.user.domain;

public record User(String email, String displayName) {

    public static User create(String email, String displayName) {
        return new User(email.trim().toLowerCase(), displayName.trim());
    }
}
