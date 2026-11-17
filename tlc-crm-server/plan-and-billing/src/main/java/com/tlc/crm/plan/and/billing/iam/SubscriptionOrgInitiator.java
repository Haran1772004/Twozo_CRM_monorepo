package com.tlc.crm.plan.and.billing.iam;

import com.tlc.crm.common.iam.OrgInitiator;
import com.tlc.crm.common.iam.OrgInitiatorAction;
import com.tlc.crm.common.iam.api.model.CrmOrg;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.plan.and.billing.api.manager.OrgPlanManager;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptionPlan;
import com.tlc.crm.plan.and.billing.api.models.OrgSubscriptions;
import com.tlc.crm.plan.and.billing.api.models.PlanContractState;
import com.tlc.crm.plan.and.billing.api.models.SubscriptionStatus;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.plans.and.billings.manager.SubscriptionPlanManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;

/**
 * An action performed by the subscription org initializer to initialize organizational fields
 * with predefined values upon the creation of a new organization.
 *
 * @author Sathish
 * @version 1.0
 */
@OrgInitiator(name = "subscription")
public class SubscriptionOrgInitiator implements OrgInitiatorAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionOrgInitiator.class);
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");
    private static final short TRIAL_DAYS_COUNT = 14;

    private final OrgPlanManager orgPlanManager;
    private final SubscriptionPlanManager subscriptionPlanManager;

    public SubscriptionOrgInitiator() {
        orgPlanManager = OrgPlanManager.getInstance();
        subscriptionPlanManager = SubscriptionPlanManager.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmOrg  Represents the {@link CrmOrg}
     * @param crmUser Represents the {@link CrmUser}
     */
    @Override
    public void execute(final CrmOrg crmOrg, final CrmUser crmUser) {
        final Long orgId = crmOrg.id();
        final Long userId = crmUser.id();
        final OrgSubscriptions orgSubscriptions = new OrgSubscriptions();
        final OrgSubscriptionPlan orgSubscriptionPlan = new OrgSubscriptionPlan();
        final Instant currentTime = Instant.now();

        orgSubscriptions.setOrgId(orgId);
        orgSubscriptionPlan.setOrgId(orgId);
        orgSubscriptionPlan.setContractState(PlanContractState.PRESENT);
        orgSubscriptionPlan.setQuantity(1);
        orgSubscriptionPlan.setStatus(SubscriptionStatus.TRIAL);
        orgSubscriptionPlan.setStartTime(new DateTime(currentTime.toEpochMilli()));
        orgSubscriptionPlan.setExpirationTime(new DateTime(currentTime.atZone(UTC_ZONE_ID).plusDays(TRIAL_DAYS_COUNT).toInstant().toEpochMilli()));
        orgSubscriptionPlan.setPlan(subscriptionPlanManager.getDefaultTrialPlan());
        orgSubscriptions.addOrgPlans(orgSubscriptionPlan);

        orgPlanManager.upsert(orgSubscriptions);
        LOGGER.info("Org Init Action: Plans and Billings initialization completed. Org ID: {}, User ID: {}", orgId, userId);
    }
}
