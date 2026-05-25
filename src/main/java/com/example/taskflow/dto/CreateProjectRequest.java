package com.example.taskflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// ownerId removed in Module 5 — owner is derived from the authenticated principal
public record CreateProjectRequest(
        @NotBlank @Size(max = 200) String name,
        String description
) {}
