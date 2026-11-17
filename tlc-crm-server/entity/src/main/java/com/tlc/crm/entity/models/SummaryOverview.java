package com.tlc.crm.entity.models;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A model that contains the summary overview details.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class SummaryOverview {

    private long totalActivities;
    private Collection<Map<String, Object>> activityTypes;
    private Collection<Map<String, Object>> activityUsers;
    private Collection<Long> userIds;

    public Collection<Map<String, Object>> getActivityTypes() {
        return activityTypes;
    }

    public void setActivityTypes(final List<Map<String, Object>> activityTypes) {
        this.activityTypes = activityTypes;
    }

    public Collection<Map<String, Object>> getActivityUsers() {
        return activityUsers;
    }

    public void setActivityUsers(final List<Map<String, Object>> activityUsers) {
        this.activityUsers = activityUsers;
    }

    public Collection<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }

    public long getTotalActivities() {
        return totalActivities;
    }

    public void setTotalActivities(final long totalActivities) {
        this.totalActivities = totalActivities;
    }
}