package com.projects.distributed_lovable.intelligence_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.projects.distributed_lovable.intelligence_service.entity.ChatMessage;
import com.projects.distributed_lovable.intelligence_service.entity.ChatSession;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("SELECT DISTINCT cm FROM ChatMessage cm LEFT JOIN FETCH cm.events ce WHERE cm.chatSession = :chatSession ORDER BY cm.createdAt ASC, ce.sequenceOrder ASC")
    List<ChatMessage> findByChatSession(ChatSession chatSession);
}