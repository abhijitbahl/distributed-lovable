package com.projects.distributed_lovable.intelligence_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projects.distributed_lovable.intelligence_service.entity.ChatSession;
import com.projects.distributed_lovable.intelligence_service.entity.ChatSessionId;

public interface ChatSessionRepository extends JpaRepository<ChatSession, ChatSessionId> {
    // Additional query methods can be defined here if needed

}
