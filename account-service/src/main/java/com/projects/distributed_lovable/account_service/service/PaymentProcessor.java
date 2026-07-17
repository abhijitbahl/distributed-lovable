package com.projects.distributed_lovable.account_service.service;

import java.util.Map;

import com.projects.distributed_lovable.account_service.dto.subscription.CheckoutRequest;
import com.projects.distributed_lovable.account_service.dto.subscription.CheckoutResponse;
import com.projects.distributed_lovable.account_service.dto.subscription.PortalResponse;
import com.stripe.model.StripeObject;

public interface PaymentProcessor {
    CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request);

    PortalResponse openCustomerPortal();

    void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata);
}
