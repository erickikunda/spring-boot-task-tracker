package com.example.taskflow.repository;

import com.example.taskflow.domain.Project;
import com.example.taskflow.domain.ProjectMember;
import com.example.taskflow.domain.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    List<ProjectMember> findByProject(Project project);
}
