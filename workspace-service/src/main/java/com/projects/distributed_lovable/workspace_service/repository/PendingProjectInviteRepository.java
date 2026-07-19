package com.projects.distributed_lovable.workspace_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projects.distributed_lovable.workspace_service.entity.PendingProjectInvite;

public interface PendingProjectInviteRepository extends JpaRepository<PendingProjectInvite, Long> {

    List<PendingProjectInvite> findByEmailIgnoreCase(String email);

    boolean existsByProjectIdAndEmailIgnoreCase(Long projectId, String email);

}
