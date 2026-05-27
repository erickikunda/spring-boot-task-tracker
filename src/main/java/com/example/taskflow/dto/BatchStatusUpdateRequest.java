package com.example.taskflow.dto;

import com.example.taskflow.domain.ProjectStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BatchStatusUpdateRequest(
        @NotEmpty @Size(max = 1000) List<@NotNull UUID> projectIds,
        @NotNull ProjectStatus newStatus
) {}
