package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.plans.and.billings.models.CancellationFeedbacks;
import com.tlc.plans.and.billings.models.CancellationReason;

import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collection;

/**
 * This class represents the data related to plan cancellation for an organization.
 *
 * @author Sathish
 * @version 1.0
 */
public class PlanCancellationData implements TlcModel {

    @NotNull(groups = Group.Create.class, message = "i18n.plan.cancellation.orgId.notnull")
    private Long orgId;

    @NotNull(groups = Group.Create.class, message = "i18n.plan.cancellation.userId.notnull")
    private Long userId;

    @NotNull(groups = Group.Create.class, message = "i18n.plan.cancellation.planId.notnull")
    private Long planId;

    @Size(min = 1, groups = Group.Create.class, message = "i18n.plan.cancellation.reason.notEmpty")
    private Collection<CancellationReason> cancellationReasons;

    @NotNull(groups = Group.Create.class, message = "i18n.plan.cancellation.feedback.notnull")
    private CancellationFeedbacks cancellationFeedbacks;

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setId(final Long planId) {
        this.planId = planId;
    }

    public Collection<CancellationReason> getCancellationReasons() {
        return cancellationReasons;
    }

    public void setCancellationReasons(final Collection<CancellationReason> cancellationReasons) {
        this.cancellationReasons = cancellationReasons;
    }

    public CancellationFeedbacks getCancellationFeedbacks() {
        return cancellationFeedbacks;
    }

    public void setCancellationFeedbacks(final CancellationFeedbacks cancellationFeedbacks) {
        this.cancellationFeedbacks = cancellationFeedbacks;
    }

    @Override
    public Long id() {
        return planId;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}
