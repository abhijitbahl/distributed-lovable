package com.projects.distributed_lovable.intelligence_service.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.common_lib.security.AuthUtil;
import com.projects.distributed_lovable.intelligence_service.dto.ChatResponse;
import com.projects.distributed_lovable.intelligence_service.entity.ChatMessage;
import com.projects.distributed_lovable.intelligence_service.entity.ChatSession;
import com.projects.distributed_lovable.intelligence_service.entity.ChatSessionId;
import com.projects.distributed_lovable.intelligence_service.mapper.ChatMapper;
import com.projects.distributed_lovable.intelligence_service.repository.ChatMessageRepository;
import com.projects.distributed_lovable.intelligence_service.repository.ChatSessionRepository;
import com.projects.distributed_lovable.intelligence_service.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final AuthUtil authUtil;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMapper chatMapper;

    @Override
    public List<ChatResponse> getProjectChatHistory(Long projectId) {
        Long userId = authUtil.getCurrentUserId();
        ChatSession chatSession = chatSessionRepository.getReferenceById(new ChatSessionId(projectId, userId));
        List<ChatMessage> chatMessageList = chatMessageRepository.findByChatSession(chatSession);

        return chatMapper.fromListOfChatMessage(chatMessageList);

    }
}
