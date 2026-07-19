package com.projects.distributed_lovable.common_lib.event;

public record UserSignedUpEvent(
        Long userId,
        String email) {
}
