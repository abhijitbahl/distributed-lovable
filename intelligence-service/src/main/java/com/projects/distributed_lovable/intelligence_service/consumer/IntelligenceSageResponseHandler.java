package com.projects.distributed_lovable.intelligence_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.common_lib.enums.ChatEventStatus;
import com.projects.distributed_lovable.common_lib.event.FileStoreResponseEvent;
import com.projects.distributed_lovable.intelligence_service.repository.ChatEventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntelligenceSageResponseHandler {

    private final ChatEventRepository chatEventRepository;

    @Transactional
    @KafkaListener(topics = "file-store-responses", groupId = "intelligence-group")
    public void handleSageResponse(FileStoreResponseEvent response) {
        chatEventRepository.findBySagaId(response.sagaId()).ifPresent(event -> {
            if (!ChatEventStatus.PENDING.equals(event.getStatus())) {// Idempotency
                log.info("Response for Saga {} already handled. Skipping.", response.sagaId());
                return;
            }

            if (response.success()) {
                event.setStatus(ChatEventStatus.CONFIRMED);
                log.info("Sage {} CONFIRMED", response.sagaId());
            } else {
                log.warn("Saga {} FAILED. Deleting event.", response.sagaId());
                event.setStatus(ChatEventStatus.FAILED);
            }
        });
    }
}
