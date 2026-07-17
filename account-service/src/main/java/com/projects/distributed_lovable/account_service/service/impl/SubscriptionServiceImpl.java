package com.projects.distributed_lovable.account_service.service.impl;

import java.time.Instant;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.projects.distributed_lovable.account_service.entity.Plan;
import com.projects.distributed_lovable.account_service.entity.Subscription;
import com.projects.distributed_lovable.account_service.entity.User;
import com.projects.distributed_lovable.account_service.mapper.SubscriptionMapper;
import com.projects.distributed_lovable.account_service.repository.PlanRepository;
import com.projects.distributed_lovable.account_service.repository.SubscriptionRepository;
import com.projects.distributed_lovable.account_service.repository.UserRepository;
import com.projects.distributed_lovable.account_service.service.SubscriptionService;
import com.projects.distributed_lovable.common_lib.dto.PlanDto;
import com.projects.distributed_lovable.common_lib.enums.SubscriptionStatus;
import com.projects.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.projects.distributed_lovable.common_lib.security.AuthUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final AuthUtil authUtil;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final Integer FREE_TIER_PROJECTS_LIMIT = 100;

    @Override
    public SubscriptionResponse getCurrentSubscription() {
        Long userId = authUtil.getCurrentUserId();
        var currentSubscription = subscriptionRepository.findByUserIdAndStatusIn(userId,
                Set.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE, SubscriptionStatus.TRAILING))
                .orElse(new Subscription());
        return subscriptionMapper.toSubscriptionResponse(currentSubscription);
    }

    @Override
    public void activateSubscription(Long userId, Long planId, String subscriptionId, String customerId) {
        boolean exists = subscriptionRepository.existsByStripeSubscriptionId(subscriptionId);
        if (exists) {
            log.warn("Subscription with Stripe subscription ID {} already exists, skipping activation", subscriptionId);
            return;
        }

        User user = getUser(userId);
        Plan plan = getPlan(planId);
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .stripeSubscriptionId(subscriptionId)
                .status(SubscriptionStatus.INCOMPLETE) // set to incomplete until we get confirmation from webhook that
                                                       // subscription is active, to avoid issues with delayed webhooks
                                                       // and users trying to use the app before subscription is active
                .build();
        subscriptionRepository.save(subscription);

    }

    @Override
    @Transactional
    public void updateSubscriptionStatus(String gatewaySubscriptionId, SubscriptionStatus status, Long planId,
            Instant periodStart,
            Boolean cancelAtPeriodEnd, Instant periodEnd) {

        boolean hasSubscriptionUpdated = false;
        Subscription subscription = getSubscription(gatewaySubscriptionId);
        if (status != null && status != subscription.getStatus()) {
            subscription.setStatus(status);
            hasSubscriptionUpdated = true;
        }
        if (planId != null && !planId.equals(subscription.getPlan().getId())) {
            Plan plan = getPlan(planId);
            subscription.setPlan(plan);
            hasSubscriptionUpdated = true;
        }
        if (periodStart != null && !periodStart.equals(subscription.getCurrentPeriodStart())) {
            subscription.setCurrentPeriodStart(periodStart);
            hasSubscriptionUpdated = true;
        }
        if (periodEnd != null && !periodEnd.equals(subscription.getCurrentPeriodEnd())) {
            subscription.setCurrentPeriodEnd(periodEnd);
            hasSubscriptionUpdated = true;
        }
        if (cancelAtPeriodEnd != null && !cancelAtPeriodEnd.equals(subscription.getCancelAtPeriodEnd())) {
            subscription.setCancelAtPeriodEnd(cancelAtPeriodEnd);
            hasSubscriptionUpdated = true;
        }
        if (hasSubscriptionUpdated) {
            log.info(
                    "Updating subscription with Stripe subscription ID {}: status={}, planId={}, periodStart={}, periodEnd={}, cancelAtPeriodEnd={}",
                    gatewaySubscriptionId, status, planId, periodStart, periodEnd, cancelAtPeriodEnd);
            subscriptionRepository.save(subscription);
        }
    }

    @Override
    public void cancelSubscription(String gatewaySubscriptionId) {
        Subscription subscription = getSubscription(gatewaySubscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            log.warn("Subscription with Stripe subscription ID {} is already canceled", gatewaySubscriptionId);
            return;
        }
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscriptionRepository.save(subscription);

        // Notify user via email or in-app notification that their subscription has been
        // canceled and they will lose access to the app at the end of the current
        // billing period. This can be done asynchronously via an event or message queue
        // to avoid slowing down the webhook response.
    }

    @Override
    public void renewSubscriptionPeriod(String gatewaySubscriptionId, Instant periodStart, Instant periodEnd) {
        Subscription subscription = getSubscription(gatewaySubscriptionId);
        Instant newStart = periodStart != null ? periodStart : subscription.getCurrentPeriodEnd();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(periodEnd);

        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE
                || subscription.getStatus() == SubscriptionStatus.INCOMPLETE) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
        }

        subscriptionRepository.save(subscription);
    }

    @Override
    public void markSubscriptionAsPastDue(String gatewaySubscriptionId) {
        Subscription subscription = getSubscription(gatewaySubscriptionId);
        if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
            log.warn("Subscription with Stripe subscription ID {} is already marked as past due",
                    gatewaySubscriptionId);
            return;
        }
        subscription.setStatus(SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);

        // Notify user via email or in-app notification that their subscription is past
        // due and they need to update their payment method to avoid cancellation. This
        // can be done asynchronously via an event or message queue to avoid slowing
        // down the webhook response.
    }

    // utility methods
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    public Plan getPlan(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId.toString()));
    }

    public Subscription getSubscription(String gatewaySubscriptionId) {
        return subscriptionRepository.findByStripeSubscriptionId(gatewaySubscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", gatewaySubscriptionId));
    }

    @Override
    public PlanDto getCurrentSubscribedPlanByUser() {
        SubscriptionResponse subscriptionResponse = getCurrentSubscription();
        return subscriptionResponse.plan();
    }
}
