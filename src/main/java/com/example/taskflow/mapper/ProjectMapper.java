package com.example.taskflow.mapper;

import com.example.taskflow.domain.Project;
import com.example.taskflow.dto.ProjectResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface ProjectMapper {
    ProjectResponse toResponse(Project project);
}
