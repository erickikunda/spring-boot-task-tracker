package com.example.taskflow.service;

import com.example.taskflow.domain.Role;
import com.example.taskflow.domain.User;
import com.example.taskflow.exception.ConflictException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email", email));
    }

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User create(String email, String displayName, String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered: " + email);
        }
        return userRepository.save(User.builder()
                .email(email)
                .displayName(displayName)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(role != null ? role : Role.MEMBER)
                .build());
    }

    @Transactional
    public User updateRole(UUID id, Role newRole) {
        var user = findById(id);
        user.setRole(newRole);
        return userRepository.save(user);
    }
}
