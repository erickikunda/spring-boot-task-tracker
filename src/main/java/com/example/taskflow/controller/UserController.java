package com.example.taskflow.controller;

import com.example.taskflow.dto.CreateUserRequest;
import com.example.taskflow.dto.PageResponse;
import com.example.taskflow.dto.UserResponse;
import com.example.taskflow.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userService.findAll(pageable).map(UserResponse::from));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(userService.findById(id));
    }

    // Admin-only direct user creation. Self-registration goes through POST /api/v1/auth/register.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@RequestBody @Valid CreateUserRequest req) {
        return UserResponse.from(
                userService.create(req.email(), req.displayName(), req.password(), req.role()));
    }
}
