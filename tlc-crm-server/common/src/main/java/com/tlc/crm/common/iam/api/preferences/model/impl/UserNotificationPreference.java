package com.tlc.crm.common.iam.api.preferences.model.impl;

import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.models.NotificationState;

import java.io.Serializable;

/**
 * <p>
 * A model which represents the {@link UserNotificationPreference} details
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class UserNotificationPreference implements UserPreference, Serializable {

    public static final UserPreferenceType TYPE = UserPreferenceType.USER_NOTIFICATION_PREFERENCE;

    private Long orgId;
    private Long userId;
    private final NotificationState notificationState;

    /**
     * {@inheritDoc}
     *
     * @return Organisation id.
     */
    @Override
    public Long orgId() {
        return orgId;
    }

    /**
     * {@inheritDoc}
     *
     * @return User id.
     */
    @Override
    public Long userId() {
        return userId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public UserNotificationPreference(final NotificationState notificationState) {
        this.notificationState = notificationState;
    }

    public NotificationState getNotificationState() {
        return notificationState;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link UserPreferenceType}
     */
    @Override
    public UserPreferenceType getType() {
        return TYPE;
    }
}
