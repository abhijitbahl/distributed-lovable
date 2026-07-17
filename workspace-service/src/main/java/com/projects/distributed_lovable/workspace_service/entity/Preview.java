package com.projects.distributed_lovable.workspace_service.entity;

import java.time.Instant;

import com.projects.distributed_lovable.common_lib.enums.PreviewStatus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Preview {
    Long id;
    Project project;

    String namespace;

    String podName;

    String previewUrl;

    PreviewStatus status;

    Instant startedAt;

    Instant terminatedAt;

    Instant createdAt;
}
