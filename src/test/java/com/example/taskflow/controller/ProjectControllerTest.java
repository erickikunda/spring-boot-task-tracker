package com.example.taskflow.controller;

import com.example.taskflow.config.SecurityConfig;
import com.example.taskflow.domain.Role;
import com.example.taskflow.domain.User;
import com.example.taskflow.security.JwtService;
import com.example.taskflow.security.ProjectSecurityService;
import com.example.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import(SecurityConfig.class)
class ProjectControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean ProjectService projectService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean(name = "projectSecurity") ProjectSecurityService projectSecurity;

    @Test
    void members_returns401_whenUnauthenticated() throws Exception {
        mvc.perform(get("/api/v1/projects/{projectId}/members", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void members_returns403_whenNotMemberOrAdmin() throws Exception {
        when(projectSecurity.isMember(any(), any())).thenReturn(false);

        mvc.perform(get("/api/v1/projects/{projectId}/members", UUID.randomUUID())
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void members_returnsMembersForProjectMember() throws Exception {
        var projectId = UUID.randomUUID();
        var member = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .displayName("Alice")
                .passwordHash("h")
                .role(Role.MEMBER)
                .createdAt(Instant.parse("2026-05-26T16:47:15Z"))
                .build();

        when(projectSecurity.isMember(any(), any())).thenReturn(true);
        when(projectService.findMembers(projectId)).thenReturn(List.of(member));

        mvc.perform(get("/api/v1/projects/{projectId}/members", projectId)
                        .with(user("alice@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$[0].displayName").value("Alice"));
    }
}
