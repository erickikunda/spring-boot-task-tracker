package com.example.taskflow.dto;

import com.example.taskflow.domain.Priority;
import com.example.taskflow.domain.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        LocalDate dueDate,
        UUID projectId,
        UserResponse reporter,
        UserResponse assignee,
        Instant createdAt
) {}
