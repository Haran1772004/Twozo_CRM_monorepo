package com.tlc.crm.notifications.service;

import com.tlc.crm.common.models.NotificationState;
import com.tlc.crm.notifications.api.manager.AppNotificationManagerImpl;
import com.tlc.crm.notifications.api.model.AppNotification;

import java.util.Collection;

/**
 * <p>
 * Performs the functionality for the {@link AppNotificationManager} DB related actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface AppNotificationManager {

    /**
     * <p>
     * Gets the instance of {@link AppNotificationManager}
     * </p>
     *
     * @return {@link AppNotificationManager}
     */
    static AppNotificationManager getInstance() {
        return AppNotificationManagerImpl.getInstance();
    }

    /**
     * <p>
     * Marks all the notification of the user as read based on organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     */
    void markAllAsRead(final long orgId, final long userId);

    /**
     * <p>
     * Checks whether any notification is present for the given user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return true, when user has any notification
     */
    boolean exists(final long orgId, final long userId);

    /**
     * <p>
     * Checks whether the given notification is exists or not
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param id     Represents the notification id
     * @return true, if notification is present
     */
    boolean exists(final long orgId, final long userId, final long id);

    /**
     * <p>
     * Marks the specific notification as read
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param id     Represents the notification id
     */
    void markAsRead(final long orgId, final long userId, final long id);

    /**
     * <p>
     * Updates the notification state
     * </p>
     *
     * @param orgId             Represents an organization id
     * @param userId            Represents the user id
     * @param notificationState Represents the notification state
     */
    void updateNotificationState(final long orgId, final long userId, final NotificationState notificationState);

    /**
     * <p>
     * Fetches the notification state of the user in an organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the notification state (on/off)
     */
    NotificationState getNotificationState(final long orgId, final long userId);

    /**
     * <p>
     * Gets the pending notification count of the user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the pending notification count
     */
    int getPendingNotificationCount(final long orgId, final long userId);

    /**
     * <p>
     * Fetches the total count of notification for the user in an organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the total count
     */
    long getTotalCount(final long orgId, final long userId);

    /**
     * <p>
     * Gets the notifications for the user in the given organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param start  Represents the start value
     * @param limit  Represents the limit value
     * @return the collection of {@link AppNotification}
     */
    Collection<AppNotification> get(final long orgId, final long userId, final int start, final int limit);

    /**
     * <p>
     * Gets the icon id for the given activity id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param activityId Represents the activity id
     * @return the icon id
     */
    Long getIconId(final long orgId, final long activityId);

    /**
     * <p>
     * Gets the top notifications for the user in the given organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param start  Represents the start value
     * @param limit  Represents the limit value
     * @return the collection of {@link AppNotification}
     */
    Collection<AppNotification> getTopNotifications(final long orgId, final long userId, final int start, final int limit);

    /**
     * <p>
     * Stores the notification based on the given details
     * </p>
     *
     * @param orgId            An organization's id.
     * @param appNotifications Represents the {@link AppNotification} in a collection.
     */
    void storeNotification(final Long orgId, final Collection<AppNotification> appNotifications);

    /**
     * <p>
     * Resets the pending notification count to zero for the given user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     */
    void resetPendingNotificationCount(final long orgId, final long userId);
}
