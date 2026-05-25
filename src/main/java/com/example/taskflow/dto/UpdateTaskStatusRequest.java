package com.example.taskflow.dto;

import com.example.taskflow.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {}
