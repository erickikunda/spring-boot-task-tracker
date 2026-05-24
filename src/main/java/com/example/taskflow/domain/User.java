package com.example.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
 import org.hibernate.annotations.UpdateTimestamp; 
 import org.hibernate.Hibernate;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")  // "user" is a reserved word in SQL
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)   // always STRING — ORDINAL breaks on enum reordering
    @Column(nullable = false, length = 50)
    private Role role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // ID-based equality: two transient (unsaved) entities are never equal to each other;
    // two managed entities with the same ID are. hashCode is constant so Sets survive
    // the transient → managed lifecycle transition without losing elements.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
         return Hibernate.getClass(this).hashCode(); // unwraps proxy → always User.class
    }
}
