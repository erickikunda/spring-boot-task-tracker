package com.example.taskflow.dto;

import com.example.taskflow.domain.Priority;
import com.example.taskflow.domain.Task;
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
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getPriority(), task.getDueDate(),
                task.getProject().getId(),
                UserResponse.from(task.getReporter()),
                task.getAssignee() != null ? UserResponse.from(task.getAssignee()) : null,
                task.getCreatedAt());
    }
}
