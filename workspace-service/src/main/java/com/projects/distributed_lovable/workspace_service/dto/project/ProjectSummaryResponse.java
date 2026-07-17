package com.projects.distributed_lovable.workspace_service.dto.project;

import java.time.Instant;

import com.projects.distributed_lovable.common_lib.enums.ProjectRole;

public record ProjectSummaryResponse(
                Long id,
                String name,
                Instant createdAt,
                Instant updatedAt,
                ProjectRole role) {
}
