package com.projects.distributed_lovable.workspace_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projects.distributed_lovable.common_lib.enums.ProjectRole;
import com.projects.distributed_lovable.workspace_service.entity.ProjectMember;
import com.projects.distributed_lovable.workspace_service.entity.ProjectMemberId;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

        List<ProjectMember> findByIdProjectId(Long projectId);

        @Query("""
                        SELECT pm.projectRole FROM ProjectMember pm
                        WHERE pm.id.projectId = :projectId
                        AND pm.id.userId = :userId
                        """)
        Optional<ProjectRole> findRoleByProjectIdAndUserId(@Param("projectId") Long projectId,
                        @Param("userId") Long userId);

        @Query("""
                        SELECT COUNT(pm) FROM ProjectMember pm
                        WHERE pm.id.userId = :userId
                        AND pm.projectRole = 'OWNER'
                        """)
        int countProjectOwnedByUserId(@Param("userId") Long userId);

}
