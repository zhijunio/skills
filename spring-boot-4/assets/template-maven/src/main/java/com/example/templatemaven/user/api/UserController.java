package com.example.templatemaven.user.api;

import com.example.templatemaven.user.application.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
class UserController {

    private final UserService userService;

    UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lists user accounts ordered by their stable identifier.
     *
     * @return the requested page of users
     */
    @GetMapping
    Page<UserResponse> list(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "{pagination.page.min}") int page,
            @RequestParam(defaultValue = "20")
                    @Min(value = 1, message = "{pagination.size.min}")
                    @Max(value = 100, message = "{pagination.size.max}")
                    int size) {
        return userService.list(page, size);
    }

    /**
     * Creates a user account.
     *
     * @return the created user and its initial optimistic-lock version
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    /**
     * Returns a user account by its identifier.
     *
     * @return the requested user
     */
    @GetMapping("/{id}")
    UserResponse findById(@PathVariable @Positive Long id) {
        return userService.findById(id);
    }

    /**
     * Updates a user account if the supplied version is current.
     *
     * @return the updated user with its incremented version
     */
    @PutMapping("/{id}")
    UserResponse update(@PathVariable @Positive Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }
}
