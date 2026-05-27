package com.example.taskflow.dto;

import com.example.taskflow.domain.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {}
