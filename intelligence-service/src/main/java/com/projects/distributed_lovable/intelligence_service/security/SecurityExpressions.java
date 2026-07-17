package com.projects.distributed_lovable.intelligence_service.security;

import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.stereotype.Component;

import com.projects.distributed_lovable.common_lib.enums.ProjectPermission;
import com.projects.distributed_lovable.common_lib.security.AuthUtil;
import com.projects.distributed_lovable.intelligence_service.client.WorkspaceClient;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component("security")
@RequiredArgsConstructor
@Slf4j
public class SecurityExpressions {

    private final AuthUtil authUtil;
    private final WorkspaceClient workspaceClient;

    private Boolean hasPermission(Long projectId, ProjectPermission permission) {
        try {
            return workspaceClient.checkPermission(projectId, permission);
        } catch (FeignException.Unauthorized e) {
            log.warn("Token expired or invalid during permission check for project: {}", projectId);
            throw new CredentialsExpiredException("JWT token is expired or invalid");
        } catch (FeignException e) {
            log.error("Workspace-service failed during permission check: {}", e.getMessage());
            return false;
        }

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
