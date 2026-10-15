package com.example.sb4oauth2login.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final InMemoryUserStore users;

    public AdminUserController(InMemoryUserStore users) {
        this.users = users;
    }

    @GetMapping
    public List<AdminUserSummary> findAll(@AuthenticationPrincipal Jwt actor) {
        return users.findAll().stream()
                .map(user -> new AdminUserSummary(
                        user.id(),
                        user.username(),
                        user.displayName(),
                        user.email(),
                        user.active(),
                        user.id().equals(actor.getSubject()),
                        user.passwordHash() != null && !user.passwordHash().isBlank(),
                        user.roles(),
                        users.externalProviders(user.id()).stream().sorted().toList()))
                .toList();
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<Void> setActive(
            @PathVariable String userId,
            @Valid @RequestBody UserStatusInput input,
            @AuthenticationPrincipal Jwt actor) {
        if (!input.active() && userId.equals(actor.getSubject())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "不能停用当前登录的管理员账号");
        }
        return switch (users.setActive(userId, input.active())) {
            case UPDATED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
            case LAST_ACTIVE_ADMIN -> throw new ResponseStatusException(HttpStatus.CONFLICT, "不能停用最后一个有效管理员");
        };
    }

    @DeleteMapping("/{userId}/external-identities/{provider}")
    public ResponseEntity<Void> unlinkProvider(@PathVariable String userId, @PathVariable String provider) {
        return switch (users.adminUnlinkExternalIdentity(userId, provider)) {
            case REMOVED -> ResponseEntity.noContent().build();
            case NOT_FOUND -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户未绑定此提供方");
            case LAST_LOGIN_METHOD -> throw new ResponseStatusException(HttpStatus.CONFLICT, "不能解绑用户唯一的登录方式");
        };
    }

    public record UserStatusInput(@NotNull Boolean active) {}
}
