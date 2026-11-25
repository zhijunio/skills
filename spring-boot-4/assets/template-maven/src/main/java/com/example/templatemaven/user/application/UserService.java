package com.example.templatemaven.user.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.templatemaven.user.api.CreateUserRequest;
import com.example.templatemaven.user.api.UpdateUserRequest;
import com.example.templatemaven.user.api.UserResponse;
import com.example.templatemaven.user.domain.User;
import com.example.templatemaven.user.domain.UserRegistered;
import com.example.templatemaven.user.persistence.UserEntity;
import com.example.templatemaven.user.persistence.UserMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserMapper userMapper, ApplicationEventPublisher eventPublisher) {
        this.userMapper = userMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserResponse create(CreateUserRequest request) {
        var user = User.create(request.email(), request.displayName());
        var entity = new UserEntity();
        entity.setEmail(user.email());
        entity.setDisplayName(user.displayName());
        entity.setVersion(0);
        try {
            userMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw new UserAlreadyExistsException(user.email());
        }
        eventPublisher.publishEvent(new UserRegistered(entity.getId(), entity.getEmail(), entity.getDisplayName()));
        return toResponse(entity);
    }

    @CacheEvict(cacheNames = "users", key = "#id")
    @Transactional(rollbackFor = Exception.class)
    public UserResponse update(Long id, UpdateUserRequest request) {
        var user = User.create(request.email(), request.displayName());
        var entity = new UserEntity();
        entity.setId(id);
        entity.setEmail(user.email());
        entity.setDisplayName(user.displayName());
        entity.setVersion(request.version());
        if (userMapper.updateById(entity) == 0) {
            if (userMapper.selectById(id) == null) {
                throw new UserNotFoundException(id);
            }
            throw new UserWriteConflictException(id);
        }
        return toResponse(entity);
    }

    @Cacheable(cacheNames = "users", key = "#id")
    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public UserResponse findById(Long id) {
        var entity = userMapper.selectById(id);
        if (entity == null) {
            throw new UserNotFoundException(id);
        }
        return toResponse(entity);
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public org.springframework.data.domain.Page<UserResponse> list(int page, int size) {
        var pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var databasePage = new Page<UserEntity>(pageRequest.getPageNumber() + 1L, pageRequest.getPageSize());
        var result =
                userMapper.selectPage(databasePage, new LambdaQueryWrapper<UserEntity>().orderByAsc(UserEntity::getId));
        var content = result.getRecords().stream().map(this::toResponse).toList();
        return new PageImpl<>(content, pageRequest, result.getTotal());
    }

    private UserResponse toResponse(UserEntity entity) {
        return new UserResponse(
                entity.getId(), entity.getEmail(), entity.getDisplayName(), entity.getVersion(), entity.getCreatedAt());
    }
}
