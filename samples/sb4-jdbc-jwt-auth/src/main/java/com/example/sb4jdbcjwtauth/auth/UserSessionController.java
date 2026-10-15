package com.example.sb4jdbcjwtauth.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class UserSessionController {

    private final UserSessionService userSessions;

    public UserSessionController(UserSessionService userSessions) {
        this.userSessions = userSessions;
    }

    @GetMapping
    public List<UserSessionResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return userSessions.list(jwt.getSubject(), sessionId(jwt));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable String sessionId) {
        if (!userSessions.revoke(jwt.getSubject(), sessionId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private String sessionId(Jwt jwt) {
        return jwt.getClaimAsString("sid");
    }
}
