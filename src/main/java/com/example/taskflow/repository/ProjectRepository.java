package com.example.taskflow.repository;

import com.example.taskflow.domain.Project;
import com.example.taskflow.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    // Override to eagerly load owner — prevents LazyInitializationException when OSIV is off
    @Override
    @EntityGraph(attributePaths = {"owner"})
    Optional<Project> findById(UUID id);

    @EntityGraph(attributePaths = {"owner"})
    List<Project> findByOwner(User owner);

    // owner is a ManyToOne — safe to JOIN FETCH. members is a collection — do NOT add it here.
    @EntityGraph(attributePaths = {"owner"})
    List<Project> findByMembersContaining(User user);

    boolean existsByIdAndOwner(UUID id, User owner);

    boolean existsByIdAndMembersContaining(UUID id, User user);

    // CAST(id AS VARCHAR) is ANSI SQL and works with both H2 and PostgreSQL.
    // clearAutomatically evicts stale first-level cache entries after the bulk UPDATE.
    @Modifying(clearAutomatically = true)
    @Query(
        value = "UPDATE projects SET status = :status, updated_at = NOW() WHERE CAST(id AS VARCHAR) IN (:ids)",
        nativeQuery = true
    )
    int updateStatusByIds(@Param("ids") List<String> ids, @Param("status") String status);
}
