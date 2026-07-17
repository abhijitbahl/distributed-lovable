package com.projects.distributed_lovable.workspace_service.service;

import java.util.List;

import com.projects.distributed_lovable.common_lib.enums.ProjectPermission;
import com.projects.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.projects.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.projects.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;

public interface ProjectService {

    List<ProjectSummaryResponse> getUserProjects();

    ProjectSummaryResponse getUserProjectById(Long id);

    ProjectResponse createProject(ProjectRequest request);

    ProjectResponse updateProject(Long id, ProjectRequest request);

    void softDelete(Long id);

    Boolean hasPermission(Long projectId, ProjectPermission permission);

}
