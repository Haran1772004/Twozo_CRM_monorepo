package com.tlc.crm.core.api.iam.manager;

import com.tlc.crm.core.sql.resource.OMNOUSERNOTIFICATIONSETTINGS;
import com.tlc.crm.notification.app.model.ActivityReminderNotificationSettings;
import com.tlc.crm.notification.app.model.AssignmentNotificationSettings;
import com.tlc.crm.notification.app.model.ChatNotificationSettings;
import com.tlc.crm.notification.app.model.EmailNotificationSettings;
import com.tlc.crm.notification.app.model.EmailReminderNotificationSettings;
import com.tlc.crm.notification.app.model.InAppNotificationSoundSettings;
import com.tlc.crm.notification.app.model.MentionAlertNotificationSettings;
import com.tlc.crm.notification.app.model.SummaryNotificationSettings;
import com.tlc.crm.notification.app.model.TrackingNotificationSettings;
import com.tlc.crm.notification.app.model.UserNotificationSettings;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages and manipulates the database related actions for user notification settings
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class UserNotificationSettingsManager {

    private static class Instance {
        private static final UserNotificationSettingsManager INSTANCE = new UserNotificationSettingsManager();
    }

    private UserNotificationSettingsManager() {
    }

    /**
     * <p>
     * Gets the {@link UserNotificationSettingsManager} Instance
     * </p>
     *
     * @return the {@link UserNotificationSettingsManager}
     */
    public static UserNotificationSettingsManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the notification settings table
     * </p>
     *
     * @return the notification settings table
     */
    private Table getUserNotificationSettingsTable() {
        return Table.get(OMNOUSERNOTIFICATIONSETTINGS.TABLE);
    }

    /**
     * <p>
     * Updates the notification settings
     * </p>
     *
     * @param userNotificationSettings Represents the {@link UserNotificationSettings}
     */
    public void updateUserNotificationSettings(final UserNotificationSettings userNotificationSettings) {
        final Long orgId = userNotificationSettings.orgId();
        final Long userId = userNotificationSettings.getUserId();
        final Table userNotificationSettingsTable = getUserNotificationSettingsTable();
        final EmailNotificationSettings emailNotificationSettings = userNotificationSettings.getEmailNotificationSettings();
        final SummaryNotificationSettings summaryNotificationSettings = userNotificationSettings.getSummaryNotificationSettings();
        final ActivityReminderNotificationSettings activityReminderNotificationSettings = userNotificationSettings.getActivityReminderNotificationSettings();
        final EmailReminderNotificationSettings emailReminderNotificationSettings = userNotificationSettings.getEmailReminderNotificationSettings();
        final AssignmentNotificationSettings assignmentNotificationSettings = userNotificationSettings.getAssignmentNotificationSettings();
        final TrackingNotificationSettings trackingNotificationSettings = userNotificationSettings.getTrackingNotificationSettings();
        final ChatNotificationSettings chatNotificationSettings = userNotificationSettings.getChatNotificationSettings();
        final MentionAlertNotificationSettings mentionAlertNotificationSettings = userNotificationSettings.getMentionsAlertNotificationSettings();
        final InAppNotificationSoundSettings inAppNotificationSoundSettings = userNotificationSettings.getInAppNotificationSoundSettings();
        final DataContainer sourceContainer = fetchNotificationSettingsDataContainer(orgId, userId);
        final DataContainer targetContainer = DataContainer.create();
        final Row userNotificationSettingsRow = sourceContainer.getRow(userNotificationSettingsTable);
        final Row row = Objects.isNull(userNotificationSettingsRow) ? new Row(userNotificationSettingsTable) : userNotificationSettingsRow;

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMNOUSERNOTIFICATIONSETTINGS.USER_ID, userId);
        }
        row.set(OMNOUSERNOTIFICATIONSETTINGS.EMAIL_NOTIFICATION_SETTINGS, emailNotificationSettings.getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.SUMMARY_NOTIFICATION_SETTINGS, summaryNotificationSettings.getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.ASSIGNMENT_NOTIFICATION_SETTINGS, assignmentNotificationSettings.getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.TRACKING_NOTIFICATION_SETTINGS, trackingNotificationSettings.getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.EMAIL_REMINDER_NOTIFICATION_SETTINGS, emailReminderNotificationSettings.getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.ACTIVITY_REMINDER_NOTIFICATION_SETTINGS, activityReminderNotificationSettings.getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.CHAT_NOTIFICATION_SETTINGS, chatNotificationSettings.getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.MENTION_ALERT_NOTIFICATION_SETTINGS, mentionAlertNotificationSettings.getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.NOTIFICATION_SOUND_SETTINGS, inAppNotificationSoundSettings.getConfigAsJsonString());

        targetContainer.updateRow(row);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the default notification settings based on the settings given
     * </p>
     *
     * @param userNotificationSettings Represents the {@link UserNotificationSettings}
     */
    public void loadDefaultSettings(final UserNotificationSettings userNotificationSettings) {
        final long orgId = userNotificationSettings.orgId();
        final DataContainer targetContainer = DataContainer.create();

        loadDefaultSettingsIntoContainer(orgId, userNotificationSettings, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the default notification settings into the provided container based on the settings given
     * </p>
     *
     * @param orgId                    Represents an organization id
     * @param userNotificationSettings Represents the {@link UserNotificationSettings}
     * @param targetContainer          Represents the target container to load the default notification settings
     */
    public void loadDefaultSettingsIntoContainer(final long orgId, final UserNotificationSettings userNotificationSettings,
                                                 final DataContainer targetContainer) {
        final Table userNotificationSettingsTable = getUserNotificationSettingsTable();
        final Long userId = userNotificationSettings.getUserId();
        final Row userNotificationSettingsRow = fetchNotificationSettingsDataContainer(orgId, userId)
                .getRow(userNotificationSettingsTable);
        final Row row = Objects.isNull(userNotificationSettingsRow) ?
                new Row(userNotificationSettingsTable) : userNotificationSettingsRow;

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMNOUSERNOTIFICATIONSETTINGS.USER_ID, userId);
        }
        row.set(OMNOUSERNOTIFICATIONSETTINGS.EMAIL_NOTIFICATION_SETTINGS,
                userNotificationSettings.getEmailNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.ASSIGNMENT_NOTIFICATION_SETTINGS,
                userNotificationSettings.getAssignmentNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.SUMMARY_NOTIFICATION_SETTINGS,
                userNotificationSettings.getSummaryNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.TRACKING_NOTIFICATION_SETTINGS,
                userNotificationSettings.getTrackingNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.ACTIVITY_REMINDER_NOTIFICATION_SETTINGS,
                userNotificationSettings.getActivityReminderNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.EMAIL_REMINDER_NOTIFICATION_SETTINGS,
                userNotificationSettings.getEmailReminderNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.CHAT_NOTIFICATION_SETTINGS,
                userNotificationSettings.getChatNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.MENTION_ALERT_NOTIFICATION_SETTINGS,
                userNotificationSettings.getMentionsAlertNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.NOTIFICATION_SOUND_SETTINGS,
                userNotificationSettings.getInAppNotificationSoundSettings().getConfigAsJsonString());

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Fetches the notification settings based on organization and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the notification settings.
     */
    public UserNotificationSettings retrieveUserNotificationSettings(final long orgId, final long userId) {
        final Table userNotificationSettingsTable = getUserNotificationSettingsTable();
        final DataContainer sourceContainer = fetchNotificationSettingsDataContainer(orgId, userId);
        final Row userNotificationSettingsRow = sourceContainer.getRow(userNotificationSettingsTable);
        final UserNotificationSettings userNotificationSettings = new UserNotificationSettings();

        userNotificationSettings.setOrgId(orgId);
        userNotificationSettings.setUserId(userId);

        if (Objects.nonNull(userNotificationSettingsRow)) {
            userNotificationSettings.setId(userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.ID));
            userNotificationSettings.setEmailNotificationSettings(new EmailNotificationSettings(
                    (String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.EMAIL_NOTIFICATION_SETTINGS)));
            userNotificationSettings.setSummaryNotificationSettings(new SummaryNotificationSettings(
                    (String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.SUMMARY_NOTIFICATION_SETTINGS)));
            userNotificationSettings.setTrackingNotificationSettings(new TrackingNotificationSettings(
                    (String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.TRACKING_NOTIFICATION_SETTINGS)));
            userNotificationSettings.setAssignmentNotificationSettings(new AssignmentNotificationSettings(
                    (String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.ASSIGNMENT_NOTIFICATION_SETTINGS)));
            userNotificationSettings.setEmailReminderNotificationSettings(new EmailReminderNotificationSettings(
                    (String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.EMAIL_REMINDER_NOTIFICATION_SETTINGS)));
            userNotificationSettings.setActivityReminderNotificationSettings(new ActivityReminderNotificationSettings(
                    (String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.ACTIVITY_REMINDER_NOTIFICATION_SETTINGS)));
            userNotificationSettings.setChatNotificationSettings(new ChatNotificationSettings(
                    (String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.CHAT_NOTIFICATION_SETTINGS)));
            userNotificationSettings.setNoteMentionNotificationSettings(new MentionAlertNotificationSettings(
                    (String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.MENTION_ALERT_NOTIFICATION_SETTINGS)));
            userNotificationSettings.setInAppNotificationSoundSettings(new InAppNotificationSoundSettings(
                    (String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.NOTIFICATION_SOUND_SETTINGS)));
        }

        return userNotificationSettings;
    }

    /**
     * <p>
     * Resets the {@link UserNotificationSettings} to its default value.
     * </p>
     *
     * @param orgId  Represents an organisation id
     * @param userId Represents the user id
     * @param userNotificationSettings Represents the entity of {@link UserNotificationSettings}
     */
    public void resetUserNotificationSettings(final long orgId, final long userId, final UserNotificationSettings userNotificationSettings) {
        final Table userNotificationSettingsTable = getUserNotificationSettingsTable();
        final DataContainer sourceContainer = fetchNotificationSettingsDataContainer(orgId, userId);
        final DataContainer targetContainer = DataContainer.create();
        final Row userNotificationSettingsRow = sourceContainer.getRow(userNotificationSettingsTable);
        final Row row = Objects.isNull(userNotificationSettingsRow) ? new Row(userNotificationSettingsTable) : userNotificationSettingsRow;

        if (row.isNewRow()) {
            row.setOrgId(orgId);
            row.set(OMNOUSERNOTIFICATIONSETTINGS.USER_ID, userId);
        }
        row.set(OMNOUSERNOTIFICATIONSETTINGS.ACTIVITY_REMINDER_NOTIFICATION_SETTINGS,
                userNotificationSettings.getActivityReminderNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.EMAIL_REMINDER_NOTIFICATION_SETTINGS,
                userNotificationSettings.getEmailReminderNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.TRACKING_NOTIFICATION_SETTINGS,
                userNotificationSettings.getTrackingNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.ASSIGNMENT_NOTIFICATION_SETTINGS,
                userNotificationSettings.getAssignmentNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.SUMMARY_NOTIFICATION_SETTINGS,
                userNotificationSettings.getSummaryNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.EMAIL_NOTIFICATION_SETTINGS,
                userNotificationSettings.getEmailNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.CHAT_NOTIFICATION_SETTINGS,
                userNotificationSettings.getChatNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.MENTION_ALERT_NOTIFICATION_SETTINGS,
                userNotificationSettings.getMentionsAlertNotificationSettings().getConfigAsJsonString());
        row.set(OMNOUSERNOTIFICATIONSETTINGS.NOTIFICATION_SOUND_SETTINGS,
                userNotificationSettings.getInAppNotificationSoundSettings().getConfigAsJsonString());

        targetContainer.updateRow(row);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Fetches the assignment notification settings based on organization and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the notification settings.
     */
    public Optional<AssignmentNotificationSettings> retrieveAssignmentNotificationSettings(final long orgId, final long userId) {
        final Table userNotificationSettingsTable = getUserNotificationSettingsTable();
        final SelectQuery selectQuery = SelectQuery.get(userNotificationSettingsTable);

        selectQuery.addSelectClause(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.ASSIGNMENT_NOTIFICATION_SETTINGS));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.USER_ID), userId)));
        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);
        final Row userNotificationSettingsRow = sourceContainer.getRow(userNotificationSettingsTable);

        if (Objects.isNull(userNotificationSettingsRow)) {
            return Optional.empty();
        }
        return Optional.of(new AssignmentNotificationSettings((String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.ASSIGNMENT_NOTIFICATION_SETTINGS)));
    }

    /**
     * <p>
     * Fetches the email tracking notification settings based on organization and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the notification settings.
     */
    public Optional<TrackingNotificationSettings> retrieveTrackingNotificationSettings(final long orgId, final long userId) {
        final Table userNotificationSettingsTable = getUserNotificationSettingsTable();
        final SelectQuery selectQuery = SelectQuery.get(userNotificationSettingsTable);

        selectQuery.addSelectClause(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.TRACKING_NOTIFICATION_SETTINGS));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.USER_ID), userId)));
        final DataContainer sourceContainer = DataStoreProvider.getOrgDataStore(orgId).get(selectQuery);
        final Row userNotificationSettingsRow = sourceContainer.getRow(userNotificationSettingsTable);

        if (Objects.isNull(userNotificationSettingsRow)) {
            return Optional.empty();
        }
        return Optional.of(new TrackingNotificationSettings((String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.TRACKING_NOTIFICATION_SETTINGS)));
    }

    /**
     * Retrieves the {@link DataContainer} which contains the {@link UserNotificationSettings} related data.
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id.
     * @return dataContainer containing the notification settings data.
     */
    private DataContainer fetchNotificationSettingsDataContainer(final long orgId, final long userId) {
        final Table userNotificationSettingsTable = getUserNotificationSettingsTable();
        final SelectQuery selectQuery = SelectQuery.get(userNotificationSettingsTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.USER_ID), userId)));

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Retrieves the optional of chat notification settings based on the organization and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the optional of chat notification settings
     */
    public Optional<ChatNotificationSettings> retrieveChatNotificationSettings(final long orgId, final long userId) {
        final Table userNotificationSettingsTable = getUserNotificationSettingsTable();
        final SelectQuery selectQuery = SelectQuery.get(userNotificationSettingsTable);

        selectQuery.addSelectClause(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.CHAT_NOTIFICATION_SETTINGS));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.USER_ID), userId)));
        final DataContainer sourceContainer = DataStoreProvider.getOrgDataStore(orgId).get(selectQuery);
        final Row userNotificationSettingsRow = sourceContainer.getRow(userNotificationSettingsTable);

        if (Objects.isNull(userNotificationSettingsRow)) {
            return Optional.empty();
        }
        return Optional.of(new ChatNotificationSettings((String) userNotificationSettingsRow.get(OMNOUSERNOTIFICATIONSETTINGS.CHAT_NOTIFICATION_SETTINGS)));
    }

    /**
     * <p>
     * Fetches the collection of mention notification settings based on the organization and users
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userIds Represents the collection of user ids
     * @return the collection of {@link MentionAlertNotificationSettings} settings
     */
    public Collection<MentionAlertNotificationSettings> getMentionNotificationSettings(final Long orgId, final Collection<Long> userIds) {
        final Table userNotificationSettingsTable = getUserNotificationSettingsTable();
        final SelectQuery selectQuery = SelectQuery.get(userNotificationSettingsTable);

        selectQuery.addSelectClause(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.MENTION_ALERT_NOTIFICATION_SETTINGS));
        selectQuery.addSelectClause(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.USER_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(userNotificationSettingsTable.getColumn(OMNOUSERNOTIFICATIONSETTINGS.USER_ID), userIds)));
        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);

        return sourceContainer.getRows(userNotificationSettingsTable).map(row -> {
                    final Object mentionAlertSettings = row.get(OMNOUSERNOTIFICATIONSETTINGS.MENTION_ALERT_NOTIFICATION_SETTINGS);

                    if (Objects.isNull(mentionAlertSettings)) {
                        return new MentionAlertNotificationSettings();
                    }
                    final MentionAlertNotificationSettings mentionAlertNotificationSettings = new MentionAlertNotificationSettings((String) mentionAlertSettings);

                    mentionAlertNotificationSettings.setUserId(row.get(OMNOUSERNOTIFICATIONSETTINGS.USER_ID));

                    return mentionAlertNotificationSettings;
                }
        ).toList();
    }
}