package com.tlc.iam.client.dropdown;

import java.util.Collection;

/**
 * <p>
 * Holds the {@link SearchFilter} details
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public final class SearchFilter {

    private Integer start;
    private Integer limit;
    private String type;
    private Object value;
    private Boolean excludeInvitedUsers;
    private Boolean includeDeactivatedUsers;
    private Collection<Long> userIds;
    private Collection<Long> excludeUserIds;
    private Boolean excludeUsersWithoutPhone;

    public Boolean isExcludeInvitedUsers() {
        return excludeInvitedUsers;
    }

    public void excludeInvitedUsers(final Boolean excludeInvitedUser) {
        this.excludeInvitedUsers = excludeInvitedUser;
    }

    public void setStart(final Integer start) {
        this.start = start;
    }

    public void setLimit(final Integer limit) {
        this.limit = limit;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    public Boolean isIncludeDeactivatedUsers() {
        return includeDeactivatedUsers;
    }

    public void includeDeactivatedUsers(final Boolean includeDeactivateUser) {
        this.includeDeactivatedUsers = includeDeactivateUser;
    }

    public Integer getStart() {
        return start;
    }

    public Integer getLimit() {
        return limit;
    }

    public String getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public Collection<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(final Collection<Long> userIds) {
        this.userIds = userIds;
    }

    public Collection<Long> getExcludeUserIds() {
        return excludeUserIds;
    }

    public void setExcludeUserIds(final Collection<Long> excludeUserIds) {
        this.excludeUserIds = excludeUserIds;
    }

    public Boolean excludeUsersWithoutPhone() {
        return excludeUsersWithoutPhone;
    }

    public void setExcludeUsersWithoutPhone(final Boolean excludeUsersWithoutPhone) {
        this.excludeUsersWithoutPhone = excludeUsersWithoutPhone;
    }
}
