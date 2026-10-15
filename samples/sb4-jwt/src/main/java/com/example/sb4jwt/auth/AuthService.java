package com.example.sb4jwt.auth;

import com.example.sb4jwt.user.JdbcUserStore;
import com.example.sb4jwt.user.User;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final JdbcUserStore users;

    public AuthService(JdbcUserStore users) {
        this.users = users;
    }

    public UserResponse register(String username, String password) {
        User user = users.register(username, password);
        return new UserResponse(user.username());
    }
}
