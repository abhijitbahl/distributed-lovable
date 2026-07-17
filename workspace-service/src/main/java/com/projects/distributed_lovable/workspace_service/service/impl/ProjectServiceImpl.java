package com.projects.distributed_lovable.workspace_service.service.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.common_lib.dto.PlanDto;
import com.projects.distributed_lovable.common_lib.enums.ProjectPermission;
import com.projects.distributed_lovable.common_lib.enums.ProjectRole;
import com.projects.distributed_lovable.common_lib.error.BadRequestException;
import com.projects.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.projects.distributed_lovable.common_lib.security.AuthUtil;
import com.projects.distributed_lovable.workspace_service.client.AccountClient;
import com.projects.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.projects.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.projects.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.projects.distributed_lovable.workspace_service.entity.Project;
import com.projects.distributed_lovable.workspace_service.entity.ProjectMember;
import com.projects.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.projects.distributed_lovable.workspace_service.mapper.ProjectMapper;
import com.projects.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.projects.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.projects.distributed_lovable.workspace_service.security.SecurityExpressions;
import com.projects.distributed_lovable.workspace_service.service.ProjectService;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectServiceImpl implements ProjectService {

    ProjectRepository projectRepository;
    ProjectMemberRepository projectMemberRepository;
    ProjectMapper projectMapper;
    AuthUtil authUtil;
    ProjectTemplateServiceImpl projectTemplateService;
    AccountClient accountClient;
    SecurityExpressions securityExpressions;

    @Override
    public ProjectResponse createProject(ProjectRequest request) {
        if (!canCreateNewProject()) {
            throw new BadRequestException(
                    "Subscription plan does not allow creating new projects. Please upgrade your subscription.");
        }

        Long ownerUserId = authUtil.getCurrentUserId();

        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();

        project = projectRepository.save(project);
        ProjectMemberId projectMemberId = new ProjectMemberId(project.getId(), ownerUserId);

        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .projectRole(ProjectRole.OWNER)
                .acceptedAt(Instant.now())
                .invitedAt(Instant.now())
                .project(project)
                .build();

        projectMemberRepository.save(projectMember);
        projectTemplateService.initializeProjectFromTemplate(project.getId());
        return projectMapper.toProjectResponse(project);
    }

    @Override
    public List<ProjectSummaryResponse> getUserProjects() {
        Long userId = authUtil.getCurrentUserId();
        var projectWithRoleList = projectRepository.findAllAccessibleByUser(userId);
        return projectWithRoleList.stream()
                .map(projectWithRole -> projectMapper.toProjectSummaryResponse(projectWithRole.getProject(),
                        projectWithRole.getProjectRole()))
                .toList();
    }

    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getUserProjectById(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        var projectWithRole = projectRepository.findAccessibleProjectByIdAndRole(projectId, userId)
                .orElseThrow(() -> new BadRequestException("Project not found"));
        return projectMapper.toProjectSummaryResponse(projectWithRole.getProject(), projectWithRole.getProjectRole());
    }

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Long userId = authUtil.getCurrentUserId();
        Project project = findAccessibleProjectById(projectId, userId);
        project.setName(request.name());
        // because of transactional we dont have to write this as it checks if the
        // object is dirty then its gonna save it automatically but its recommended to
        // do this
        project = projectRepository.save(project);
        return projectMapper.toProjectResponse(project);
    }

    @Override
    @PreAuthorize("@security.canDeleteProject(#projectId )")
    public void softDelete(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        Project project = findAccessibleProjectById(projectId, userId);
        project.setDeletedAt(Instant.now());
        projectRepository.save(project);

    }

    // INTERNAL METHODS
    public Project findAccessibleProjectById(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId.toString()));
    }

    private boolean canCreateNewProject() {
        Long userId = authUtil.getCurrentUserId();
        if (userId == null) {
            return false;
        }

        PlanDto plan = accountClient.getCurrentSubscriptionPlanByUser();
        int maxAllowed = plan.maxProjects();
        int ownedCount = projectMemberRepository.countProjectOwnedByUserId(userId);

        return ownedCount < maxAllowed;
    }

    @Override
    public Boolean hasPermission(Long projectId, ProjectPermission permission) {
        return securityExpressions.hasPermission(projectId, permission);
    }

}
