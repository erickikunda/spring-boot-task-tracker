package com.example.taskflow.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String body,
        UserResponse author,
        Instant createdAt
) {}
