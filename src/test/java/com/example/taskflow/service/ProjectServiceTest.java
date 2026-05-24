package com.example.taskflow.service;

import com.example.taskflow.domain.*;
import com.example.taskflow.exception.BusinessRuleException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.ProjectRepository;
import com.example.taskflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @InjectMocks ProjectService projectService;

    private User owner() {
        return User.builder()
                .id(UUID.randomUUID()).email("alice@example.com")
                .displayName("Alice").passwordHash("h").role(Role.MANAGER)
                .build();
    }

    @Test
    void create_addsOwnerToMembers() {
        var owner = owner();
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = projectService.create("Alpha", "desc", owner);

        assertThat(result.getOwner()).isEqualTo(owner);
        assertThat(result.getMembers()).contains(owner);
        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void archive_setsStatusToArchived() {
        var owner = owner();
        var project = Project.builder()
                .id(UUID.randomUUID()).name("P").status(ProjectStatus.ACTIVE).owner(owner).build();
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = projectService.archive(project.getId());

        assertThat(result.getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
    }

    @Test
    void removeMember_throwsBusinessRuleException_whenRemovingOwner() {
        var owner = owner();
        var project = Project.builder()
                .id(UUID.randomUUID()).name("P").status(ProjectStatus.ACTIVE).owner(owner).build();
        when(projectRepository.findById(project.getId())).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.removeMember(project.getId(), owner.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void findById_throwsResourceNotFoundException_whenMissing() {
        var id = UUID.randomUUID();
        when(projectRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
