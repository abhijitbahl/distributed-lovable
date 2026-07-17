package com.projects.distributed_lovable.intelligence_service.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.common_lib.enums.ChatEventStatus;
import com.projects.distributed_lovable.common_lib.enums.ChatEventType;
import com.projects.distributed_lovable.common_lib.enums.MessageRole;
import com.projects.distributed_lovable.common_lib.event.FileStoreRequestEvent;
import com.projects.distributed_lovable.common_lib.security.AuthUtil;
import com.projects.distributed_lovable.intelligence_service.client.WorkspaceClient;
import com.projects.distributed_lovable.intelligence_service.dto.StreamResponse;
import com.projects.distributed_lovable.intelligence_service.entity.ChatEvent;
import com.projects.distributed_lovable.intelligence_service.entity.ChatMessage;
import com.projects.distributed_lovable.intelligence_service.entity.ChatSession;
import com.projects.distributed_lovable.intelligence_service.entity.ChatSessionId;
import com.projects.distributed_lovable.intelligence_service.llm.CodeGenerationTools;
import com.projects.distributed_lovable.intelligence_service.llm.FileTreeContextAdvisor;
import com.projects.distributed_lovable.intelligence_service.llm.LLMResponseParser;
import com.projects.distributed_lovable.intelligence_service.llm.PromptUtils;
import com.projects.distributed_lovable.intelligence_service.repository.ChatEventRepository;
import com.projects.distributed_lovable.intelligence_service.repository.ChatMessageRepository;
import com.projects.distributed_lovable.intelligence_service.repository.ChatSessionRepository;
import com.projects.distributed_lovable.intelligence_service.service.AIGenerationService;
import com.projects.distributed_lovable.intelligence_service.service.UsageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIGenerationServiceImpl implements AIGenerationService {

    private final ChatClient chatClient;
    private final AuthUtil authUtil;
    private final FileTreeContextAdvisor fileTreeContextAdvisor;
    private final LLMResponseParser llmResponseParser;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatEventRepository chatEventRepository;
    private final UsageService usageService;
    private final WorkspaceClient workspaceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Regex pattern to extract file tags. Two groups:
    // Group 1: ([^\"]+) - captures the file path (anything between path=" and ")
    // Group 2: (.*?) - captures the file content (anything between > and </file>)
    private static final Pattern FILE_TAG_PATTERN = Pattern.compile("<file path=\"([^\"]+)\">(.*?)</file>",
            Pattern.DOTALL);

    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public Flux<StreamResponse> streamResponse(String userMessage, Long projectId) {
        // usageService.checkDailyTokenUsage(); // Check daily token usage before
        // proceeding

        Long userId = authUtil.getCurrentUserId();
        ChatSession chatSession = createChatSessionIfNotExists(userId, projectId);

        Map<String, Object> advisorParams = Map.of(
                "projectId", projectId,
                "userId", userId);

        StringBuilder fullResponseBuffer = new StringBuilder();

        CodeGenerationTools codeGenerationTools = new CodeGenerationTools(projectId, workspaceClient);

        AtomicReference<Long> startTime = new AtomicReference<>(System.currentTimeMillis());
        AtomicReference<Long> endTime = new AtomicReference<>(0L);
        AtomicReference<Usage> usageRef = new AtomicReference<>();

        return chatClient.prompt()
                .system(PromptUtils.CODE_GENERATION_SYSTEM_PROMPT)
                .user(userMessage)
                .tools(codeGenerationTools)
                .advisors(advisorSpec -> {
                    advisorSpec.params(advisorParams);
                    advisorSpec.advisors(fileTreeContextAdvisor);
                })
                .stream()
                .chatResponse()
                .doOnNext(response -> {
                    if (response.getResults() != null && !response.getResults().isEmpty()) {
                        String content = response.getResult().getOutput().getText();
                        if (content != null && !content.isEmpty() && endTime.get() == 0L) {// first non empty chunk
                                                                                           // received
                            endTime.set(System.currentTimeMillis());
                        }
                        if (response.getMetadata().getUsage() != null) {
                            usageRef.set(response.getMetadata().getUsage());
                        }

                        fullResponseBuffer.append(content);
                        // Optionally, you can log the content or perform other side effects here
                    }
                })
                .doOnComplete(() -> {
                    // Optionally, you can perform actions when the streaming is complete, such as
                    // logging the full response or saving it to the database
                    Schedulers.boundedElastic().schedule(() -> {
                        log.info("Full AI response for project {}: {}", projectId, fullResponseBuffer.toString());

                        Long duration = (endTime.get() - startTime.get()) / 1000; // duration in seconds
                        // this will work asynchronously without blocking the main thread and will not
                        // affect the streaming of the response to the client
                        // this will improve the performance and responsiveness of the application while
                        // still allowing you to capture and utilize the full AI response once it's
                        // complete.
                        finalizeChats(userMessage, chatSession, fullResponseBuffer.toString(), duration,
                                usageRef.get(), userId);
                    });
                })
                .doOnError(error -> {

                    log.error("Error during AI response streaming for project {}: {}", projectId, error.getMessage());
                })
                .map(chatResponse -> {
                    if (chatResponse.getResults() != null && !chatResponse.getResults().isEmpty()) {
                        String text = chatResponse.getResult().getOutput().getText();
                        return new StreamResponse(text != null ? text : ""); // Ensure we don't return null);
                    }
                    return new StreamResponse("");
                });
    }

    private void finalizeChats(String userMessage, ChatSession chatSession, String fullText,
            Long duration, Usage usage, Long userId) {
        Long projectId = chatSession.getId().getProjectId();
        if (usage != null) {
            int totalTokens = usage.getTotalTokens();
            usageService.recordTokenUsage(chatSession.getId().getUserId(), totalTokens);
        }

        // Save the user message
        chatMessageRepository.save(
                ChatMessage.builder()
                        .chatSession(chatSession)
                        .content(userMessage)
                        .role(MessageRole.USER)
                        .tokenUsed(usage.getPromptTokens())
                        .build());

        // Save the assistant message
        ChatMessage assistantChatMessage = ChatMessage.builder()
                .chatSession(chatSession)
                .role(MessageRole.ASSISTANT)
                .content("Assistant Message here")
                .tokenUsed(usage.getCompletionTokens())
                .build();

        assistantChatMessage = chatMessageRepository.save(assistantChatMessage);

        List<ChatEvent> chatEventList = llmResponseParser.parseChatEvents(fullText, assistantChatMessage);
        chatEventList.addFirst(ChatEvent.builder()
                .type(ChatEventType.THOUGHT)
                .status(ChatEventStatus.CONFIRMED)
                .chatMessage(assistantChatMessage)
                .content("Thought for " + duration + "s")
                .sequenceOrder(0)
                .build());
        chatEventList.stream().filter(event -> event.getType() == ChatEventType.FILE_EDIT)
                .forEach(event -> {
                    String sagaId = UUID.randomUUID().toString();
                    event.setSagaId(sagaId);
                    FileStoreRequestEvent fileStoreRequestEvent = new FileStoreRequestEvent(
                            projectId, sagaId, event.getFilePath(), event.getContent(), userId);
                    log.info("Storage request event sent: {}", event.getFilePath());
                    kafkaTemplate.send("file-storage-request-event", "project-" + projectId, fileStoreRequestEvent);
                });

        chatEventRepository.saveAll(chatEventList);
    }

    private ChatSession createChatSessionIfNotExists(Long userId, Long projectId) {
        ChatSessionId chatSessionId = new ChatSessionId(projectId, userId);
        ChatSession chatSession = chatSessionRepository.findById(chatSessionId).orElse(null);
        if (chatSession == null) {
            chatSession = ChatSession.builder()
                    .id(chatSessionId)
                    .build();
            chatSessionRepository.save(chatSession);
        }
        return chatSession;
    }

}
