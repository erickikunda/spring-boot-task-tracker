package com.example.taskflow.repository;

import com.example.taskflow.domain.Project;
import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.TaskStatus;
import com.example.taskflow.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProject(Project project);

    // assignee + reporter are ManyToOne (single-valued) — safe to JOIN FETCH with pagination.
    // Never add a collection (e.g. comments) here: Hibernate would apply LIMIT in memory.
    @EntityGraph(attributePaths = {"assignee", "reporter"})
    Page<Task> findByProject(Project project, Pageable pageable);

    @EntityGraph(attributePaths = {"assignee", "reporter"})
    Page<Task> findByProjectAndStatus(Project project, TaskStatus status, Pageable pageable);

    List<Task> findByAssignee(User assignee);

    List<Task> findByReporter(User reporter);

    long countByProjectAndStatus(Project project, TaskStatus status);
}
