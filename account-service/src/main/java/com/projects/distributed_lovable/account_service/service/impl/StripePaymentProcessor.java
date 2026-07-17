package com.projects.distributed_lovable.account_service.service.impl;

import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.projects.distributed_lovable.account_service.dto.subscription.CheckoutRequest;
import com.projects.distributed_lovable.account_service.dto.subscription.CheckoutResponse;
import com.projects.distributed_lovable.account_service.dto.subscription.PortalResponse;
import com.projects.distributed_lovable.account_service.entity.Plan;
import com.projects.distributed_lovable.account_service.entity.User;
import com.projects.distributed_lovable.common_lib.enums.SubscriptionStatus;
import com.projects.distributed_lovable.common_lib.error.BadRequestException;
import com.projects.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.projects.distributed_lovable.account_service.repository.PlanRepository;
import com.projects.distributed_lovable.account_service.repository.UserRepository;
import com.projects.distributed_lovable.common_lib.security.AuthUtil;
import com.projects.distributed_lovable.account_service.service.PaymentProcessor;
import com.projects.distributed_lovable.account_service.service.SubscriptionService;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProcessor implements PaymentProcessor {

    private final AuthUtil authUtil;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    @Value("${app.frontend.url}")
    private String frontEndUrl;

    @Override
    public CheckoutResponse createCheckoutSessionUrl(CheckoutRequest request) {
        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Plan ", request.planId().toString()));

        Long userId = authUtil.getCurrentUserId();
        User user = getUser(userId);

        var params = SessionCreateParams.builder()
                .addLineItem(
                        SessionCreateParams.LineItem.builder().setPrice(plan.getStripePriceId()).setQuantity(1L)
                                .build())
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSubscriptionData(
                        new SessionCreateParams.SubscriptionData.Builder()
                                .setBillingMode(SessionCreateParams.SubscriptionData.BillingMode.builder()
                                        .setType(SessionCreateParams.SubscriptionData.BillingMode.Type.FLEXIBLE)
                                        .build())
                                .build())
                .setSuccessUrl(frontEndUrl + "/success=true&session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(frontEndUrl + "/cancel.html")
                .putMetadata("userId", userId.toString())
                .putMetadata("planId", plan.getId().toString());
        try {

            String stripeCustomerId = user.getStripeCustomerId();
            if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
                params.setCustomerEmail(user.getUsername());// create new customer with email
            } else {
                params.setCustomer(stripeCustomerId);// use existing customer ID
            }
            Session session = Session.create(params.build());// make call to stripe API
            return new CheckoutResponse(session.getUrl());
        } catch (StripeException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException("Failed to create Stripe checkout session", e);
        }
    }

    @Override
    public PortalResponse openCustomerPortal() {
        Long userId = authUtil.getCurrentUserId();
        User user = getUser(userId);
        String stripeCustomerId = user.getStripeCustomerId();
        if (stripeCustomerId == null || stripeCustomerId.isEmpty()) {
            throw new BadRequestException("User does not have a Stripe customer ID, User ID: " + userId);
        }
        com.stripe.model.billingportal.Session portalSession;
        try {
            portalSession = com.stripe.model.billingportal.Session.create(
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(stripeCustomerId)
                            .setReturnUrl(frontEndUrl)
                            .build());
            return new PortalResponse(portalSession.getUrl());
        } catch (StripeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void handleWebhookEvent(String type, StripeObject stripeObject, Map<String, String> metadata) {
        log.debug("Handling stripe event of type: {}", type);
        switch (type) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted((Session) stripeObject, metadata);// one time on successful checkout
            case "customer.subscription.updated":
                handleCustomerSubscriptionUpdated((Subscription) stripeObject);// when subscription is updated, canceled
                                                                               // etc.
            case "customer.subscription.deleted":
                handleCustomerSubscriptionDeleted((Subscription) stripeObject);// when subscription is deleted/expired,
                                                                               // ends. Revoke access
            case "invoice.paid":
                handleInvoicePaid((Invoice) stripeObject);// when invoice is paid, renewals
            case "invoice.payment_failed":
                handleInvoicePaymentFailed((Invoice) stripeObject);// when invoice payment fails, marked as PAST_DUE
            default:
                log.debug("Ignoring the  event type: {}", type);
        }
    }

    private void handleInvoicePaymentFailed(Invoice invoice) {
        String subscriptionId = extractSubscriptionIdFromInvoice(invoice);
        if (subscriptionId == null) {
            log.error("No subscription ID found in invoice {}", invoice.getId());
            return;
        }
        subscriptionService.markSubscriptionAsPastDue(subscriptionId);
    }

    private void handleInvoicePaid(Invoice invoice) {
        String subscriptionId = extractSubscriptionIdFromInvoice(invoice);
        if (subscriptionId == null) {
            log.error("No subscription ID found in invoice {}", invoice.getId());
            return;
        }
        // Fetch the subscription object from Stripe
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);// sdk making API call to stripe
            var item = subscription.getItems().getData().get(0);
            Instant periodStart = toInstant(item.getCurrentPeriodStart());
            Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

            subscriptionService.renewSubscriptionPeriod(subscriptionId, periodStart, periodEnd);
        } catch (StripeException e) {
            log.error("Failed to retrieve subscription {} from Stripe: {}", subscriptionId, e.getMessage());
            return;
        }
    }

    private void handleCustomerSubscriptionDeleted(Subscription subscription) {
        if (subscription == null) {
            log.error("Stripe subscription object is null in handleCustomerSubscriptionDeleted");
            return;
        }
        subscriptionService.cancelSubscription(subscription.getId());

    }

    private void handleCustomerSubscriptionUpdated(Subscription subscription) {
        if (subscription == null) {
            log.error("Stripe subscription object is null in handleCustomerSubscriptionUpdated");
            return;
        }

        SubscriptionStatus status = mapStripeStatusToEnum(subscription.getStatus());
        if (status == null) {
            log.warn("Unknown status '{}' for subscription {}", subscription.getStatus(), subscription.getId());
            return;
        }

        SubscriptionItem item = subscription.getItems().getData().get(0);
        Instant periodStart = toInstant(item.getCurrentPeriodStart());
        Instant periodEnd = toInstant(item.getCurrentPeriodEnd());

        Long planId = resolvePlanId(item.getPrice());
        subscriptionService.updateSubscriptionStatus(subscription.getId(), status, planId, periodStart,
                subscription.getCancelAtPeriodEnd(), periodEnd);
    }

    private void handleCheckoutSessionCompleted(Session session, Map<String, String> metadata) {
        if (session == null) {
            log.error("Stripe session object is null in webhook handling");
            return;
        }
        Long userId = Long.parseLong(metadata.get("userId"));
        Long planId = Long.parseLong(metadata.get("planId"));
        String subscriptionId = session.getSubscription();
        String customerId = session.getCustomer();
        User user = getUser(userId);
        if (user.getStripeCustomerId() == null || user.getStripeCustomerId().isEmpty()) {
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
        }

        subscriptionService.activateSubscription(userId, planId, subscriptionId, customerId);
    }

    // Helper methods
    private Long resolvePlanId(Price price) {
        if (price == null || price.getId() == null) {
            return null;
        }
        return planRepository.findByStripePriceId(price.getId())
                .map(Plan::getId)
                .orElse(null);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new ResourceNotFoundException("User ", userId.toString()));
    }

    private Instant toInstant(Long epoch) {
        return epoch != null ? Instant.ofEpochSecond(epoch) : null;
    }

    private SubscriptionStatus mapStripeStatusToEnum(String status) {
        switch (status) {
            case "active":
                return SubscriptionStatus.ACTIVE;
            case "trialing":
                return SubscriptionStatus.TRAILING;
            case "canceled":
                return SubscriptionStatus.CANCELED;
            case "past_due", "unpaid", "paused", "incomplete_expired":
                return SubscriptionStatus.PAST_DUE;
            case "incomplete":
                return SubscriptionStatus.INCOMPLETE;
            default:
                log.warn("Unknown subscription status: {}", status);
                return null;
        }
    }

    private String extractSubscriptionIdFromInvoice(Invoice invoice) {
        var parent = invoice.getParent();
        if (parent == null) {
            log.warn("Invoice {} does not have a subscription", invoice.getId());
            return null;
        }

        var subDetails = parent.getSubscriptionDetails();
        return subDetails != null ? subDetails.getSubscription() : null;
    }

}
