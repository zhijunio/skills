package com.example.sb4oauth2login.user;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InMemoryUserStore {
    private record ExternalIdentity(String registrationId, String subject) {}

    public enum UnlinkResult {
        REMOVED,
        NOT_FOUND,
        LAST_LOGIN_METHOD
    }

    public enum ActiveResult {
        UPDATED,
        NOT_FOUND,
        LAST_ACTIVE_ADMIN
    }

    private final Map<String, AppUser> users = new ConcurrentHashMap<>();
    private final Map<ExternalIdentity, AppUser> externalUsers = new ConcurrentHashMap<>();

    @Autowired
    public InMemoryUserStore(
            PasswordEncoder encoder,
            @Value("${app.security.demo-password}") String password,
            @Value("${app.security.admin-password}") String adminPassword) {
        AppUser demo = new AppUser("local-demo", "demo", "示例用户", "demo@example.test", encoder.encode(password), true);
        AppUser admin = new AppUser(
                "local-admin",
                "admin",
                "管理员",
                "admin@example.test",
                encoder.encode(adminPassword),
                true,
                Set.of("USER", "ADMIN"));
        users.put(demo.username(), demo);
        users.put(admin.username(), admin);
    }

    public InMemoryUserStore(PasswordEncoder encoder, String password) {
        this(encoder, password, password);
    }

    public Optional<AppUser> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public Optional<AppUser> findById(String id) {
        return users.values().stream().filter(user -> user.id().equals(id)).findFirst();
    }

    public synchronized List<AppUser> findAll() {
        return users.values().stream()
                .sorted(Comparator.comparing(AppUser::username))
                .toList();
    }

    public synchronized AppUser findOrCreateExternal(String provider, String subject, String name, String email) {
        ExternalIdentity identity = new ExternalIdentity(provider, subject);
        return externalUsers.compute(identity, (ignored, existing) -> {
            if (existing != null) {
                if (!existing.active()) {
                    return existing;
                }
                AppUser updated = new AppUser(
                        existing.id(),
                        existing.username(),
                        hasText(name) ? name : existing.displayName(),
                        hasText(email) ? email : existing.email(),
                        existing.passwordHash(),
                        existing.active(),
                        existing.roles());
                users.put(updated.username(), updated);
                return updated;
            }
            String userId = UUID.randomUUID().toString();
            AppUser user = new AppUser(
                    userId,
                    "oauth-" + userId,
                    hasText(name) ? name : provider + " 用户",
                    hasText(email) ? email : "",
                    "",
                    true,
                    Set.of("USER"));
            users.put(user.username(), user);
            return user;
        });
    }

    public synchronized ActiveResult setActive(String userId, boolean active) {
        AppUser user = findById(userId).orElse(null);
        if (user == null) {
            return ActiveResult.NOT_FOUND;
        }
        if (!active
                && user.active()
                && user.roles().contains("ADMIN")
                && users.values().stream()
                                .filter(AppUser::active)
                                .filter(candidate -> candidate.roles().contains("ADMIN"))
                                .count()
                        == 1) {
            return ActiveResult.LAST_ACTIVE_ADMIN;
        }
        AppUser updated = new AppUser(
                user.id(),
                user.username(),
                user.displayName(),
                user.email(),
                user.passwordHash(),
                active,
                user.roles());
        users.put(updated.username(), updated);
        externalUsers.replaceAll((identity, linkedUser) -> linkedUser.id().equals(userId) ? updated : linkedUser);
        return ActiveResult.UPDATED;
    }

    public Set<String> externalProviders(String userId) {
        return externalUsers.entrySet().stream()
                .filter(entry -> entry.getValue().id().equals(userId))
                .map(entry -> entry.getKey().registrationId())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public synchronized UnlinkResult unlinkExternalIdentity(String userId, String registrationId) {
        return unlinkExternalIdentity(userId, registrationId, false);
    }

    public synchronized UnlinkResult adminUnlinkExternalIdentity(String userId, String registrationId) {
        return unlinkExternalIdentity(userId, registrationId, true);
    }

    private UnlinkResult unlinkExternalIdentity(String userId, String registrationId, boolean allowLastMethod) {
        Map.Entry<ExternalIdentity, AppUser> target = externalUsers.entrySet().stream()
                .filter(entry -> entry.getValue().id().equals(userId)
                        && entry.getKey().registrationId().equals(registrationId))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return UnlinkResult.NOT_FOUND;
        }
        AppUser user = target.getValue();
        long identityCount = externalUsers.values().stream()
                .filter(linked -> linked.id().equals(userId))
                .count();
        boolean hasLocalPassword =
                user.passwordHash() != null && !user.passwordHash().isBlank();
        if (identityCount == 1 && !hasLocalPassword && !allowLastMethod) {
            return UnlinkResult.LAST_LOGIN_METHOD;
        }
        externalUsers.remove(target.getKey());
        return UnlinkResult.REMOVED;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
