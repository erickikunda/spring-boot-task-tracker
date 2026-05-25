package com.example.taskflow.dto;

import com.example.taskflow.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// reporterId removed in Module 5 — reporter is derived from the authenticated principal
public record CreateTaskRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        Priority priority,
        LocalDate dueDate
) {}
