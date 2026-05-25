package com.example.taskflow.dto;

import com.example.taskflow.domain.Project;
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
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(), project.getName(), project.getDescription(),
                project.getStatus(), UserResponse.from(project.getOwner()),
                project.getCreatedAt());
    }
}
