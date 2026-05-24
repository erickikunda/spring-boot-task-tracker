package com.example.taskflow.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
   import org.hibernate.annotations.UpdateTimestamp; 
 import org.hibernate.Hibernate; 
  import org.hibernate.Hibernate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "tasks",
    indexes = {
        @Index(name = "idx_tasks_project_id",  columnList = "project_id"),
        @Index(name = "idx_tasks_assignee_id", columnList = "assignee_id"),
        @Index(name = "idx_tasks_status",      columnList = "status")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Priority priority;

    private LocalDate dueDate;  // nullable — tasks don't require a due date

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Nullable: a task may be unassigned
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
         return Hibernate.getClass(this).hashCode(); // unwraps proxy → always User.class
    }
}
