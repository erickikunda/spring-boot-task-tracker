package com.example.taskflow.controller;

import com.example.taskflow.dto.CreateUserRequest;
import com.example.taskflow.dto.PageResponse;
import com.example.taskflow.dto.UpdateRoleRequest;
import com.example.taskflow.dto.UserResponse;
import com.example.taskflow.mapper.UserMapper;
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
    private final UserMapper userMapper;

    @GetMapping
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userService.findAll(pageable).map(userMapper::toResponse));
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return userMapper.toResponse(userService.findById(id));
    }

    // Admin-only direct user creation. Self-registration goes through POST /api/v1/auth/register.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@RequestBody @Valid CreateUserRequest req) {
        return userMapper.toResponse(
                userService.create(req.email(), req.displayName(), req.password(), req.role()));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateRole(@PathVariable UUID id, @RequestBody @Valid UpdateRoleRequest req) {
        return userMapper.toResponse(userService.updateRole(id, req.role()));
    }
}
