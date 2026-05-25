package com.example.taskflow.controller;

import com.example.taskflow.dto.AuthResponse;
import com.example.taskflow.dto.CreateUserRequest;
import com.example.taskflow.dto.LoginRequest;
import com.example.taskflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@RequestBody @Valid CreateUserRequest req) {
        return authService.register(req.email(), req.displayName(), req.password(), req.role());
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid LoginRequest req) {
        return authService.login(req.email(), req.password());
    }
}
