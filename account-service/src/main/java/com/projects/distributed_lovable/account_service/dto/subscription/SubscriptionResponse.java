package com.projects.distributed_lovable.account_service.dto.subscription;

import java.time.Instant;

import com.projects.distributed_lovable.common_lib.dto.PlanDto;

public record SubscriptionResponse(
                PlanDto plan,
                String status,
                Instant currentPeriodEnd,
                Long tokensUsedThisCycle) {

}
