package com.example.taskflow.controller;

import com.example.taskflow.domain.TaskStatus;
import com.example.taskflow.dto.*;
import com.example.taskflow.mapper.TaskMapper;
import com.example.taskflow.security.UserPrincipal;
import com.example.taskflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskMapper taskMapper;

    @GetMapping
    @PreAuthorize("@projectSecurity.isMember(#projectId, authentication.name) or hasRole('ADMIN')")
    public PageResponse<TaskResponse> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            Pageable pageable) {
        var page = status != null
                ? taskService.findByProjectAndStatus(projectId, status, pageable)
                : taskService.findByProject(projectId, pageable);
        return PageResponse.from(page.map(taskMapper::toResponse));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@projectSecurity.isMember(#projectId, authentication.name) or hasRole('ADMIN')")
    public TaskResponse create(
            @PathVariable UUID projectId,
            @RequestBody @Valid CreateTaskRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return taskMapper.toResponse(taskService.create(
                projectId, principal.user().getId(),
                req.title(), req.description(), req.priority(), req.dueDate()));
    }

    @GetMapping("/{taskId}")
    @PreAuthorize("@projectSecurity.isMember(#projectId, authentication.name) or hasRole('ADMIN')")
    public TaskResponse get(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        return taskMapper.toResponse(taskService.findById(taskId));
    }

    @PatchMapping("/{taskId}/status")
    @PreAuthorize("@projectSecurity.isMember(#projectId, authentication.name) or hasRole('ADMIN')")
    public TaskResponse updateStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @RequestBody @Valid UpdateTaskStatusRequest req) {
        return taskMapper.toResponse(taskService.updateStatus(taskId, req.status()));
    }

    @PutMapping("/{taskId}/assignee")
    @PreAuthorize("@projectSecurity.isMember(#projectId, authentication.name) or hasRole('ADMIN')")
    public TaskResponse assign(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @RequestBody @Valid AssignTaskRequest req) {
        return taskMapper.toResponse(taskService.assign(taskId, req.assigneeId()));
    }

    @DeleteMapping("/{taskId}/assignee")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@projectSecurity.isMember(#projectId, authentication.name) or hasRole('ADMIN')")
    public void unassign(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        taskService.unassign(taskId);
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@projectSecurity.isMember(#projectId, authentication.name) or hasRole('ADMIN')")
    public void delete(@PathVariable UUID projectId, @PathVariable UUID taskId) {
        taskService.delete(taskId);
    }
}
