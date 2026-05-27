package com.example.taskflow.controller;

import com.example.taskflow.dto.BatchStatusUpdateRequest;
import com.example.taskflow.dto.BatchUpdateResult;
import com.example.taskflow.dto.CreateProjectRequest;
import com.example.taskflow.dto.ProjectResponse;
import com.example.taskflow.dto.UserResponse;
import com.example.taskflow.mapper.ProjectMapper;
import com.example.taskflow.mapper.UserMapper;
import com.example.taskflow.security.UserPrincipal;
import com.example.taskflow.service.ProjectService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;

    @GetMapping
    public List<ProjectResponse> listForCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return projectService.findByMember(principal.user())
                .stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@RequestBody @Valid CreateProjectRequest req,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        return projectMapper.toResponse(
                projectService.create(req.name(), req.description(), principal.user()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.isMember(#id, authentication.name) or hasRole('ADMIN')")
    public ProjectResponse get(@PathVariable UUID id) {
        return projectMapper.toResponse(projectService.findById(id));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("@projectSecurity.isMember(#id, authentication.name) or hasRole('ADMIN')")
    public List<UserResponse> members(@PathVariable UUID id) {
        return projectService.findMembers(id)
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @PostMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@projectSecurity.isOwner(#id, authentication.name) or hasRole('ADMIN')")
    public void addMember(@PathVariable UUID id, @PathVariable UUID userId) {
        projectService.addMember(id, userId);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@projectSecurity.isOwner(#id, authentication.name) or hasRole('ADMIN')")
    public void removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        projectService.removeMember(id, userId);
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("@projectSecurity.isOwner(#id, authentication.name) or hasRole('ADMIN')")
    public ProjectResponse archive(@PathVariable UUID id) {
        return projectMapper.toResponse(projectService.archive(id));
    }

    @PatchMapping("/batch-status")
    @PreAuthorize("hasRole('ADMIN')")
    public BatchUpdateResult batchUpdateStatus(
            @RequestBody @Valid BatchStatusUpdateRequest req,
            @RequestParam(defaultValue = "500") @Min(1) @Max(1000) int batchSize) {
        return projectService.batchUpdateStatus(req.projectIds(), req.newStatus(), batchSize);
    }
}
