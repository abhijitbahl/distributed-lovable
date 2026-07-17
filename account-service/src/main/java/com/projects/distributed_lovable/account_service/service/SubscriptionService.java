package com.projects.distributed_lovable.account_service.service;

import java.time.Instant;

import com.projects.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.projects.distributed_lovable.common_lib.dto.PlanDto;
import com.projects.distributed_lovable.common_lib.enums.SubscriptionStatus;

public interface SubscriptionService {

    SubscriptionResponse getCurrentSubscription();

    void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId);

    void updateSubscriptionStatus(String gatewaySubscriptionId, SubscriptionStatus status, Long planId,
            Instant periodStart,
            Boolean cancelAtPeriodEnd, Instant periodEnd);

    void cancelSubscription(String gatewaySubscriptionId);

    void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd);

    void markSubscriptionAsPastDue(String gatewaySubscriptionId);

    PlanDto getCurrentSubscribedPlanByUser();
}
