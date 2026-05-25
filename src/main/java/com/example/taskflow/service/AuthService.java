package com.example.taskflow.service;

import com.example.taskflow.dto.AuthResponse;
import com.example.taskflow.dto.UserResponse;
import com.example.taskflow.repository.UserRepository;
import com.example.taskflow.security.JwtService;
import com.example.taskflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(String email, String displayName, String password,
                                  com.example.taskflow.domain.Role role) {
        var user = userService.create(email, displayName, password, role);
        var token = jwtService.generateToken(new UserPrincipal(user));
        return new AuthResponse(token, UserResponse.from(user));
    }

    public AuthResponse login(String email, String rawPassword) {
        // Throws BadCredentialsException (→ 401) on wrong credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword));
        var user = userRepository.findByEmail(email)
                .orElseThrow();  // can't happen: authenticate() succeeded
        var token = jwtService.generateToken(new UserPrincipal(user));
        return new AuthResponse(token, UserResponse.from(user));
    }
}
