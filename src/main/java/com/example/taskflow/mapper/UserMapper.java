package com.example.taskflow.mapper;

import com.example.taskflow.domain.User;
import com.example.taskflow.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
