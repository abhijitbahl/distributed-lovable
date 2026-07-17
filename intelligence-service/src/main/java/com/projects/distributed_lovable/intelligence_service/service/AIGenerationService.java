package com.projects.distributed_lovable.intelligence_service.service;

import com.projects.distributed_lovable.intelligence_service.dto.StreamResponse;

import reactor.core.publisher.Flux;

public interface AIGenerationService {

    Flux<StreamResponse> streamResponse(String message, Long projectId);

}
