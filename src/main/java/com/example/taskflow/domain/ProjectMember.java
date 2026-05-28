package com.example.taskflow.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "project_members")
@Getter
@NoArgsConstructor
public class ProjectMember {

    @EmbeddedId
    private ProjectMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    public ProjectMember(Project project, User user) {
        this.project = project;
        this.user = user;
        this.id = new ProjectMemberId(project.getId(), user.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectMember other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return ProjectMemberId.class.hashCode();
    }
}
