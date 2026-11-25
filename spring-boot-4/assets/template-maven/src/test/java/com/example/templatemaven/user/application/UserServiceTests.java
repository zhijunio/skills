package com.example.templatemaven.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.templatemaven.user.api.CreateUserRequest;
import com.example.templatemaven.user.api.UpdateUserRequest;
import com.example.templatemaven.user.domain.UserRegistered;
import com.example.templatemaven.user.persistence.UserEntity;
import com.example.templatemaven.user.persistence.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class UserServiceTests {

    @Test
    void create_persistsNormalizedUserAndPublishesRegistrationEvent() {
        var mapper = org.mockito.Mockito.mock(UserMapper.class);
        var eventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        when(mapper.insert(any(UserEntity.class))).thenAnswer(invocation -> {
            var entity = invocation.getArgument(0, UserEntity.class);
            entity.setId(42L);
            return 1;
        });
        var service = new UserService(mapper, eventPublisher);

        var response = service.create(new CreateUserRequest(" Ada@Example.com ", " Ada "));

        assertEquals(42L, response.id());
        assertEquals("ada@example.com", response.email());
        assertEquals("Ada", response.displayName());
        assertEquals(0, response.version());
        verify(mapper).insert(any(UserEntity.class));
        verify(eventPublisher).publishEvent(new UserRegistered(42L, "ada@example.com", "Ada"));
    }

    @Test
    void findById_throwsNotFoundWhenUserDoesNotExist() {
        var mapper = org.mockito.Mockito.mock(UserMapper.class);
        var eventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        when(mapper.selectById(99L)).thenReturn(null);
        var service = new UserService(mapper, eventPublisher);

        assertThrows(UserNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void update_throwsConflictWhenOptimisticUpdateAffectsNoRows() {
        var mapper = org.mockito.Mockito.mock(UserMapper.class);
        var eventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        when(mapper.updateById(any(UserEntity.class))).thenReturn(0);
        when(mapper.selectById(42L)).thenReturn(new UserEntity());
        var service = new UserService(mapper, eventPublisher);

        assertThrows(
                UserWriteConflictException.class,
                () -> service.update(42L, new UpdateUserRequest("ada@example.com", "Ada", 3)));
    }

    @Test
    void update_throwsNotFoundWhenTargetDoesNotExist() {
        var mapper = org.mockito.Mockito.mock(UserMapper.class);
        var eventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        when(mapper.updateById(any(UserEntity.class))).thenReturn(0);
        when(mapper.selectById(42L)).thenReturn(null);
        var service = new UserService(mapper, eventPublisher);

        assertThrows(
                UserNotFoundException.class,
                () -> service.update(42L, new UpdateUserRequest("ada@example.com", "Ada", 3)));
    }
}
