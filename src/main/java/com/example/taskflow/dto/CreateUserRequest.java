package com.example.taskflow.dto;

import com.example.taskflow.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 2, max = 100) String displayName,
        @NotBlank @Size(min = 8) String password,
        Role role
) {}
