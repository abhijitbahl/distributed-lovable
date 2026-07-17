package com.projects.distributed_lovable.workspace_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projects.distributed_lovable.workspace_service.entity.ProcessedEvent;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {

}
