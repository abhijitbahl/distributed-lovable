package com.projects.distributed_lovable.workspace_service.security;

import org.springframework.stereotype.Component;

import com.projects.distributed_lovable.common_lib.enums.ProjectPermission;
import com.projects.distributed_lovable.common_lib.security.AuthUtil;
import com.projects.distributed_lovable.workspace_service.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;

@Component("security")
@RequiredArgsConstructor
public class SecurityExpressions {

    private final AuthUtil authUtil;
    private final ProjectMemberRepository projectMemberRepository;

    public Boolean hasPermission(Long projectId, ProjectPermission permission) {
        Long userId = authUtil.getCurrentUserId();
        return projectMemberRepository.findRoleByProjectIdAndUserId(projectId, userId)
                .map(role -> role.getPermissions().contains(permission))
                .orElse(false);
    }

    public Boolean canViewProject(Long projectId) {
        System.out.println("Checking view permission for projectId: " + projectId);
        return hasPermission(projectId, ProjectPermission.VIEW);
    }

    public Boolean canEditProject(Long projectId) {
        System.out.println("Checking edit permission for projectId: " + projectId);
        return hasPermission(projectId, ProjectPermission.EDIT);
    }

    public Boolean canDeleteProject(Long projectId) {
        return hasPermission(projectId, ProjectPermission.DELETE);
    }

    public Boolean canManageProjectMembers(Long projectId) {
        return hasPermission(projectId, ProjectPermission.MANAGE_MEMBERS);
    }

    public Boolean canViewProjectMembers(Long projectId) {
        return hasPermission(projectId, ProjectPermission.VIEW_MEMBERS);
    }
}
