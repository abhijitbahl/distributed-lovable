package com.projects.distributed_lovable.workspace_service.dto.member;

import java.time.Instant;

import com.projects.distributed_lovable.common_lib.enums.ProjectRole;

public record MemberResponse(
                Long userId,
                String username,
                String name,
                ProjectRole projectRole,
                Instant invitedAt) {

}
