package com.example.taskflow.service;

import com.example.taskflow.domain.Comment;
import com.example.taskflow.domain.Task;
import com.example.taskflow.domain.User;
import com.example.taskflow.exception.ForbiddenException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.CommentRepository;
import com.example.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;

    public List<Comment> findByTask(UUID taskId) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
        return commentRepository.findByTaskOrderByCreatedAtAsc(task);
    }

    @Transactional
    public Comment create(UUID taskId, User author, String body) {
        var task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
        var comment = Comment.builder()
                .body(body)
                .task(task)
                .author(author)
                .build();
        return commentRepository.save(comment);
    }

    @Transactional
    public void delete(UUID commentId, User requestingUser) {
        var comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));
        // Domain rule: only the author may delete. Admin override wired in Module 5 via @PreAuthorize.
        if (!comment.getAuthor().getId().equals(requestingUser.getId())) {
            throw new ForbiddenException("Only the comment author can delete this comment");
        }
        commentRepository.delete(comment);
    }
}
