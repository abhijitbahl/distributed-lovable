package com.projects.distributed_lovable.workspace_service.consumer;

import java.time.Instant;
import java.util.List;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.common_lib.event.UserSignedUpEvent;
import com.projects.distributed_lovable.workspace_service.entity.PendingProjectInvite;
import com.projects.distributed_lovable.workspace_service.entity.ProjectMember;
import com.projects.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.projects.distributed_lovable.workspace_service.repository.PendingProjectInviteRepository;
import com.projects.distributed_lovable.workspace_service.repository.ProjectMemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PendingInviteConsumer {

    private final PendingProjectInviteRepository pendingProjectInviteRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Transactional
    @KafkaListener(topics = "user-signed-up-event", groupId = "workspace-group")
    public void consumeUserSignedUp(UserSignedUpEvent event) {
        List<PendingProjectInvite> pendingInvites = pendingProjectInviteRepository
                .findByEmailIgnoreCase(event.email());

        for (PendingProjectInvite pendingInvite : pendingInvites) {
            ProjectMemberId projectMemberId = new ProjectMemberId(pendingInvite.getProject().getId(), event.userId());
            if (!projectMemberRepository.existsById(projectMemberId)) {
                ProjectMember projectMember = ProjectMember.builder()
                        .id(projectMemberId)
                        .project(pendingInvite.getProject())
                        .projectRole(pendingInvite.getProjectRole())
                        .invitedAt(Instant.now())
                        .build();
                projectMemberRepository.save(projectMember);
                log.info("Resolved pending invite for {} on project {}", event.email(),
                        pendingInvite.getProject().getId());
            }
        }

        pendingProjectInviteRepository.deleteAll(pendingInvites);
    }
}
