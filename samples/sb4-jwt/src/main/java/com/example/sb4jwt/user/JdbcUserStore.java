package com.example.sb4jwt.user;

import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserStore implements UserDetailsService {
    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;

    public JdbcUserStore(JdbcClient jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String username, String password) {
        String normalizedUsername = normalize(username);
        String passwordHash = passwordEncoder.encode(password);
        try {
            jdbc.sql("INSERT INTO app_user (username, password_hash) VALUES (:username, :passwordHash)")
                    .param("username", normalizedUsername)
                    .param("passwordHash", passwordHash)
                    .update();
        } catch (DuplicateKeyException exception) {
            throw new UsernameAlreadyExistsException();
        }
        return new User(normalizedUsername, passwordHash);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return jdbc.sql("SELECT username, password_hash FROM app_user WHERE username = :username")
                .param("username", normalize(username))
                .query((resultSet, row) -> org.springframework.security.core.userdetails.User.withUsername(
                                resultSet.getString("username"))
                        .password(resultSet.getString("password_hash"))
                        .authorities("ROLE_USER")
                        .build())
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private String normalize(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }

    public static class UsernameAlreadyExistsException extends RuntimeException {}
}
