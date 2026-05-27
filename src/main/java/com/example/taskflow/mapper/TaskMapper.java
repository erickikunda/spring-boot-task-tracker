package com.example.taskflow.mapper;

import com.example.taskflow.domain.Task;
import com.example.taskflow.dto.TaskResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface TaskMapper {
    @Mapping(source = "project.id", target = "projectId")
    TaskResponse toResponse(Task task);
}
