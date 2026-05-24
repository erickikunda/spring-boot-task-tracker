package com.example.taskflow.repository;

import com.example.taskflow.domain.Comment;
import com.example.taskflow.domain.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    // author is always needed when rendering comments — fetch it in the same query
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByTaskOrderByCreatedAtAsc(Task task);
}
