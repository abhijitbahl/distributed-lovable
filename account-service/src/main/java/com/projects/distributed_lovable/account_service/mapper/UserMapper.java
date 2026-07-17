package com.projects.distributed_lovable.account_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.projects.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.projects.distributed_lovable.account_service.dto.auth.UserProfileResponse;
import com.projects.distributed_lovable.account_service.entity.User;
import com.projects.distributed_lovable.common_lib.dto.UserDto;
import com.projects.distributed_lovable.common_lib.security.JwtUserPrincipal;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(SignupRequest signupRequest);

    @Mapping(source = "userId", target = "id")
    UserProfileResponse toUserProfileResponse(JwtUserPrincipal user);

    UserDto toUserDto(User user);
}
