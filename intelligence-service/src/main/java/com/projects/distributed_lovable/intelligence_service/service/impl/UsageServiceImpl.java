package com.projects.distributed_lovable.intelligence_service.service.impl;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.projects.distributed_lovable.common_lib.dto.PlanDto;
import com.projects.distributed_lovable.common_lib.security.AuthUtil;
import com.projects.distributed_lovable.intelligence_service.client.AccountClient;
import com.projects.distributed_lovable.intelligence_service.entity.UsageLog;
import com.projects.distributed_lovable.intelligence_service.repository.UsageLogRepository;
import com.projects.distributed_lovable.intelligence_service.service.UsageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UsageServiceImpl implements UsageService {

    private final UsageLogRepository usageLogRepository;
    private final AccountClient accountClient;
    private final AuthUtil authUtil;

    @Override
    public void recordTokenUsage(Long userId, Integer actualTokens) {
        LocalDate today = LocalDate.now();

        UsageLog todayLog = usageLogRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> {
                    UsageLog newUsageLog = createNewDailyLog(userId, today);
                    return usageLogRepository.save(newUsageLog);
                });
        todayLog.setTokensUsed(todayLog.getTokensUsed() + actualTokens);
        usageLogRepository.save(todayLog);
    }

    @Override
    public void checkDailyTokenUsage() {
        Long userId = authUtil.getCurrentUserId();
        PlanDto plan = accountClient.getCurrentSubscriptionPlanByUser();

        LocalDate today = LocalDate.now();
        UsageLog todayLog = usageLogRepository.findByUserIdAndDate(userId, today)
                .orElseGet(() -> {
                    UsageLog newUsageLog = createNewDailyLog(userId, today);
                    return usageLogRepository.save(newUsageLog);
                });

        if (plan.unlimitedAi())
            return; // If the user has an unlimited plan, no need to check usage
        int currentUsage = todayLog.getTokensUsed();
        int dailyLimit = plan.maxTokensPerDay();

        if (currentUsage >= dailyLimit) {
            log.warn("User {} has exceeded the daily token limit. Current usage: {}, Daily limit: {}", userId,
                    currentUsage, dailyLimit);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Daily token limit exceeded. Please upgrade your subscription or wait until tomorrow.");
        }
    }

    private UsageLog createNewDailyLog(Long userId, LocalDate today) {
        UsageLog newUsageLog = new UsageLog();
        newUsageLog.setUserId(userId);
        newUsageLog.setDate(today);
        newUsageLog.setTokensUsed(0);
        return newUsageLog;
    }

}
