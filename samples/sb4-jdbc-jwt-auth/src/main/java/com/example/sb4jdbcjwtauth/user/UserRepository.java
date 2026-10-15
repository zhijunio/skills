package com.example.sb4jdbcjwtauth.user;

import com.example.sb4jdbcjwtauth.StringIds;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<User> findByEmail(String email) {
        return jdbc.sql("SELECT id, email, password_hash AS password, created_at FROM users WHERE email = :email")
                .param("email", email)
                .query(User.class)
                .optional();
    }

    public Optional<User> findById(String id) {
        return jdbc.sql("SELECT id, email, password_hash AS password, created_at FROM users WHERE id = :id")
                .param("id", id)
                .query(User.class)
                .optional();
    }

    public User create(String email, String password) {
        String id = StringIds.newId();
        return jdbc.sql("""
                        INSERT INTO users (id, email, password_hash)
                        VALUES (:id, :email, :password)
                        RETURNING id, email, password_hash AS password, created_at
                        """)
                .param("id", id)
                .param("email", email)
                .param("password", password)
                .query(User.class)
                .single();
    }
}
