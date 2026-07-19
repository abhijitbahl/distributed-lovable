package com.projects.distributed_lovable.account_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "workspace-service", path = "/workspace", url = "${WORKSPACE_SERVICE_URI:}")
public interface WorkspaceClient {

    @PostMapping("/internal/v1/users/{userId}/resolve-pending-invites")
    void resolvePendingInvites(@PathVariable("userId") Long userId, @RequestParam("email") String email);

}
