package com.example.templatemaven.architecturefixture.api;

import com.example.templatemaven.user.persistence.UserMapper;

/** Test fixture that intentionally violates the API-to-persistence boundary. */
public class IllegalApiFixture {

    private final UserMapper userMapper;

    public IllegalApiFixture(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserMapper userMapper() {
        return userMapper;
    }
}
