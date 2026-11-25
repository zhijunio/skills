package com.example.templatemaven.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UserTests {

    @Test
    void create_normalizesEmailAndTrimsDisplayName() {
        var user = User.create("  Ada@Example.com ", " Ada Lovelace ");

        assertEquals("ada@example.com", user.email());
        assertEquals("Ada Lovelace", user.displayName());
    }
}
