package com.projects.distributed_lovable.workspace_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.projects.distributed_lovable.common_lib.enums.ProjectRole;
import com.projects.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.projects.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.projects.distributed_lovable.workspace_service.entity.Project;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    ProjectResponse toProjectResponse(Project project);

    @Mapping(target = "role", source = "projectRole")
    ProjectSummaryResponse toProjectSummaryResponse(Project project, ProjectRole projectRole);

    List<ProjectSummaryResponse> toListOfProjectSummaryResponse(List<Project> projectList);
}
