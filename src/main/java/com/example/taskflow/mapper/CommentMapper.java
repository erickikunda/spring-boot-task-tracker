package com.example.taskflow.mapper;

import com.example.taskflow.domain.Comment;
import com.example.taskflow.dto.CommentResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface CommentMapper {
    CommentResponse toResponse(Comment comment);
}
