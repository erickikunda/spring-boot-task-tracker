package com.example.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// authorId removed in Module 5 — author is derived from the authenticated principal
public record CreateCommentRequest(
        @NotBlank @Size(max = 10_000) String body
) {}
