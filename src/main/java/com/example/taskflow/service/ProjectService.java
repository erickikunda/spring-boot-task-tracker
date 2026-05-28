package com.example.taskflow.service;

import com.example.taskflow.domain.Project;
import com.example.taskflow.domain.ProjectMember;
import com.example.taskflow.domain.ProjectMemberId;
import com.example.taskflow.domain.ProjectStatus;
import com.example.taskflow.domain.User;
import com.example.taskflow.dto.BatchUpdateResult;
import com.example.taskflow.exception.BusinessRuleException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.ProjectMemberRepository;
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
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;

    public Project findById(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    public List<Project> findByOwner(User owner) {
        return projectRepository.findByOwner(owner);
    }

    public List<Project> findByMember(User member) {
        return projectRepository.findByMemberships_User(member);
    }

    public List<User> findMembers(UUID projectId) {
        var project = findById(projectId);
        return projectMemberRepository.findByProject(project).stream()
                .map(ProjectMember::getUser)
                .sorted(Comparator.comparing(User::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public Project create(String name, String description, User owner) {
        var project = projectRepository.save(Project.builder()
                .name(name)
                .description(description)
                .status(ProjectStatus.ACTIVE)
                .owner(owner)
                .build());
        projectMemberRepository.save(new ProjectMember(project, owner));
        return project;
    }

    @Transactional
    public void addMember(UUID projectId, UUID userId) {
        var project = findById(projectId);
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        var memberId = new ProjectMemberId(projectId, userId);
        if (!projectMemberRepository.existsById(memberId)) {
            projectMemberRepository.save(new ProjectMember(project, user));
        }
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId) {
        var project = findById(projectId);
        if (project.getOwner().getId().equals(userId)) {
            throw new BusinessRuleException("Cannot remove the project owner from members");
        }
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        projectMemberRepository.deleteById(new ProjectMemberId(projectId, userId));
    }

    @Transactional
    public Project archive(UUID projectId) {
        var project = findById(projectId);
        project.setStatus(ProjectStatus.ARCHIVED);
        return projectRepository.save(project);
    }

    // Runs all chunks inside one transaction: either every batch commits or none does.
    // Native IN-list queries degrade past ~1000 ids (query plan, DB limits), so we chunk.
    @Transactional
    public BatchUpdateResult batchUpdateStatus(List<UUID> ids, ProjectStatus newStatus, int batchSize) {
        List<String> idStrings = ids.stream().map(UUID::toString).toList();
        int updatedCount = 0;
        int batchCount = 0;
        for (int i = 0; i < idStrings.size(); i += batchSize) {
            List<String> chunk = idStrings.subList(i, Math.min(i + batchSize, idStrings.size()));
            updatedCount += projectRepository.updateStatusByIds(chunk, newStatus.name());
            batchCount++;
        }
        return new BatchUpdateResult(updatedCount, batchCount, batchSize);
    }
}
