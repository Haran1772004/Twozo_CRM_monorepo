package com.tlc.crm.core.features.api.activity.model;

import com.tlc.l10n.temporal.model.Date;

import java.io.Serializable;
import java.util.Objects;

/**
 * Holds the activity related fields for summary.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ActivitySummary implements Serializable {

    private Date lastActivityDate;
    private Date nextActivityDate;
    private String lastActivityType;
    private long doneActivities;
    private long upcomingActivities;
    private long pendingActivities;
    private long totalActivities;

    public Date getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(final Date lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public Date getNextActivityDate() {
        return nextActivityDate;
    }

    public void setNextActivityDate(final Date nextActivityDate) {
        this.nextActivityDate = nextActivityDate;
    }

    public String getLastActivityType() {
        return lastActivityType;
    }

    public void setLastActivityType(final String lastActivityType) {
        this.lastActivityType = lastActivityType;
    }

    public long getDoneActivities() {
        return doneActivities;
    }

    public void setDoneActivities(final Long doneActivities) {
        this.doneActivities = Objects.requireNonNullElse(doneActivities, 0L);
    }

    public long getUpcomingActivities() {
        return upcomingActivities;
    }

    public void setUpcomingActivities(final Long upcomingActivities) {
        this.upcomingActivities = Objects.requireNonNullElse(upcomingActivities, 0L);
    }

    public long getTotalActivities() {
        return totalActivities > 0 ? totalActivities : upcomingActivities + doneActivities + pendingActivities;
    }

    public long getPendingActivities() {
        return pendingActivities;
    }

    public void setTotalActivities(final Long totalActivities) {
        this.totalActivities = Objects.requireNonNullElse(totalActivities, 0L);
    }

    public void setPendingActivities(final Long pendingActivities) {
        this.pendingActivities = Objects.requireNonNullElseGet(pendingActivities, () -> 0L);
    }
}
