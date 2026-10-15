package com.example.sb4jdbcjwtauth.auth;

import com.example.sb4jdbcjwtauth.StringIds;

import com.example.sb4jdbcjwtauth.user.User;
import com.example.sb4jdbcjwtauth.user.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Auth service")
class AuthServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final AuthService service = new AuthService(users, passwordEncoder, events);

    @Test
    @DisplayName("normalizes email and stores an encoded password during registration")
    void registerNormalizesEmailAndStoresEncodedPassword() {
        String userId = StringIds.newId();
        Instant createdAt = Instant.now();
        when(passwordEncoder.encode("strong-password")).thenReturn("encoded-password");
        when(users.create("alex@example.com", "encoded-password"))
                .thenReturn(new User(userId, "alex@example.com", "encoded-password", createdAt));

        UserResponse response = service.register(new RegisterRequest(" Alex@Example.com ", "strong-password"));

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("alex@example.com");
        verify(users).create("alex@example.com", "encoded-password");
        verify(events).publishEvent(any(AuthLifecycleEvent.class));
    }

    @Test
    @DisplayName("reports duplicate email as a domain error")
    void registerConvertsDuplicateEmailToDomainException() {
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(users.create(eq("alex@example.com"), eq("encoded-password")))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThatThrownBy(() -> service.register(new RegisterRequest("alex@example.com", "strong-password")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(events, never()).publishEvent(any(AuthLifecycleEvent.class));
    }
}
