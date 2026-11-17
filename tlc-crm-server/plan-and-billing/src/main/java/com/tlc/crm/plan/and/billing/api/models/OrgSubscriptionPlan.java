package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.plans.and.billings.models.Discount;
import com.tlc.plans.and.billings.models.SubscriptionPlan;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents an organization's subscription plan, including details
 * such as associated plan, status, quantity, discounts, and time bounds.
 *
 * @author Sathish
 * @version 1.0
 */
public class OrgSubscriptionPlan implements TlcModel {

    private Long id;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.plan.orgId.not.null")
    private Long orgId;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.plan.subscription.not.null")
    private SubscriptionPlan plan;

    @NotNull(groups = {Group.Create.class, Group.Update.class}, message = "i18n.plan.subscription.quantity.not.null")
    private int quantity;

    private DateTime startTime;

    private DateTime expirationTime;

    private SubscriptionStatus status;
    private PlanContractState contractState;
    private Collection<Discount> discounts;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(final SubscriptionPlan plan) {
        this.plan = plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(final SubscriptionStatus status) {
        this.status = status;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(final int quantity) {
        this.quantity = quantity;
    }

    public Collection<Discount> getDiscounts() {
        return discounts;
    }

    public void setDiscounts(final Collection<Discount> discounts) {
        this.discounts = discounts;
    }

    public void addDiscount(final Discount discount) {
        discounts = Objects.isNull(discounts) ? new ArrayList<>() : discounts;

        discounts.add(discount);
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final DateTime startTime) {
        this.startTime = startTime;
    }

    public DateTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(final DateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    public PlanContractState getContractState() {
        return contractState;
    }

    public void setContractState(final PlanContractState contractState) {
        this.contractState = contractState;
    }

    @Override
    public Object identity() {
        return null;
    }
}