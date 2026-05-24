package com.example.taskflow.repository;

import com.example.taskflow.domain.Project;
import com.example.taskflow.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOwner(User owner);

    // owner is a ManyToOne — safe to JOIN FETCH. members is a collection — do NOT add it here.
    @EntityGraph(attributePaths = {"owner"})
    List<Project> findByMembersContaining(User user);

    boolean existsByIdAndOwner(UUID id, User owner);

    boolean existsByIdAndMembersContaining(UUID id, User user);
}
