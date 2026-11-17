package com.tlc.crm.core.api.insights.models;

/**
 * <p>
 * Represents the activity insights data
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class ActivityInsights {

    private long totalActivities;
    private long doneActivities;
    private long upcomingActivities;
    private long pendingActivities;

    private Long lastActivityDate;
    private Long nextActivityDate;
    private Long lastActivityTypeId;

    /**
     * <p>
     * Gets the done activities.
     * </p>
     *
     * @return the get done activities value
     */
    public long getDoneActivities() {
        return doneActivities;
    }

    /**
     * <p>
     * Sets the done activities.
     * </p>
     *
     * @param doneActivities the done activities
     */
    public void setDoneActivities(final long doneActivities) {
        this.doneActivities = doneActivities;
    }

    /**
     * <p>
     * Gets the upcoming activities.
     * </p>
     *
     * @return the get upcoming activities value
     */
    public long getUpcomingActivities() {
        return upcomingActivities;
    }

    /**
     * <p>
     * Sets the upcoming activities.
     * </p>
     *
     * @param upcomingActivities the upcoming activities
     */
    public void setUpcomingActivities(final long upcomingActivities) {
        this.upcomingActivities = upcomingActivities;
    }

    /**
     * <p>
     * Gets the pending activities.
     * </p>
     *
     * @return the get pending activities value
     */
    public long getPendingActivities() {
        return pendingActivities;
    }

    /**
     * <p>
     * Sets the pending activities.
     * </p>
     *
     * @param pendingActivities the pending activities
     */
    public void setPendingActivities(final long pendingActivities) {
        this.pendingActivities = pendingActivities;
    }

    /**
     * <p>
     * Gets the total activities.
     * </p>
     *
     * @return the get total activities value
     */
    public long getTotalActivities() {
        return totalActivities;
    }

    /**
     * <p>
     * Sets the total activities.
     * </p>
     *
     * @param totalActivities the total activities
     */
    public void setTotalActivities(final long totalActivities) {
        this.totalActivities = totalActivities;
    }

    /**
     * <p>
     * Gets the last activity date.
     * </p>
     *
     * @return the get last activity date value
     */
    public Long getLastActivityDate() {
        return lastActivityDate;
    }

    /**
     * <p>
     * Sets the last activity date.
     * </p>
     *
     * @param lastActivityDate the last activity date
     */
    public void setLastActivityDate(final Long lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    /**
     * <p>
     * Gets the next activity date.
     * </p>
     *
     * @return the get next activity date value
     */
    public Long getNextActivityDate() {
        return nextActivityDate;
    }

    /**
     * <p>
     * Sets the next activity date.
     * </p>
     *
     * @param nextActivityDate the next activity date
     */
    public void setNextActivityDate(final Long nextActivityDate) {
        this.nextActivityDate = nextActivityDate;
    }

    /**
     * <p>
     * Gets the last activity type ID.
     * </p>
     *
     * @return the last activity type ID value
     */
    public Long getLastActivityTypeId() {
        return lastActivityTypeId;
    }

    /**
     * <p>
     * Sets the last activity type ID.
     * </p>
     *
     * @param lastActivityTypeId the last activity type ID
     */
    public void setLastActivityTypeId(final Long lastActivityTypeId) {
        this.lastActivityTypeId = lastActivityTypeId;
    }
}