package com.example.taskflow.security;

import com.example.taskflow.repository.ProjectRepository;
import com.example.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// Bean name "projectSecurity" used in @PreAuthorize SpEL: @projectSecurity.isMember(...)
@Service("projectSecurity")
@RequiredArgsConstructor
public class ProjectSecurityService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isMember(UUID projectId, String email) {
        return userRepository.findByEmail(email)
                .map(u -> projectRepository.existsByIdAndMemberships_User(projectId, u))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(UUID projectId, String email) {
        return userRepository.findByEmail(email)
                .map(u -> projectRepository.existsByIdAndOwner(projectId, u))
                .orElse(false);
    }
}
