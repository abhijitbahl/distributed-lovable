package com.projects.distributed_lovable.account_service.mapper;

import org.mapstruct.Mapper;

import com.projects.distributed_lovable.account_service.dto.subscription.SubscriptionResponse;
import com.projects.distributed_lovable.account_service.entity.Plan;
import com.projects.distributed_lovable.account_service.entity.Subscription;
import com.projects.distributed_lovable.common_lib.dto.PlanDto;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {
    SubscriptionResponse toSubscriptionResponse(Subscription subscription);

    PlanDto toPlanResponse(Plan plan);
}
