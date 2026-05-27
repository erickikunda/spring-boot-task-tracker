package com.example.taskflow.dto;

import com.example.taskflow.domain.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        Role role,
        Instant createdAt
) {}
