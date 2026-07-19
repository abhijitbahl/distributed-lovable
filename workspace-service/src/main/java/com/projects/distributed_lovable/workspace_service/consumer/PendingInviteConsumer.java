package com.projects.distributed_lovable.workspace_service.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.common_lib.event.UserSignedUpEvent;
import com.projects.distributed_lovable.workspace_service.service.ProjectMemberService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fallback path for resolving pending invites on signup: account-service
 * also tries this synchronously via an internal call before its signup
 * response returns, so a new user sees their invited projects immediately.
 * This listener exists in case that synchronous call fails (e.g. a
 * transient network blip) — it's idempotent, so processing both is safe.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PendingInviteConsumer {

    private final ProjectMemberService projectMemberService;

    @Transactional
    @KafkaListener(topics = "user-signed-up-event", groupId = "workspace-group")
    public void consumeUserSignedUp(UserSignedUpEvent event) {
        projectMemberService.resolvePendingInvites(event.userId(), event.email());
    }
}
