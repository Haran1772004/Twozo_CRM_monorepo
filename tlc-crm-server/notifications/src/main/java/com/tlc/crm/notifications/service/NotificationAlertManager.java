package com.tlc.crm.notifications.service;

import com.tlc.crm.notifications.api.manager.NotificationAlertManagerImpl;
import com.tlc.crm.notifications.api.model.alert.ActivityAlert;
import com.tlc.crm.notifications.api.model.alert.ExportAlert;
import com.tlc.crm.notifications.api.model.alert.EntityAssignmentAlert;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.api.model.alert.SocialChatAlert;
import com.tlc.crm.notifications.api.model.alert.SourceMentionAlert;

import java.util.Collection;

/**
 * <p>
 * Manages DB related actions for Notification Alerts.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public interface NotificationAlertManager {

    /**
     * <p>
     * Gets {@link NotificationAlertManager}.
     * </p>
     *
     * @return {@link NotificationAlertManager}.
     */
    static NotificationAlertManager getInstance() {
        return NotificationAlertManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets {@link NotificationAlert} in a collection.
     * </p>
     *
     * @param orgId  An organization's id.
     * @param start  Start point.
     * @param limit  Limit to fetch.
     * @param time   Alert's timestamp.
     * @param userId A user's id.
     * @return Gets {@link NotificationAlert} in a collection.
     */
    Collection<NotificationAlert> get(final Long orgId, final int start, final int limit, final long time, final Long userId);

    /**
     * <p>
     * Gets total count of alerts.
     * </p>
     *
     * @param orgId  An organization's id.
     * @param time   Alert's timestamp.
     * @param userId A user's id.
     * @return total count of alerts.
     */
    Long getTotalCount(final Long orgId, final long time, final Long userId);

    /**
     * <p>
     * Deletes expired alerts based on alert ids.
     * </p>
     *
     * @param orgId    Organization's id.
     * @param alertIds {@link NotificationAlert} ids.
     */
    void deleteExpiredAlerts(final Long orgId, final Collection<Long> alertIds);

    /**
     * <p>
     * Stores {@link ActivityAlert}.
     * </p>
     *
     * @param activityReminderAlert {@link ActivityAlert}.
     */
    void storeActivityReminderAlert(final ActivityAlert activityReminderAlert);

    /**
     * <p>
     * Stores {@link ActivityAlert}.
     * </p>
     *
     * @param newActivityAlert {@link ActivityAlert}.
     */
    void storeNewActivityAlert(final ActivityAlert newActivityAlert);

    /**
     * <p>
     * Stores {@link ActivityAlert}.
     * </p>
     *
     * @param newActivityAlert {@link ActivityAlert}.
     */
    void storeActivityCollaboratorAlert(final ActivityAlert newActivityAlert, final Collection<Long> userIds);

    /**
     * <p>
     * Stores the  {@link SourceMentionAlert}
     * </p>
     *
     * @param sourceMentionAlert Represents the {@link SourceMentionAlert}
     */
    void storeSourceMentionAlert(final SourceMentionAlert sourceMentionAlert);

    /**
     * <p>
     * Stores the {@link ExportAlert}.
     * </p>
     *
     * @param exportAlert Represents the {@link ExportAlert}.
     */
    void storeExportAlert(final ExportAlert exportAlert);

    /**
     * <p>
     * Deletes activities reminder alerts.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param assigneeId  Represents the assignee id
     * @param activityIds Represents the collection of activity id
     */
    void deleteActivityReminderAlerts(final long orgId, final long assigneeId, final Collection<Long> activityIds);

    /**
     * <p>
     * Checks whether the activity has reminder alert.
     * </p>
     *
     * @param orgId      Represents an organization id.
     * @param activityId Represents the activity id.
     * @return whether the activity has reminder alert.
     */
    boolean hasActivityReminderAlert(final long orgId, final long activityId);

    /**
     * <p>
     * Stores {@link SocialChatAlert}.
     * </p>
     *
     * @param socialChatAlert {@link SocialChatAlert}.
     */
    void storeChatAlert(final SocialChatAlert socialChatAlert);

    /**
     * <p>
     * Stores {@link EntityAssignmentAlert}.
     * </p>
     *
     * @param entityAssignmentAlert {@link EntityAssignmentAlert}.
     */
    void storeEntityAssignmentAlert(final EntityAssignmentAlert entityAssignmentAlert);

    /**
     * <p>
     * Gets the collection of {@link NotificationAlert}.
     * </p>
     *
     * @param time Represents the current time.
     * @return the collection of {@link NotificationAlert}.
     */
    Collection<NotificationAlert> getAlerts(final long time, final int start, final int limit);

    /**
     * <p>
     * Gets the activity reminder alerts
     * </p>
     *
     * @param orgId  Represents the organisation Id
     * @param userId Represents the user Id
     * @return The collection of {@link NotificationAlert}
     */
    Collection<NotificationAlert> getActivityReminderAlerts(final Long orgId, final Long userId, final int start, final int limit);

    /**
     * <p>
     * Whether the alert exists.
     * </p>
     *
     * @param orgId Represents an organization id.
     * @param id    Represents the alert id.
     * @return whether the alert exists.
     */
    boolean exists(final long orgId, final long id);

    /**
     * <p>
     * Marks the alert as read.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents the user id.
     * @param id     Represents the alert id.
     */
    void markAsRead(final long orgId, final long userId, final long id);

    /**
     * <p>
     * Marks the alert as delivered.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents the user id.
     * @param ids    Represents the collection of alert id.
     */
    void markAsDelivered(final long orgId, final long userId, final Collection<Long> ids);
}