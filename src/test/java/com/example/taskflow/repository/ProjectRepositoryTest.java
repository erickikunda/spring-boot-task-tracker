package com.example.taskflow.repository;

import com.example.taskflow.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

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
}
