package com.projects.distributed_lovable.common_lib.enums;

import static com.projects.distributed_lovable.common_lib.enums.ProjectPermission.DELETE;
import static com.projects.distributed_lovable.common_lib.enums.ProjectPermission.EDIT;
import static com.projects.distributed_lovable.common_lib.enums.ProjectPermission.MANAGE_MEMBERS;
import static com.projects.distributed_lovable.common_lib.enums.ProjectPermission.VIEW;
import static com.projects.distributed_lovable.common_lib.enums.ProjectPermission.VIEW_MEMBERS;

import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProjectRole {
    EDITOR(VIEW, EDIT, DELETE, VIEW_MEMBERS),
    VIEWER(Set.of(VIEW, VIEW_MEMBERS)),
    OWNER(Set.of(VIEW, EDIT, DELETE,
            MANAGE_MEMBERS, VIEW_MEMBERS));

    ProjectRole(ProjectPermission... permissions) {// 2 different ways to pass multiple permissions, this way or using
                                                   // Set.of below
        this.permissions = Set.of(permissions);
    }

    private final Set<ProjectPermission> permissions;
}
