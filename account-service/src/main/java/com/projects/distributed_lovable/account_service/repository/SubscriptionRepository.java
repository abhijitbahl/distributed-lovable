package com.projects.distributed_lovable.account_service.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

import com.projects.distributed_lovable.account_service.entity.Subscription;
import com.projects.distributed_lovable.common_lib.enums.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Get the current active, past_due or trailing subscription for a user (only
    // one should exist at a time)
    Optional<Subscription> findByUserIdAndStatusIn(Long userId, Set<SubscriptionStatus> statusSet);

    boolean existsByStripeSubscriptionId(String subscriptionId);

    Optional<Subscription> findByStripeSubscriptionId(String gatewaySubscriptionId);

}
