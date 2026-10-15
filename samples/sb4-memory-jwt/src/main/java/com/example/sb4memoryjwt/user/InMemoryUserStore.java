package com.example.sb4memoryjwt.user;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InMemoryUserStore implements UserDetailsService {
    private final ConcurrentMap<String, User> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    public InMemoryUserStore(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public User register(String username, String password) {
        String key = normalize(username);
        User user = new User(key, passwordEncoder.encode(password));
        if (users.putIfAbsent(key, user) != null) {
            throw new UsernameAlreadyExistsException();
        }
        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = users.get(normalize(username));
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return org.springframework.security.core.userdetails.User.withUsername(user.username())
                .password(user.passwordHash())
                .authorities("ROLE_USER")
                .build();
    }

    private String normalize(String username) {
        return username.strip().toLowerCase(Locale.ROOT);
    }

    public static class UsernameAlreadyExistsException extends RuntimeException {}
}
