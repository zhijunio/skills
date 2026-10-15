package com.example.sb4memoryjwt.auth;

import com.example.sb4memoryjwt.user.InMemoryUserStore;
import com.example.sb4memoryjwt.user.User;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final InMemoryUserStore users;

    public AuthService(InMemoryUserStore users) {
        this.users = users;
    }

    public UserResponse register(String username, String password) {
        User user = users.register(username, password);
        return new UserResponse(user.username());
    }
}
