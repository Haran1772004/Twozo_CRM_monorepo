package com.tlc.crm.core.api.insights.service;

import com.tlc.crm.core.api.deal.model.DealStatistics;
import com.tlc.crm.core.api.deal.model.EmailSummary;
import com.tlc.crm.core.api.insights.models.EntityInsightsData;
import com.tlc.crm.core.api.insights.manager.EntityInsightsManager;
import com.tlc.crm.core.api.insights.manager.impl.EntityInsightsManagerImpl;
import com.tlc.crm.core.api.insights.models.ActivityInsights;
import com.tlc.crm.core.api.insights.models.DealInsights;
import com.tlc.crm.core.api.insights.models.EmailInsights;
import com.tlc.crm.core.api.summaryInfo.model.SummaryInfo;
import com.tlc.crm.core.features.api.activity.model.ActivitySummary;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.l10n.temporal.model.Date;

import java.util.Objects;

/**
 * <p>
 * Represents the entity insights to summary mapper helper
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class EntityInsightsToSummaryInfoMapper {

    private final EntityInsightsManager entityInsightsManager;
    private final ActivityTypeManager activityTypeManager;

    private EntityInsightsToSummaryInfoMapper() {
        this.entityInsightsManager = EntityInsightsManagerImpl.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
    }

    private static final class InstanceHolder {
        private static final EntityInsightsToSummaryInfoMapper INSTANCE = new EntityInsightsToSummaryInfoMapper();
    }

    /**
     * <p>
     * Gets the {@link EntityInsightsToSummaryInfoMapper} singleton.
     * </p>
     *
     * @return The mapper instance.
     */
    public static EntityInsightsToSummaryInfoMapper getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Build the materialized insights row for the given entity and copies its
     * activity / email / deal columns into a new {@link SummaryInfo}.
     * </p>
     *
     * @param orgId    Represents the organization id.
     * @param entityId Represents the entity id.
     * @param source   Represents the entity type.
     * @return A {@link SummaryInfo} populated from the materialized row.
     */
    public SummaryInfo buildSummaryInfo(final long orgId, final long entityId, final Source source) {
        final EntityInsightsData entityInsightsData = entityInsightsManager.get(orgId, entityId, source);
        final SummaryInfo summaryInfo = new SummaryInfo();

        summaryInfo.setActivitySummary(toActivitySummary(orgId, entityInsightsData.getActivityInsights()));
        summaryInfo.setEmailSummary(toEmailSummary(entityInsightsData.getEmailInsights()));
        summaryInfo.setDealStatistics(toDealStatistics(entityInsightsData.getDealInsights()));

        return summaryInfo;
    }

    /**
     * <p>
     * Converts ActivityInsights to ActivitySummary, resolving the activity type name from ID.
     * </p>
     *
     * @param orgId            Represents the organization ID (for resolving activity type name)
     * @param activityInsights the activity insights
     * @return the ActivitySummary with resolved activity type name
     */
    private ActivitySummary toActivitySummary(final long orgId, final ActivityInsights activityInsights) {
        final ActivitySummary activitySummary = new ActivitySummary();

        if (Objects.isNull(activityInsights)) {
            return activitySummary;
        }
        activitySummary.setDoneActivities(activityInsights.getDoneActivities());
        activitySummary.setUpcomingActivities(activityInsights.getUpcomingActivities());
        activitySummary.setPendingActivities(activityInsights.getPendingActivities());
        activitySummary.setTotalActivities(activityInsights.getTotalActivities());
        activitySummary.setLastActivityDate(toDate(activityInsights.getLastActivityDate()));
        activitySummary.setNextActivityDate(toDate(activityInsights.getNextActivityDate()));
        final Long activityTypeId = activityInsights.getLastActivityTypeId();

        if (Objects.nonNull(activityTypeId)) {
            activitySummary.setLastActivityType(activityTypeManager.fetchName(orgId, activityTypeId));
        }

        return activitySummary;
    }

    /**
     * <p>
     * To email summary.
     * </p>
     *
     * @param emailInsights the email insights
     * @return the to email summary value
     */
    private EmailSummary toEmailSummary(final EmailInsights emailInsights) {
        final EmailSummary emailSummary = new EmailSummary();

        if (Objects.isNull(emailInsights)) {
            return emailSummary;
        }
        emailSummary.setEmailMessageCount(emailInsights.getEmailMessageCount());
        emailSummary.setLastEmailSent(toDate(emailInsights.getLastEmailSent()));
        emailSummary.setLastEmailReceived(toDate(emailInsights.getLastEmailReceived()));

        return emailSummary;
    }

    /**
     * <p>
     * To deal statistics.
     * </p>
     *
     * @param dealInsights the deal insights
     * @return the to deal statistics value
     */
    private DealStatistics toDealStatistics(final DealInsights dealInsights) {
        final DealStatistics dealStatistics = new DealStatistics();

        if (Objects.isNull(dealInsights)) {
            return dealStatistics;
        }
        dealStatistics.setOpenDealsCount(dealInsights.getOpenDealsCount());
        dealStatistics.setWonDealsCount(dealInsights.getWonDealsCount());
        dealStatistics.setLostDealsCount(dealInsights.getLostDealsCount());

        return dealStatistics;
    }


    /**
     * <p>
     * To date.
     * </p>
     *
     * @param epochMillis the epoch millis
     * @return the to date value
     */
    private Date toDate(final Long epochMillis) {
        return (Objects.nonNull(epochMillis) && epochMillis > 0) ? new Date(epochMillis) : null;
    }
}