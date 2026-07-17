package com.projects.distributed_lovable.workspace_service.service;

import java.util.List;

import com.projects.distributed_lovable.workspace_service.dto.member.InviteMemberRequest;
import com.projects.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.projects.distributed_lovable.workspace_service.dto.member.UpdateMemberRoleRequest;

public interface ProjectMemberService {

    List<MemberResponse> getProjectMembers(Long projectId);

    MemberResponse inviteMember(Long projectId, InviteMemberRequest request);

    MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request);

    void removeProjectMember(Long projectId, Long memberId);

}
