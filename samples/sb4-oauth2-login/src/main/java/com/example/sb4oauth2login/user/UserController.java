package com.example.sb4oauth2login.user;

import com.example.sb4oauth2login.oauth.OAuthClientService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserController {
    private final OAuthClientService clients;
    private final InMemoryUserStore users;

    public UserController(OAuthClientService clients, InMemoryUserStore users) {
        this.clients = clients;
        this.users = users;
    }

    @GetMapping("/api/auth/providers")
    Map<String, List<Map<String, Object>>> providers() {
        List<Map<String, Object>> providers = clients.findEnabled().stream()
                .map(provider -> Map.<String, Object>of(
                        "id",
                        provider.registrationId(),
                        "label",
                        provider.loginButtonLabel() == null ? provider.clientName() : provider.loginButtonLabel(),
                        "icon",
                        provider.loginButtonIcon() == null ? "" : provider.loginButtonIcon()))
                .toList();
        return Map.of("providers", providers);
    }

    @GetMapping("/api/me")
    Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "id",
                jwt.getSubject(),
                "username",
                jwt.getClaimAsString("username"),
                "name",
                jwt.getClaimAsString("name"),
                "email",
                jwt.getClaimAsString("email"),
                "roles",
                jwt.getClaimAsString("scope").split(" "),
                "externalProviders",
                users.externalProviders(jwt.getSubject()));
    }

    @DeleteMapping("/api/me/external-identities/{provider}")
    ResponseEntity<Void> unlinkExternalIdentity(@AuthenticationPrincipal Jwt jwt, @PathVariable String provider) {
        return switch (users.unlinkExternalIdentity(jwt.getSubject(), provider)) {
            case REMOVED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "登录方式未关联");
            case LAST_LOGIN_METHOD -> throw new ResponseStatusException(HttpStatus.CONFLICT, "不能解绑唯一登录方式");
        };
    }
}
