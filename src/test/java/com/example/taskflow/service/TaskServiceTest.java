package com.example.taskflow.service;

import com.example.taskflow.domain.*;
import com.example.taskflow.exception.BusinessRuleException;
import com.example.taskflow.repository.ProjectRepository;
import com.example.taskflow.repository.TaskRepository;
import com.example.taskflow.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock ProjectRepository projectRepository;
    @Mock UserRepository userRepository;
    @Mock MeterRegistry meterRegistry;
    @Mock Counter counter;
    @InjectMocks TaskService taskService;

    private Task taskWithStatus(TaskStatus status) {
        return Task.builder()
                .id(UUID.randomUUID())
                .title("Test task")
                .status(status)
                .priority(Priority.MEDIUM)
                .project(Project.builder().id(UUID.randomUUID()).name("P")
                        .status(ProjectStatus.ACTIVE)
                        .owner(User.builder().id(UUID.randomUUID()).build()).build())
                .reporter(User.builder().id(UUID.randomUUID()).build())
                .build();
    }

    @Test
    void create_defaultsStatusToTodoAndPriorityToMedium() {
        var projectId = UUID.randomUUID();
        var reporterId = UUID.randomUUID();
        var project = Project.builder().id(projectId).name("P")
                .status(ProjectStatus.ACTIVE)
                .owner(User.builder().id(UUID.randomUUID()).build()).build();
        var reporter = User.builder().id(reporterId).build();
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(reporterId)).thenReturn(Optional.of(reporter));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meterRegistry.counter(anyString(), anyString(), anyString())).thenReturn(counter);

        var result = taskService.create(projectId, reporterId, "New task", null, null, null);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(result.getPriority()).isEqualTo(Priority.MEDIUM);
    }

    @Test
    void updateStatus_succeeds_forValidTransition() {
        var task = taskWithStatus(TaskStatus.TODO);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = taskService.updateStatus(task.getId(), TaskStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateStatus_throwsBusinessRuleException_forInvalidTransition() {
        var task = taskWithStatus(TaskStatus.DONE);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.updateStatus(task.getId(), TaskStatus.TODO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DONE");
    }

    // DONE and CANCELLED are terminal — verify neither accepts any inbound transition
    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"DONE", "CANCELLED"})
    void terminalStatuses_rejectAllTransitions(TaskStatus terminal) {
        var task = taskWithStatus(terminal);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        for (var next : TaskStatus.values()) {
            assertThatThrownBy(() -> taskService.updateStatus(task.getId(), next))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Test
    void updateStatus_inReview_canTransitionToDoneOrBack() {
        var task = taskWithStatus(TaskStatus.IN_REVIEW);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> taskService.updateStatus(task.getId(), TaskStatus.DONE))
                .doesNotThrowAnyException();
    }
}
