package com.example.taskflow.service;

import com.example.taskflow.domain.*;
import com.example.taskflow.exception.BusinessRuleException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.ProjectRepository;
import com.example.taskflow.repository.TaskRepository;
import com.example.taskflow.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MeterRegistry meterRegistry;

    public Task findById(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    public Page<Task> findByProject(UUID projectId, Pageable pageable) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        return taskRepository.findByProject(project, pageable);
    }

    public Page<Task> findByProjectAndStatus(UUID projectId, TaskStatus status, Pageable pageable) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        return taskRepository.findByProjectAndStatus(project, status, pageable);
    }

    @Transactional
    public Task create(UUID projectId, UUID reporterId, String title, String description,
                       Priority priority, LocalDate dueDate) {
        var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        var reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reporterId));

        var task = Task.builder()
                .title(title)
                .description(description)
                .status(TaskStatus.TODO)
                .priority(priority != null ? priority : Priority.MEDIUM)
                .dueDate(dueDate)
                .project(project)
                .reporter(reporter)
                .build();

        var saved = taskRepository.save(task);
        // Tag by priority so you can break down tasks.created{priority="HIGH"} in dashboards
        meterRegistry.counter("tasks.created",
                "priority", saved.getPriority().name().toLowerCase()).increment();
        return saved;
    }

    @Transactional
    public Task assign(UUID taskId, UUID assigneeId) {
        var task = findById(taskId);
        var assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new ResourceNotFoundException("User", assigneeId));
        task.setAssignee(assignee);
        return taskRepository.save(task);
    }

    @Transactional
    public Task unassign(UUID taskId) {
        var task = findById(taskId);
        task.setAssignee(null);
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateStatus(UUID taskId, TaskStatus newStatus) {
        var task = findById(taskId);
        if (!task.getStatus().canTransitionTo(newStatus)) {
            throw new BusinessRuleException(
                    "Cannot transition task from %s to %s".formatted(task.getStatus(), newStatus));
        }
        task.setStatus(newStatus);
        return taskRepository.save(task);
    }

    @Transactional
    public void delete(UUID taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task", taskId);
        }
        taskRepository.deleteById(taskId);
    }
}
