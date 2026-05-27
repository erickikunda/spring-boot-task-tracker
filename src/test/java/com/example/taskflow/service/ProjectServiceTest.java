package com.example.taskflow.service;

import com.example.taskflow.domain.*;
import com.example.taskflow.exception.BusinessRuleException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.ProjectRepository;
import com.example.taskflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @Test
    void batchUpdateStatus_singleBatch_whenIdsCountBelowBatchSize() {
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        when(projectRepository.updateStatusByIds(any(), any())).thenReturn(3);

        var result = projectService.batchUpdateStatus(ids, ProjectStatus.ARCHIVED, 500);

        assertThat(result.updatedCount()).isEqualTo(3);
        assertThat(result.batchCount()).isEqualTo(1);
        assertThat(result.batchSize()).isEqualTo(500);
        verify(projectRepository, times(1)).updateStatusByIds(any(), eq("ARCHIVED"));
    }

    @Test
    void batchUpdateStatus_multipleChunks_whenIdsExceedBatchSize() {
        // 5 ids, batchSize=2 → chunks [0,1], [2,3], [4] = 3 repo calls
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                          UUID.randomUUID(), UUID.randomUUID());
        when(projectRepository.updateStatusByIds(any(), any()))
                .thenReturn(2).thenReturn(2).thenReturn(1);

        var result = projectService.batchUpdateStatus(ids, ProjectStatus.ACTIVE, 2);

        assertThat(result.updatedCount()).isEqualTo(5);
        assertThat(result.batchCount()).isEqualTo(3);
        verify(projectRepository, times(3)).updateStatusByIds(any(), eq("ACTIVE"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void batchUpdateStatus_passesCorrectChunkContentsToRepository() {
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        var id3 = UUID.randomUUID();
        when(projectRepository.updateStatusByIds(any(), any())).thenReturn(1);

        projectService.batchUpdateStatus(List.of(id1, id2, id3), ProjectStatus.ARCHIVED, 2);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(projectRepository, times(2)).updateStatusByIds(captor.capture(), eq("ARCHIVED"));
        assertThat(captor.getAllValues().get(0)).containsExactly(id1.toString(), id2.toString());
        assertThat(captor.getAllValues().get(1)).containsExactly(id3.toString());
    }
}
