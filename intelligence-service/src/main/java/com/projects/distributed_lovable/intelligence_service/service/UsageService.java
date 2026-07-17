package com.projects.distributed_lovable.intelligence_service.service;

public interface UsageService {

    void recordTokenUsage(Long userId, Integer actualTokens);

    void checkDailyTokenUsage();

}
