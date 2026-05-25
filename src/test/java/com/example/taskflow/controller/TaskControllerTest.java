package com.example.taskflow.controller;

import com.example.taskflow.config.SecurityConfig;
import com.example.taskflow.security.JwtService;
import com.example.taskflow.security.ProjectSecurityService;
import com.example.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
}
