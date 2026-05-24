package com.example.taskflow.repository;

import com.example.taskflow.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void savesAndRetrievesUserByEmail() {
        var user = User.builder()
                .email("alice@example.com")
                .displayName("Alice Smith")
                .passwordHash("$2a$10$irrelevant")
                .role(Role.MEMBER)
                .build();
        em.persistAndFlush(user);
        // Detach everything — forces the next findByEmail to hit the DB,
        // not the first-level (session) cache.
        em.clear();

        var found = userRepository.findByEmail("alice@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("Alice Smith");
        assertThat(found.get().getRole()).isEqualTo(Role.MEMBER);
        assertThat(found.get().getId()).isNotNull();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void existsByEmail_returnsTrueForExistingEmail() {
        em.persistAndFlush(User.builder()
                .email("bob@example.com").displayName("Bob").passwordHash("h").role(Role.MANAGER).build());
        em.clear();

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void uniqueEmailConstraint_rejectsDuplicate() {
        em.persistAndFlush(User.builder()
                .email("dup@example.com").displayName("First").passwordHash("h").role(Role.MEMBER).build());

        assertThatThrownBy(() ->
                em.persistAndFlush(User.builder()
                        .email("dup@example.com").displayName("Second").passwordHash("h").role(Role.MEMBER).build()))
                .isInstanceOf(Exception.class);
    }
}
