package com.projects.distributed_lovable.workspace_service.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.common_lib.dto.UserDto;
import com.projects.distributed_lovable.common_lib.security.AuthUtil;
import com.projects.distributed_lovable.workspace_service.client.AccountClient;
import com.projects.distributed_lovable.workspace_service.dto.member.InviteMemberRequest;
import com.projects.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.projects.distributed_lovable.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.projects.distributed_lovable.workspace_service.entity.Project;
import com.projects.distributed_lovable.workspace_service.entity.ProjectMember;
import com.projects.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.projects.distributed_lovable.workspace_service.mapper.ProjectMemberMapper;
import com.projects.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.projects.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.projects.distributed_lovable.workspace_service.service.EmailService;
import com.projects.distributed_lovable.workspace_service.service.ProjectMemberService;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {

    ProjectMemberRepository projectMemberRepository;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    AuthUtil authUtil;
    AccountClient accountClient;
    EmailService emailService;

    @Override
    @PreAuthorize("@security.canViewProjectMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {
        return projectMemberRepository.findByIdProjectId(projectId).stream()
                .map(projectMemberMapper::toProjectMemberResponseFromMember)
                .map(this::enrichWithUsername).toList();
    }

    private MemberResponse enrichWithUsername(MemberResponse response) {
        UserDto user = accountClient.getUserById(response.userId());
        return new MemberResponse(response.userId(), user.username(), response.name(), response.projectRole(),
                response.invitedAt());
    }

    @Override
    @PreAuthorize("@security.canManageProjectMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) {
        Long userId = authUtil.getCurrentUserId();
        Project project = findAccessibleProjectById(projectId, userId);

        Optional<UserDto> invitee = accountClient.getUserByEmail(request.username());
        if (invitee.isEmpty()) {
            emailService.sendProjectInviteEmail(request.username(), project.getName(), false);
            return null;
        }

        if (invitee.get().id().equals(userId)) {
            throw new RuntimeException("Cannot invite yourself");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, invitee.get().id());
        if (projectMemberRepository.existsById(projectMemberId)) {
            throw new RuntimeException("Cannot invite them again!");
        }

        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .projectRole(request.role())
                .invitedAt(Instant.now())
                .build();

        projectMemberRepository.save(projectMember);
        emailService.sendProjectInviteEmail(request.username(), project.getName(), true);
        return projectMemberMapper.toProjectMemberResponseFromMember(projectMember);

    }

    @Override
    @PreAuthorize("@security.canManageProjectMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request) {
        // Long userId = authUtil.getCurrentUserId();
        // Project project = findAccessibleProjectById(projectId, userId);

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        ProjectMember projectMember = projectMemberRepository.findById(projectMemberId).orElseThrow();
        projectMember.setProjectRole(request.role());

        projectMemberRepository.save(projectMember);

        return projectMemberMapper.toProjectMemberResponseFromMember(projectMember);
    }

    @Override
    @PreAuthorize("@security.canManageProjectMembers(#projectId)")
    public void removeProjectMember(Long projectId, Long memberId) {
        // Long userId = authUtil.getCurrentUserId();
        // Project project = findAccessibleProjectById(projectId, userId);

        ProjectMemberId projectMemberId = new ProjectMemberId(projectId, memberId);
        if (!projectMemberRepository.existsById(projectMemberId)) {
            throw new RuntimeException("project member does not exist");
        }

        projectMemberRepository.deleteById(projectMemberId);
    }

    // INTERNAL METHODS
    public Project findAccessibleProjectById(Long projectId, Long userId) {
        return projectRepository.findAccessibleProjectById(projectId, userId).orElseThrow();
    }

}
