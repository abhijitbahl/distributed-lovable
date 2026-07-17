package com.projects.distributed_lovable.intelligence_service.dto;

import java.time.Instant;
import java.util.List;

import com.projects.distributed_lovable.common_lib.enums.MessageRole;


public record ChatResponse(
        Long id,
        List<ChatEventResponse> events,
        String content,
        // USER. ASSISTANT
        MessageRole role,
        Integer tokenUsed,
        Instant createdAt) {

}
