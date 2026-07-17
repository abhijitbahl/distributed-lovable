package com.projects.distributed_lovable.intelligence_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.projects.distributed_lovable.common_lib.dto.FileTreeDto;
import com.projects.distributed_lovable.common_lib.enums.ProjectPermission;

@FeignClient(name = "workspace-service", path = "/workspace", url="${WORKSPACE_SERVICE_URI:}")
public interface WorkspaceClient {

    @GetMapping("/internal/v1/projects{projectId}/files/tree")
    FileTreeDto getFileTree(@PathVariable("projectId") Long projectId);

    @GetMapping("/internal/v1/projects/{projectId}/files/content")
    String getFileContent(@PathVariable("projectId") Long ProjectId, @RequestParam("path") String path);

    @GetMapping("/internal/v1/projects/{projectId}/permissions/check")
    Boolean checkPermission(@PathVariable("projectId") Long projectId,
            @RequestParam("permission") ProjectPermission permission);

}
