package com.projects.distributed_lovable.workspace_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projects.distributed_lovable.workspace_service.entity.ProjectFile;

public interface ProjectFileRepository  extends JpaRepository<ProjectFile, Long> {

    Optional<ProjectFile> findByProjectIdAndPath(Long projectId, String cleanPath);

    List<ProjectFile> findByProjectId(Long projectId);
    
}
