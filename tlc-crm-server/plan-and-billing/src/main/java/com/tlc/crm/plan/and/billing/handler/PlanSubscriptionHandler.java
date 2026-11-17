package com.tlc.crm.plan.and.billing.handler;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.models.BillingDetails;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptions;
import com.tlc.crm.plan.and.billing.api.models.PlanActivationRequest;
import com.tlc.crm.plan.and.billing.api.models.PlanCancellationData;
import com.tlc.crm.plan.and.billing.api.models.SubscriptionResponse;
import com.tlc.crm.plan.and.billing.internal.status.SubscriptionErrorCodes;
import com.tlc.crm.plan.and.billing.util.TransactionIdUtil;

import com.tlc.payment.gateway.models.Payment;
import com.tlc.payment.gateway.models.PaymentResponse;
import com.tlc.payment.gateway.service.PaymentService;
import com.tlc.payment.gateway.service.provider.PaymentServiceProvider;

import com.tlc.plans.and.billings.models.PlanType;
import com.tlc.plans.and.billings.models.SubscriptionPlan;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handler class orchestrating organization subscription-related actions such as subscribing
 * to new plans and (eventually) cancelling subscriptions.
 *
 * <p>
 * This class acts as a service boundary for business logic that sits above
 * raw CRUD operations, integration of payment gateways,
 * automated renewals, audit triggers, etc.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PlanSubscriptionHandler {

    private static final String ORDER_ITEM = "CRM Plan Subscription";

    private final OrgPlanManager orgPlanManager;
    private final PaymentService paymentService;

    private PlanSubscriptionHandler() {
        orgPlanManager = OrgPlanManager.getInstance();
        paymentService = PaymentServiceProvider.get();
    }

    private static final class InstanceHolder {
        private static final PlanSubscriptionHandler PLAN_SUBSCRIPTION_HANDLER = new PlanSubscriptionHandler();
    }

    /**
     * Returns the singleton instance of this handler.
     *
     * @return PlanSubscriptionHandler (singleton)
     */
    public static PlanSubscriptionHandler getInstance() {
        return InstanceHolder.PLAN_SUBSCRIPTION_HANDLER;
    }

    /**
     * Subscribes the plan for an organization.
     *
     * @param orgSubscriptions Represents the plans to be updated for the organization
     */
    public SubscriptionResponse subscribe(final OrgSubscriptions orgSubscriptions) {
        final Payment payment = new Payment();
        final BillingDetails billingDetails = orgSubscriptions.getBillingDetails();
        final String transactionId = TransactionIdUtil.generateTransactionId(orgSubscriptions.orgId(), orgSubscriptions.getUserId());

        billingDetails.setTransactionId(transactionId);
        orgPlanManager.upsert(orgSubscriptions);
        payment.setOrderItem(ORDER_ITEM);
        payment.setPrice(billingDetails.getPrice());
        payment.setPayer(billingDetails.getContact());
        payment.setTransactionId(transactionId);
        payment.setLocalTransactionId(billingDetails.getLocalTransactionId());
        final PaymentResponse paymentResponse = paymentService.makePayment(payment);
        final SubscriptionResponse subscriptionResponse = new SubscriptionResponse();

        subscriptionResponse.setStatus(paymentResponse.getStatus());
        subscriptionResponse.setCheckoutUrl(paymentResponse.getCheckoutUrl());
        return subscriptionResponse;
    }

    /**
     * Cancels the organization subscriptions.
     * <p>
     * Logic to reverse billing, terminate access rights,
     * trigger notifications, and audit events on plan cancellation.
     * </p>
     *
     * @param planCancellationData plans cancellation data of the organization
     */
    public void cancelSubscription(final PlanCancellationData planCancellationData) {
        if (orgPlanManager.isPaidPlan(planCancellationData.orgId(), planCancellationData.id())) {
            orgPlanManager.cancelPlan(planCancellationData);
        } else {
            throw ErrorCode.get(SubscriptionErrorCodes.INVALID_PLAN, "i18n.plan.subscription.cancellation.failed");
        }
    }

    /**
     * Reactivates the cancelled plan of an organization.
     *
     * @param orgId  The unique identifier of an organization.
     * @param planId The unique identifier of the plan.
     */
    public void reactivatePlan(final long orgId, final long planId) {
        if (orgPlanManager.isPlanExpired(orgId, planId)) {
            throw ErrorCode.get(SubscriptionErrorCodes.PLAN_ALREADY_EXPIRED, "i18n.plan.subscription.reactivation.failed");
        } else {
            orgPlanManager.reactivatePlan(orgId, planId);
        }
    }

    /**
     * Triggers the expiration process for organization subscription plans.
     *
     * This method performs the following steps:
     * 1. Retrieves all subscription plans that require expiration updates.
     * 2. Extracts the organization IDs associated with these expired plans.
     * 3. Fetches upcoming plans for these organizations.
     * 4. Constructs a list of PlanActivationRequest objects, pairing expired plans
     *    with their respective upcoming plans when available.
     * 5. Invokes the plan switching mechanism by passing the prepared data list.
     *
     * This encapsulates the logic for transitioning organizations from expired
     * plans to new active plans in a single cohesive operation.
     */
    public void triggerExpirationProcess() {
        final ExpiredPlansPageHandler expiredPlansPageHandler = new ExpiredPlansPageHandler();

        do {
            final Collection<OrgSubscriptions> expiredPlans = expiredPlansPageHandler.getCurrentPage().getData();

            if (Objects.nonNull(expiredPlans)) {
                final Collection<Long> orgIds = expiredPlans.stream().map(OrgSubscriptions::orgId).collect(Collectors.toSet());

                if (!orgIds.isEmpty()) {
                    final Map<Long, OrgSubscriptions> upcomingPlans = orgPlanManager.getUpcomingPlans(orgIds).stream()
                            .collect(Collectors.toMap(OrgSubscriptions::orgId, Function.identity()));
                    final Collection<PlanActivationRequest> planActivationRequests = new HashSet<>();

                    for (final OrgSubscriptions expiredOrgSubscriptions : expiredPlans) {
                        final Long orgId = expiredOrgSubscriptions.orgId();
                        final OrgSubscriptions orgSubscriptions = upcomingPlans.getOrDefault(orgId, null);

                        for (final OrgSubscriptionPlan expiredOrgSubscription : expiredOrgSubscriptions.getOrgPlans()) {
                            final PlanActivationRequest planActivationRequest = new PlanActivationRequest();

                            if (Objects.nonNull(orgSubscriptions)) {
                                final SubscriptionPlan subscriptionPlan = expiredOrgSubscription.getPlan();
                                final Optional<OrgSubscriptionPlan> optionalOrgSubscriptionPlan;

                                if (PlanType.ADD_ON == subscriptionPlan.getPlanType()) {
                                    optionalOrgSubscriptionPlan = orgSubscriptions.getAddOnPlan(subscriptionPlan.getId());
                                } else {
                                    optionalOrgSubscriptionPlan = orgSubscriptions.getOrdinaryPlan();
                                }

                                optionalOrgSubscriptionPlan.ifPresent(orgSubscriptionPlan -> {
                                    planActivationRequest.setUpcomingPlanId(orgSubscriptionPlan.id());
                                    planActivationRequest.setUpcomingPlanBillingCycle(orgSubscriptionPlan.getPlan().getPricingScheme().getBillingCycle());
                                });
                            }
                            planActivationRequest.setOrgId(orgId);
                            planActivationRequest.setOldSubscriptionPlan(expiredOrgSubscription);
                            planActivationRequests.add(planActivationRequest);
                        }
                    }
                    orgPlanManager.switchPlan(planActivationRequests);
                }
            }
        } while (expiredPlansPageHandler.navigateToNextPageIfAvailable());
    }
}