package com.projects.distributed_lovable.workspace_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projects.distributed_lovable.common_lib.dto.FileTreeDto;
import com.projects.distributed_lovable.common_lib.enums.ProjectPermission;
import com.projects.distributed_lovable.workspace_service.service.ProjectFileService;
import com.projects.distributed_lovable.workspace_service.service.ProjectMemberService;
import com.projects.distributed_lovable.workspace_service.service.ProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/")
public class InternalWorkspaceController {
    private final ProjectService projectService;
    private final ProjectFileService projectFileService;
    private final ProjectMemberService projectMemberService;

    @GetMapping("/projects{projectId}/files/tree")
    public FileTreeDto getFileTree(@PathVariable("projectId") Long projectId) {
        return projectFileService.getFileTree(projectId);
    }

    @GetMapping("/projects/{projectId}/files/content")
    public String getFileContent(@PathVariable("projectId") Long projectId, @RequestParam("path") String path) {
        return projectFileService.getFileContent(projectId, path);
    }

    @GetMapping("/projects/{projectId}/permissions/check")
    public Boolean checkProjectPermission(@PathVariable("projectId") Long projectId, @RequestParam ProjectPermission permission) {
        return projectService.hasPermission(projectId, permission);
    }

    @PostMapping("/users/{userId}/resolve-pending-invites")
    public void resolvePendingInvites(@PathVariable("userId") Long userId, @RequestParam("email") String email) {
        projectMemberService.resolvePendingInvites(userId, email);
    }
}
