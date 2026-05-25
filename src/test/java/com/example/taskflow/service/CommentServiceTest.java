package com.example.taskflow.service;

import com.example.taskflow.domain.*;
import com.example.taskflow.exception.ForbiddenException;
import com.example.taskflow.exception.ResourceNotFoundException;
import com.example.taskflow.repository.CommentRepository;
import com.example.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock TaskRepository taskRepository;
    @InjectMocks CommentService commentService;

    private User user(UUID id) {
        return User.builder().id(id).email(id + "@example.com")
                .displayName("U").passwordHash("h").role(Role.MEMBER).build();
    }

    @Test
    void delete_throwsForbiddenException_whenCallerIsNotAuthor() {
        var author = user(UUID.randomUUID());
        var stranger = user(UUID.randomUUID());
        var comment = Comment.builder().id(UUID.randomUUID()).body("hi").author(author).build();
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.delete(comment.getId(), stranger))
                .isInstanceOf(ForbiddenException.class);

        verify(commentRepository, never()).delete(any());
    }

    @Test
    void delete_removesComment_whenCallerIsAuthor() {
        var author = user(UUID.randomUUID());
        var comment = Comment.builder().id(UUID.randomUUID()).body("hi").author(author).build();
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));

        assertThatCode(() -> commentService.delete(comment.getId(), author))
                .doesNotThrowAnyException();

        verify(commentRepository).delete(comment);
    }

    @Test
    void create_throwsResourceNotFoundException_whenTaskDoesNotExist() {
        var taskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(taskId, user(UUID.randomUUID()), "body"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
