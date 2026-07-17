package com.projects.distributed_lovable.common_lib.event;

import lombok.Builder;

@Builder
public record FileStoreResponseEvent(
        String sagaId,
        Boolean success,
        String errorMessage,
        Long projectId) {

}
