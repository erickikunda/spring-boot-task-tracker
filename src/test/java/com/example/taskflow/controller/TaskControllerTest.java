package com.example.taskflow.controller;

import com.example.taskflow.config.SecurityConfig;
import com.example.taskflow.domain.Priority;
import com.example.taskflow.domain.Project;
import com.example.taskflow.domain.ProjectStatus;
import com.example.taskflow.domain.Role;
import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskStatus;
import com.example.taskflow.domain.User;
import com.example.taskflow.security.JwtService;
import com.example.taskflow.security.ProjectSecurityService;
import com.example.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

// @WithMockUser does NOT work with SessionCreationPolicy.STATELESS — the
// SecurityContextHolderFilter replaces the context from the session/request-attribute repository,
// discarding what @WithMockUser set. Use SecurityMockMvcRequestPostProcessors.user() instead;
// it injects auth directly as a request attribute, which the stateless repository DOES read.
@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
class TaskControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean TaskService taskService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    // Name must match the SpEL reference @projectSecurity in @PreAuthorize expressions.
    @MockitoBean(name = "projectSecurity") ProjectSecurityService projectSecurity;

    private Task task(UUID projectId, UUID taskId) {
        var reporter = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .displayName("Alice")
                .passwordHash("h")
                .role(Role.MEMBER)
                .createdAt(Instant.parse("2026-05-26T16:47:15Z"))
                .build();
        var project = Project.builder()
                .id(projectId)
                .name("Project")
                .status(ProjectStatus.ACTIVE)
                .owner(reporter)
                .build();
        return Task.builder()
                .id(taskId)
                .title("Task")
                .status(TaskStatus.TODO)
                .priority(Priority.MEDIUM)
                .project(project)
                .reporter(reporter)
                .createdAt(Instant.parse("2026-05-26T16:48:15Z"))
                .build();
    }

    @Test
    void list_returns401_whenUnauthenticated() throws Exception {
        mvc.perform(get("/api/v1/projects/{projectId}/tasks", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returns403_whenNotMemberOrAdmin() throws Exception {
        when(projectSecurity.isMember(any(), any())).thenReturn(false);

        mvc.perform(get("/api/v1/projects/{projectId}/tasks", UUID.randomUUID())
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    // ROLE_ADMIN satisfies `or hasRole('ADMIN')` regardless of isMember() result.
    @Test
    void list_returns200_forAdmin() throws Exception {
        when(taskService.findByProject(any(), any())).thenReturn(Page.empty());

        mvc.perform(get("/api/v1/projects/{projectId}/tasks", UUID.randomUUID())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void get_returnsTaskForProjectMember() throws Exception {
        var projectId = UUID.randomUUID();
        var taskId = UUID.randomUUID();
        when(projectSecurity.isMember(any(), any())).thenReturn(true);
        when(taskService.findById(taskId)).thenReturn(task(projectId, taskId));

        mvc.perform(get("/api/v1/projects/{projectId}/tasks/{taskId}", projectId, taskId)
                        .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()));
    }

    @Test
    void updateStatus_returnsTaskForProjectMember() throws Exception {
        var projectId = UUID.randomUUID();
        var taskId = UUID.randomUUID();
        when(projectSecurity.isMember(any(), any())).thenReturn(true);
        when(taskService.updateStatus(eq(taskId), eq(TaskStatus.IN_PROGRESS)))
                .thenReturn(task(projectId, taskId));

        mvc.perform(patch("/api/v1/projects/{projectId}/tasks/{taskId}/status", projectId, taskId)
                        .with(user("alice@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()));
    }

    @Test
    void assign_returnsTaskForProjectMember() throws Exception {
        var projectId = UUID.randomUUID();
        var taskId = UUID.randomUUID();
        var assigneeId = UUID.randomUUID();
        when(projectSecurity.isMember(any(), any())).thenReturn(true);
        when(taskService.assign(eq(taskId), eq(assigneeId))).thenReturn(task(projectId, taskId));

        mvc.perform(put("/api/v1/projects/{projectId}/tasks/{taskId}/assignee", projectId, taskId)
                        .with(user("alice@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assigneeId":"%s"}
                                """.formatted(assigneeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()));
    }

    @Test
    void unassign_returns204ForProjectMember() throws Exception {
        when(projectSecurity.isMember(any(), any())).thenReturn(true);

        mvc.perform(delete("/api/v1/projects/{projectId}/tasks/{taskId}/assignee",
                        UUID.randomUUID(), UUID.randomUUID())
                        .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_returns204ForProjectMember() throws Exception {
        when(projectSecurity.isMember(any(), any())).thenReturn(true);

        mvc.perform(delete("/api/v1/projects/{projectId}/tasks/{taskId}",
                        UUID.randomUUID(), UUID.randomUUID())
                        .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isNoContent());
    }
}
