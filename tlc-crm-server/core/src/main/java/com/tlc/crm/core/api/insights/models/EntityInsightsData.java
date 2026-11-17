package com.tlc.crm.core.api.insights.models;

import com.tlc.crm.entity.fields.source.Source;

/**
 * <p>
 * Represents the entity insights data.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EntityInsightsData {

    private Long id;
    private long orgId;
    private long entityId;
    private Source entityType;

    private ActivityInsights activityInsights;
    private EmailInsights emailInsights;
    private DealInsights dealInsights;

    private long lastUpdated;

    /**
     * <p>
     * Gets the id.
     * </p>
     *
     * @return the get id value
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Sets the id.
     * </p>
     *
     * @param id the id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Gets the org id.
     * </p>
     *
     * @return the get org id value
     */
    public long getOrgId() {
        return orgId;
    }

    /**
     * <p>
     * Sets the org id.
     * </p>
     *
     * @param orgId the org id
     */
    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    /**
     * <p>
     * Gets the entity id.
     * </p>
     *
     * @return the get entity id value
     */
    public long getEntityId() {
        return entityId;
    }

    /**
     * <p>
     * Sets the entity id.
     * </p>
     *
     * @param entityId the entity id
     */
    public void setEntityId(final long entityId) {
        this.entityId = entityId;
    }

    /**
     * <p>
     * Gets the entity type.
     * </p>
     *
     * @return the get entity type value
     */
    public Source getEntityType() {
        return entityType;
    }

    /**
     * <p>
     * Sets the entity type.
     * </p>
     *
     * @param entityType the entity type
     */
    public void setEntityType(final Source entityType) {
        this.entityType = entityType;
    }

    /**
     * <p>
     * Gets the activity insights.
     * </p>
     *
     * @return the get activity insights value
     */
    public ActivityInsights getActivityInsights() {
        return activityInsights;
    }

    /**
     * <p>
     * Sets the activity insights.
     * </p>
     *
     * @param activityInsights the activity insights
     */
    public void setActivityInsights(final ActivityInsights activityInsights) {
        this.activityInsights = activityInsights;
    }

    /**
     * <p>
     * Gets the email insights.
     * </p>
     *
     * @return the get email insights value
     */
    public EmailInsights getEmailInsights() {
        return emailInsights;
    }

    /**
     * <p>
     * Sets the email insights.
     * </p>
     *
     * @param emailInsights the email insights
     */
    public void setEmailInsights(final EmailInsights emailInsights) {
        this.emailInsights = emailInsights;
    }

    /**
     * <p>
     * Gets the deal insights.
     * </p>
     *
     * @return the get deal insights value
     */
    public DealInsights getDealInsights() {
        return dealInsights;
    }

    /**
     * <p>
     * Sets the deal insights.
     * </p>
     *
     * @param dealInsights the deal insights
     */
    public void setDealInsights(final DealInsights dealInsights) {
        this.dealInsights = dealInsights;
    }


    /**
     * <p>
     * Gets the last updated.
     * </p>
     *
     * @return the get last updated value
     */
    public long getLastUpdated() {
        return lastUpdated;
    }

    /**
     * <p>
     * Sets the last updated.
     * </p>
     *
     * @param lastUpdated the last updated
     */
    public void setLastUpdated(final long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}