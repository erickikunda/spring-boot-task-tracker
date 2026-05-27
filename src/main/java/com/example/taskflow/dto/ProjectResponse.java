package com.example.taskflow.dto;

import com.example.taskflow.domain.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        ProjectStatus status,
        UserResponse owner,
        Instant createdAt
) {}
