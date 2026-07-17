package com.projects.distributed_lovable.workspace_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.projects.distributed_lovable.common_lib.enums.ProjectRole;
import com.projects.distributed_lovable.workspace_service.entity.Project;

public interface ProjectRepository extends JpaRepository<Project, Long> {

        @Query("""
                        SELECT p as project, pm.projectRole as projectRole FROM Project p
                        JOIN ProjectMember pm ON pm.project.id=p.id
                        WHERE pm.id.userId = :userId
                        AND p.deletedAt IS NULL
                        ORDER BY p.updatedAt DESC
                        """)
        List<ProjectWithRole> findAllAccessibleByUser(@Param("userId") Long userId);

        @Query("""
                        SELECT p FROM Project p
                        WHERE p.id=:projectId
                        AND p.deletedAt IS NULL
                        AND EXISTS (
                            SELECT 1 FROM ProjectMember pm
                            WHERE pm.id.projectId = p.id
                            AND pm.id.userId = :userId
                        )
                        """)
        Optional<Project> findAccessibleProjectById(@Param("projectId") Long projectId, @Param("userId") Long userId);

        @Query("""
                        SELECT p as project, pm.projectRole as projectRole FROM Project p
                        JOIN ProjectMember pm ON pm.project.id=p.id
                        WHERE p.id=:projectId
                        AND pm.id.userId = :userId
                        AND p.deletedAt IS NULL
                        """)
        Optional<ProjectWithRole> findAccessibleProjectByIdAndRole(@Param("projectId") Long projectId,
                        @Param("userId") Long userId);

        interface ProjectWithRole {
                Project getProject();

                ProjectRole getProjectRole();
        }
}
