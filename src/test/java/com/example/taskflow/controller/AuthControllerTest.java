package com.example.taskflow.controller;

import com.example.taskflow.config.SecurityConfig;
import com.example.taskflow.domain.Role;
import com.example.taskflow.dto.AuthResponse;
import com.example.taskflow.dto.UserResponse;
import com.example.taskflow.security.JwtService;
import com.example.taskflow.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @Import(SecurityConfig.class) ensures our custom filter chain (JWT, permitAll rules) is active
// rather than Spring Boot's default form-login security.
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;

    @MockitoBean AuthService authService;
    // Both needed by JwtAuthenticationFilter, which is a Filter bean included in the slice.
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    private AuthResponse fakeResponse() {
        var user = new UserResponse(UUID.randomUUID(), "alice@example.com", "Alice",
                Role.MEMBER, Instant.now());
        return new AuthResponse("signed.jwt", user);
    }

    @Test
    void register_returns201_withValidBody() throws Exception {
        when(authService.register(anyString(), anyString(), anyString(), any()))
                .thenReturn(fakeResponse());

        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice","password":"pass1234"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("signed.jwt"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));
    }

    @Test
    void register_returns400_whenEmailIsMissing() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Alice","password":"pass1234"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns400_whenPasswordIsTooShort() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","displayName":"Alice","password":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns200_withValidCredentials() throws Exception {
        when(authService.login(anyString(), anyString()))
                .thenReturn(fakeResponse());

        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com","password":"pass1234"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt"));
    }
}
