package com.example.taskflow.dto;

import com.example.taskflow.domain.Comment;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        String body,
        UserResponse author,
        Instant createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(), comment.getBody(),
                UserResponse.from(comment.getAuthor()),
                comment.getCreatedAt());
    }
}
