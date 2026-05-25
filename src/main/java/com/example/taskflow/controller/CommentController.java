package com.example.taskflow.controller;

import com.example.taskflow.dto.CommentResponse;
import com.example.taskflow.dto.CreateCommentRequest;
import com.example.taskflow.security.UserPrincipal;
import com.example.taskflow.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentResponse> list(@PathVariable UUID taskId) {
        return commentService.findByTask(taskId).stream()
                .map(CommentResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @PathVariable UUID taskId,
            @RequestBody @Valid CreateCommentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return CommentResponse.from(commentService.create(taskId, principal.user(), req.body()));
    }

    // Service enforces author-only delete; ADMIN override is wired via @PreAuthorize in the service.
    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID taskId,
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        commentService.delete(commentId, principal.user());
    }
}
