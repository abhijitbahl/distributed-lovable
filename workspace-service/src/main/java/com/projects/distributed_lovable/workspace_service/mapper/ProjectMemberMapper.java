package com.projects.distributed_lovable.workspace_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.projects.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.projects.distributed_lovable.workspace_service.entity.ProjectMember;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    @Mapping(target = "userId", source = "id.userId")
    MemberResponse toProjectMemberResponseFromMember(ProjectMember projectMember);
}
