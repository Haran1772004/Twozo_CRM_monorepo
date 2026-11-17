package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.plans.and.billings.models.PlanType;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the subscription plans for a specific organization.
 *
 * @author Sathish
 * @version 1.0
 */
public class OrgSubscriptions implements TlcModel {

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.plan.orgId.not.null")
    private Long orgId;

    @Valid
    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.plan.subscription.not.null")
    @NotEmpty(groups = {Group.Create.class, Group.Update.class}, message = "i18n.plan.subscription.not.null")
    private Map<PlanType, Collection<OrgSubscriptionPlan>> orgSubscriptionPlans;

    private BillingDetails billingDetails;

    private Long userId;

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setOrgPlans(final Map<PlanType, Collection<OrgSubscriptionPlan>> orgSubscriptionPlans) {
        this.orgSubscriptionPlans = orgSubscriptionPlans;
    }

    public void addOrgPlans(final OrgSubscriptionPlan orgSubscriptionPlan) {
        orgSubscriptionPlans = Objects.isNull(orgSubscriptionPlans) ? new HashMap<>() : orgSubscriptionPlans;

        orgSubscriptionPlans.computeIfAbsent(orgSubscriptionPlan.getPlan().getPlanType(),
                planType -> new ArrayList<>()).add(orgSubscriptionPlan);
    }

    public Collection<OrgSubscriptionPlan> getOrgPlans() {
        return orgSubscriptionPlans.values().stream().flatMap(Collection::stream).toList();
    }

    public Optional<OrgSubscriptionPlan> getOrdinaryPlan() {
        return Optional.ofNullable(orgSubscriptionPlans).flatMap(
                orgSubscriptions -> orgSubscriptions.getOrDefault(PlanType.ORDINARY, Collections.emptyList()).stream().findFirst());
    }

    public Optional<OrgSubscriptionPlan> getAddOnPlan(final long planId) {
        return Optional.ofNullable(orgSubscriptionPlans)
                .flatMap(orgSubscriptions -> orgSubscriptions.getOrDefault(PlanType.ADD_ON, Collections.emptyList())
                        .stream().filter(orgSubscriptionPlan -> Objects.equals(planId, orgSubscriptionPlan.getPlan().getId())).findFirst());
    }

    public BillingDetails getBillingDetails() {
        return billingDetails;
    }

    public void setBillingDetails(final BillingDetails billingDetails) {
        this.billingDetails = billingDetails;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    @Override
    public Long id() {
        return null;
    }

    @Override
    public Object identity() {
        return null;
    }
}