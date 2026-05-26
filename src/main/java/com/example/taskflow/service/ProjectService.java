package com.example.taskflow.service;

import com.example.taskflow.domain.Project;
import com.example.taskflow.domain.ProjectStatus;
import com.example.taskflow.domain.User;
import com.example.taskflow.exception.BusinessRuleException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.ProjectRepository;
import com.example.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public Project findById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    public List<Project> findByOwner(User owner) {
        return projectRepository.findByOwner(owner);
    }

    public List<Project> findByMember(User member) {
        return projectRepository.findByMembersContaining(member);
    }

    public List<User> findMembers(UUID projectId) {
        var project = findById(projectId);
        return project.getMembers().stream()
                .sorted(Comparator.comparing(User::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public Project create(String name, String description, User owner) {
        var project = Project.builder()
                .name(name)
                .description(description)
                .status(ProjectStatus.ACTIVE)
                .owner(owner)
                .build();
        project.getMembers().add(owner);  // owner is always a member
        return projectRepository.save(project);
    }

    @Transactional
    public void addMember(UUID projectId, UUID userId) {
        var project = findById(projectId);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        project.getMembers().add(user);  // Set semantics: idempotent
        projectRepository.save(project);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId) {
        var project = findById(projectId);
        if (project.getOwner().getId().equals(userId)) {
            throw new BusinessRuleException("Cannot remove the project owner from members");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        project.getMembers().remove(user);
        projectRepository.save(project);
    }

    @Transactional
    public Project archive(UUID projectId) {
        var project = findById(projectId);
        project.setStatus(ProjectStatus.ARCHIVED);
        return projectRepository.save(project);
    }
}
