package com.projects.distributed_lovable.intelligence_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projects.distributed_lovable.intelligence_service.entity.ChatEvent;

public interface ChatEventRepository extends JpaRepository<ChatEvent, Long> {

    Optional<ChatEvent> findBySagaId(String sageId);

}
