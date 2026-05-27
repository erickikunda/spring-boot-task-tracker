package com.example.taskflow.repository;

import com.example.taskflow.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class ProjectRepositoryTest {

    @Autowired ProjectRepository projectRepository;
    @Autowired TestEntityManager em;

    private User savedUser(String email) {
        return em.persistAndFlush(User.builder()
                .email(email).displayName("User").passwordHash("h").role(Role.MEMBER).build());
    }

    private Project savedProject(User owner, User... members) {
        var project = Project.builder()
                .name("P").status(ProjectStatus.ACTIVE).owner(owner).build();
        for (var member : members) {
            project.getMembers().add(member);
        }
        return em.persistAndFlush(project);
    }

    @Test
    void existsByIdAndOwner_returnsTrueForProjectOwner() {
        var owner = savedUser("owner@example.com");
        var project = savedProject(owner);
        em.clear();

        assertThat(projectRepository.existsByIdAndOwner(project.getId(), owner)).isTrue();
    }

    @Test
    void existsByIdAndOwner_returnsFalseForNonOwner() {
        var owner = savedUser("owner@example.com");
        var other = savedUser("other@example.com");
        var project = savedProject(owner);
        em.clear();

        assertThat(projectRepository.existsByIdAndOwner(project.getId(), other)).isFalse();
    }

    @Test
    void existsByIdAndMembersContaining_returnsTrueForAddedMember() {
        var owner = savedUser("owner@example.com");
        var member = savedUser("member@example.com");
        var project = savedProject(owner, member);
        em.clear();

        assertThat(projectRepository.existsByIdAndMembersContaining(project.getId(), member)).isTrue();
    }

    @Test
    void existsByIdAndMembersContaining_returnsFalseForNonMember() {
        var owner = savedUser("owner@example.com");
        var stranger = savedUser("stranger@example.com");
        var project = savedProject(owner);
        em.clear();

        assertThat(projectRepository.existsByIdAndMembersContaining(project.getId(), stranger)).isFalse();
    }

    @Test
    void updateStatusByIds_updatesOnlyMatchingRows_andReturnsCount() {
        var owner = savedUser("owner@example.com");
        var p1 = savedProject(owner);
        var p2 = savedProject(owner);
        var p3 = savedProject(owner); // not in the update list
        // clearAutomatically on @Modifying clears the first-level cache, so
        // the subsequent findById calls reload from the DB within this transaction.
        int updated = projectRepository.updateStatusByIds(
                List.of(p1.getId().toString(), p2.getId().toString()),
                "ARCHIVED"
        );

        assertThat(updated).isEqualTo(2);
        assertThat(projectRepository.findById(p1.getId()).orElseThrow().getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(projectRepository.findById(p2.getId()).orElseThrow().getStatus()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(projectRepository.findById(p3.getId()).orElseThrow().getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void updateStatusByIds_returnsZero_whenNoIdsMatch() {
        var owner = savedUser("owner@example.com");
        savedProject(owner);

        int updated = projectRepository.updateStatusByIds(
                List.of(UUID.randomUUID().toString()),
                "ARCHIVED"
        );

        assertThat(updated).isZero();
    }
}
