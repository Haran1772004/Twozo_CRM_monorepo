package com.tlc.crm.plan.and.billing.api.models;

import com.tlc.plans.and.billings.models.BillingCycle;

/**
 * Data model representing the activation details necessary for switching subscription plans.
 *
 * Contains identifiers for the organization, the plan that is expiring, and the upcoming plan to be activated.
 * This class serves as a carrier for plan activation data during the plan transition process.
 *
 * @author Sathish
 * @version 1.0
 */
public class PlanActivationRequest {

    private Long orgId;
    private Long upcomingPlanId;
    private OrgSubscriptionPlan oldSubscriptionPlan;
    private BillingCycle upcomingPlanBillingCycle;

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUpcomingPlanId() {
        return upcomingPlanId;
    }

    public void setUpcomingPlanId(final Long upcomingPlanId) {
        this.upcomingPlanId = upcomingPlanId;
    }

    public OrgSubscriptionPlan getOldSubscriptionPlan() {
        return oldSubscriptionPlan;
    }

    public void setOldSubscriptionPlan(final OrgSubscriptionPlan oldSubscriptionPlan) {
        this.oldSubscriptionPlan = oldSubscriptionPlan;
    }

    public BillingCycle getUpcomingPlanBillingCycle() {
        return upcomingPlanBillingCycle;
    }

    public void setUpcomingPlanBillingCycle(final BillingCycle upcomingPlanBillingCycle) {
        this.upcomingPlanBillingCycle = upcomingPlanBillingCycle;
    }
}
