package com.tlc.crm.notifications.api.manager;

import com.tlc.commons.json.Json;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.notifications.api.model.alert.ActivityAlert;
import com.tlc.crm.notifications.api.model.alert.ExportAlert;
import com.tlc.crm.notifications.api.model.alert.EntityAssignmentAlert;
import com.tlc.crm.notifications.api.model.alert.NotificationAlert;
import com.tlc.crm.notifications.api.model.alert.SocialChatAlert;
import com.tlc.crm.notifications.api.model.alert.SourceMentionAlert;
import com.tlc.crm.notifications.service.NotificationAlertManager;
import com.tlc.crm.notifications.sql.resource.OMANACTIVITYREMINDERALERT;
import com.tlc.crm.notifications.sql.resource.OMANNOTIFICATIONALERT;
import com.tlc.crm.notifications.sql.resource.OMANNOTIFICATIONALERTUSERS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.LimitClause;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Manages DB related actions for in alerts.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class NotificationAlertManagerImpl extends AbstractNotificationManager implements NotificationAlertManager {

    private NotificationAlertManagerImpl() {
    }

    private static class Instance {
        private static final NotificationAlertManagerImpl INSTANCE = new NotificationAlertManagerImpl();
    }

    /**
     * <p>
     * Gets {@link NotificationAlertManagerImpl}.
     * </p>
     *
     * @return {@link NotificationAlertManagerImpl}.
     */
    public static NotificationAlertManagerImpl getInstance() {
        return Instance.INSTANCE;
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
    @Override
    public Collection<NotificationAlert> get(final Long orgId, final int start, final int limit, final long time, final Long userId) {
        final Table alertTable = getAlertTable();
        final Table alertUsersTable = getAlertUsersTable();
        final Column alertPkColumn = alertTable.getPKColumn();
        final SelectQuery alertQuery = SelectQuery.get(alertTable);

        alertQuery.addJoinClause(new JoinClause(alertPkColumn, alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.ALERT_ID)));
        alertQuery.setWhereClause(getWhereClause(time, userId, alertTable, alertUsersTable));
        alertQuery.addSelectClause(alertTable);
        final Collection<Row> alertTableRows = orgDataStore(orgId).get(alertQuery).getRows(alertTable).toList();

        updateAlertUsersTableColumnValueAsTrue(orgId, alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.IS_DELIVERED),
                getAlertUsersPkValue(orgId, userId, alertTableRows.stream().map(Row::getPKValue).toList()));

        return convertRowsToAlerts(alertTableRows);
    }

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
    @Override
    public Long getTotalCount(final Long orgId, final long time, final Long userId) {
        final Table alertTable = getAlertTable();
        final Table alertUsersTable = getAlertUsersTable();
        final CountQuery alertQuery = CountQuery.get(alertTable);

        alertQuery.addJoinClause(new JoinClause(alertTable.getPKColumn(),
                alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.ALERT_ID)));
        alertQuery.setWhereClause(getWhereClause(time, userId, alertTable, alertUsersTable));

        return orgDataStore(orgId).get(alertQuery);
    }

    /**
     * <p>
     * Deletes expired alerts based on alert ids.
     * </p>
     *
     * @param orgId    Organization's id.
     * @param alertIds {@link NotificationAlert} ids.
     */
    @Override
    public void deleteExpiredAlerts(final Long orgId, final Collection<Long> alertIds) {
        final Table alertTable = getAlertTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(alertTable);

        deleteQuery.setWhereClause(new WhereClause(Criteria.in(alertTable.getPKColumn(), alertIds)));
        orgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * <p>
     * Stores {@link ActivityAlert}.
     * </p>
     *
     * @param activityReminderAlert {@link ActivityAlert}.
     */
    @Override
    public void storeActivityReminderAlert(final ActivityAlert activityReminderAlert) {
        final Long orgId = activityReminderAlert.orgId();
        final Long activityId = activityReminderAlert.getActivityId();
        final Table alertTable = getAlertTable();
        final Table alertUsersTable = getAlertUsersTable();
        final Column alertPkColumn = alertTable.getPKColumn();
        final Table activityReminderAlertTable = getActivityReminderAlertTable();
        final SelectQuery activityReminderQuery = SelectQuery.get(alertTable);

        activityReminderQuery.addJoinClause(List.of(new JoinClause(alertPkColumn,
                alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.ALERT_ID)), new JoinClause(alertPkColumn,
                activityReminderAlertTable.getColumn(OMANACTIVITYREMINDERALERT.ALERT_ID))));
        activityReminderQuery.addSelectClause(alertTable, alertUsersTable, activityReminderAlertTable);
        activityReminderQuery.setWhereClause(new WhereClause(Criteria.eq(activityReminderAlertTable.getColumn(OMANACTIVITYREMINDERALERT.ACTIVITY_ID), activityId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(activityReminderQuery);
        final Row alertRow;
        final Row alertUsersRow;
        final Row activityReminderAlertRow;

        if (dataContainer.isEmpty()) {
            alertRow = new Row(alertTable);
            activityReminderAlertRow = new Row(activityReminderAlertTable);
            alertUsersRow = new Row(alertUsersTable);

            activityReminderAlertRow.set(OMANACTIVITYREMINDERALERT.ACTIVITY_ID, activityId);
        } else {
            alertRow = dataContainer.getRow(alertTable);
            activityReminderAlertRow = dataContainer.getRow(activityReminderAlertTable);
            alertUsersRow = dataContainer.getRow(alertUsersTable);
        }
        final DataContainer targetContainer = DataContainer.create();
        final Long pkValue = alertRow.getPKValue();

        setAlertRow(alertRow, activityReminderAlert);
        activityReminderAlertRow.set(OMANACTIVITYREMINDERALERT.ALERT_ID, pkValue);
        setAlertUsersRow(alertUsersRow, orgId, pkValue, activityReminderAlert.getAssigneeId());
        targetContainer.updateRow(alertRow);
        targetContainer.updateRow(activityReminderAlertRow);
        targetContainer.updateRow(alertUsersRow);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Stores {@link ActivityAlert}.
     * </p>
     *
     * @param newActivityAlert {@link ActivityAlert}.
     */
    @Override
    public void storeNewActivityAlert(final ActivityAlert newActivityAlert) {
        setSingleUserAlertRow(newActivityAlert, newActivityAlert.getAssigneeId());
    }

    /**
     * <p>
     * Stores {@link SocialChatAlert}.
     * </p>
     *
     * @param entityAssignmentAlert {@link EntityAssignmentAlert}.
     */
    @Override
    public void storeEntityAssignmentAlert(final EntityAssignmentAlert entityAssignmentAlert) {
        setSingleUserAlertRow(entityAssignmentAlert, entityAssignmentAlert.getAssigneeId());
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceMentionAlert Represents the {@link SourceMentionAlert}
     */
    @Override
    public void storeSourceMentionAlert(final SourceMentionAlert sourceMentionAlert) {
        setSingleUserAlertRow(sourceMentionAlert, sourceMentionAlert.getAssigneeId());
    }

    /**
     * {@inheritDoc}
     *
     * @param exportAlert Represents the {@link ExportAlert}.
     */
    @Override
    public void storeExportAlert(final ExportAlert exportAlert) {
        setSingleUserAlertRow(exportAlert, exportAlert.getAssigneeId());
    }

    /**
     * <p>
     * Stores {@link ActivityAlert}.
     * </p>
     *
     * @param newActivityAlert {@link ActivityAlert}.
     */
    @Override
    public void storeActivityCollaboratorAlert(final ActivityAlert newActivityAlert, final Collection<Long> userIds) {
        setMultiUsersAlertRows(newActivityAlert, userIds);
    }

    /**
     * <p>
     * Stores {@link SocialChatAlert}.
     * </p>
     *
     * @param socialChatAlert {@link SocialChatAlert}.
     */
    @Override
    public void storeChatAlert(final SocialChatAlert socialChatAlert) {
        setMultiUsersAlertRows(socialChatAlert, socialChatAlert.getAssigneeIds());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param assigneeId  Represents the assignee id
     * @param activityIds Represents the collection of activity id
     */
    @Override
    public void deleteActivityReminderAlerts(final long orgId, final long assigneeId, final Collection<Long> activityIds) {
        final Table activityReminderAlertTable = getActivityReminderAlertTable();
        final SelectQuery activityReminderAlertQuery = SelectQuery.get(activityReminderAlertTable);

        activityReminderAlertQuery.addSelectClause(activityReminderAlertTable.getColumn(OMANACTIVITYREMINDERALERT.ALERT_ID));
        activityReminderAlertQuery.setWhereClause(new WhereClause(Criteria.in(
                activityReminderAlertTable.getColumn(OMANACTIVITYREMINDERALERT.ACTIVITY_ID), activityIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(activityReminderAlertQuery);

        if (!dataContainer.isEmpty()) {
            final Table alertTable = getAlertTable();
            final Stream<Row> activityReminderAlertRows = dataContainer.getRows(activityReminderAlertTable);
            final Collection<Long> alertIds = activityReminderAlertRows.map(activityReminderAlertRow -> (Long) activityReminderAlertRow.get(OMANACTIVITYREMINDERALERT.ALERT_ID)).toList();
            final DeleteQuery deleteQuery = DeleteQuery.get(alertTable);

            deleteQuery.setWhereClause(new WhereClause(Criteria.in(alertTable.getPKColumn(), alertIds)));
            orgDataStore(orgId).delete(deleteQuery);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param activityId Represents the activity id
     * @return true, if present
     */
    @Override
    public boolean hasActivityReminderAlert(final long orgId, final long activityId) {
        final Table activityReminderAlertTable = getActivityReminderAlertTable();
        final CountQuery countQuery = CountQuery.get(activityReminderAlertTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(
                activityReminderAlertTable.getColumn(OMANACTIVITYREMINDERALERT.ACTIVITY_ID), activityId)));

        return NumericalConstant.ZERO < orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the {@link SocialChatAlert}.
     * </p>
     *
     * @param time Represents the reminder time.
     * @return the collection of {@link SocialChatAlert}.
     */
    @Override
    public Collection<NotificationAlert> getAlerts(final long time, final int start, final int limit) {
        final Table alertTable = getAlertTable();
        final Table alertUsersTable = getAlertUsersTable();
        final SelectQuery alertQuery = SelectQuery.get(alertTable);

        alertQuery.addJoinClause(List.of(new JoinClause(
                alertTable.getPKColumn(), alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.ALERT_ID))));
        alertQuery.addSelectClause(alertTable, alertUsersTable);
        alertQuery.setWhereClause(new WhereClause(Criteria.le(alertTable.getColumn(OMANNOTIFICATIONALERT.TIMESTAMP), time)));
        alertQuery.setLimitClause(new LimitClause(start, limit));
        final DataContainer dataContainer = readableDataStore().get(alertQuery);

        return convertRowsToAlerts(dataContainer, dataContainer.getRows(alertTable).toList());
    }

    /**
     * {@inheritDoc}.
     *
     * @param orgId  Represents the organization id.
     * @param userId Represents the user id.
     * @param start  Represents the start index.
     * @param limit  Represents the limit.
     * @return The collection of {@link NotificationAlert}
     */
    @Override
    public Collection<NotificationAlert> getActivityReminderAlerts(final Long orgId, final Long userId, final int start, final int limit) {
        final Table alertTable = getAlertTable();
        final Table alertUsersTable = getAlertUsersTable();
        final Table reminderAlertTable = getActivityReminderAlertTable();
        final SelectQuery reminderAlertQuery = SelectQuery.get(alertTable);
        final Column alertPkColumn = alertTable.getPKColumn();
        final Instant instant = Instant.now();

        reminderAlertQuery.addJoinClause(new JoinClause(alertPkColumn, reminderAlertTable.getColumn(OMANACTIVITYREMINDERALERT.ALERT_ID)));
        reminderAlertQuery.addJoinClause(new JoinClause(alertPkColumn, alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.ALERT_ID)));
        reminderAlertQuery.addSelectClause(alertTable, reminderAlertTable, alertUsersTable);
        reminderAlertQuery.setWhereClause(new WhereClause(Criteria.ge(alertTable.getColumn(OMANNOTIFICATIONALERT.TIMESTAMP), instant.toEpochMilli()))
                .and(Criteria.le(alertTable.getColumn(OMANNOTIFICATIONALERT.TIMESTAMP), instant.plus(NumericalConstant.ONE, ChronoUnit.MINUTES).toEpochMilli()))
                .and(Criteria.eq(alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.IS_READ), false))
                .and(Criteria.eq(alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.IS_DELIVERED), false))
                .and(Criteria.eq(alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.USER_ID), userId)));
        reminderAlertQuery.setLimitClause(new LimitClause(start, limit));
        final DataContainer dataContainer = orgDataStore(orgId).get(reminderAlertQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }
        return convertRowsToAlerts(dataContainer, dataContainer.getRows(alertTable).toList());
    }

    /***
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id.
     * @param id     Represents the alert id.
     * @return whether the alert exists.
     */
    @Override
    public boolean exists(final long orgId, final long id) {
        final Table alertTable = getAlertTable();
        final CountQuery countQuery = CountQuery.get(alertTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(alertTable.getPKColumn(), id)));

        return NumericalConstant.ZERO < orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents the user id.
     * @param id     Represents the alert id.
     */
    @Override
    public void markAsRead(final long orgId, final long userId, final long id) {
        updateAlertUsersTableColumnValueAsTrue(orgId, getAlertUsersTable().getColumn(OMANNOTIFICATIONALERTUSERS.IS_READ),
                getAlertUsersPkValue(orgId, userId, List.of(id)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents the user id.
     * @param ids    Represents the collection of alert id.
     */
    @Override
    public void markAsDelivered(final long orgId, final long userId, final Collection<Long> ids) {
        updateAlertUsersTableColumnValueAsTrue(orgId, getAlertUsersTable().getColumn(OMANNOTIFICATIONALERTUSERS.IS_DELIVERED),
                getAlertUsersPkValue(orgId, userId, ids));
    }

    /**
     * <p>
     * Gets alert users table's pk values.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents the user id.
     * @param ids    Represents the collection of alert id.
     * @return alert users table's pk values.
     */
    private Collection<Long> getAlertUsersPkValue(final long orgId, final long userId, final Collection<Long> ids) {

        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        final Table alertTable = getAlertTable();
        final Table alertUsersTable = getAlertUsersTable();
        final SelectQuery alertUsersPkQuery = SelectQuery.get(alertTable);
        final Column alertPkColumn = alertTable.getPKColumn();

        alertUsersPkQuery.addJoinClause(List.of(new JoinClause(
                alertPkColumn, alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.ALERT_ID))));
        alertUsersPkQuery.addSelectClause(alertUsersTable.getPKColumn());
        alertUsersPkQuery.setWhereClause(new WhereClause(Criteria.in(alertPkColumn, ids)).and(
                Criteria.eq(alertUsersTable.getColumn(OMANNOTIFICATIONALERTUSERS.USER_ID), userId)));

        return orgDataStore(orgId).get(alertUsersPkQuery).getRows(alertUsersTable).map(Row::getPKValue).toList();
    }

    /**
     * <p>
     * Updates the passed column as true in alert users table.
     * </p>
     *
     * @param orgId             Represents an organization id.
     * @param updateColumn      {@link Column}.
     * @param alertUsersPkValue Represents the collection of alert user table ids.
     */
    private void updateAlertUsersTableColumnValueAsTrue(final long orgId, final Column updateColumn,
                                                        final Collection<Long> alertUsersPkValue) {

        if (!alertUsersPkValue.isEmpty()) {
            final Table alertUsersTable = getAlertUsersTable();
            final UpdateQuery updateQuery = UpdateQuery.get(alertUsersTable);

            updateQuery.addUpdateColumn(updateColumn, true);
            updateQuery.setWhereClause(new WhereClause(Criteria.in(alertUsersTable.getPKColumn(), alertUsersPkValue)));
            orgDataStore(orgId).update(updateQuery);
        }
    }

    /**
     * <p>
     * Gets {@link WhereClause} based on current user.
     * </p>
     *
     * @param time           Current time.
     * @param userId         A user's id.
     * @param alertTable     {@link Table}.
     * @param userAlertTable {@link Table}.
     * @return {@link WhereClause}.
     */
    private WhereClause getWhereClause(final long time, final Long userId, final Table alertTable,
                                       final Table userAlertTable) {

        return new WhereClause(Criteria.le(alertTable.getColumn(OMANNOTIFICATIONALERT.TIMESTAMP), time)).and(
                new WhereClause(Criteria.eq(userAlertTable.getColumn(OMANNOTIFICATIONALERTUSERS.USER_ID), userId)).and(
                        Criteria.eq(userAlertTable.getColumn(OMANNOTIFICATIONALERTUSERS.IS_DELIVERED), false)));
    }

    /**
     * <p>
     * Converts {@link Row} into collection {@link NotificationAlert}.
     * </p>
     *
     * @param alertRows Collection {@link Row}.
     * @return {@link ActivityAlert}.
     */
    private Collection<NotificationAlert> convertRowsToAlerts(final Collection<Row> alertRows) {
        final Collection<NotificationAlert> notificationAlerts = new ArrayList<>(alertRows.size());

        for (final Row alertRow : alertRows) {
            final NotificationAlert.Category category = NotificationAlert.Category.get(alertRow.get(OMANNOTIFICATIONALERT.CATEGORY));

            final NotificationAlert notificationAlert = switch (category) {
                case ACTIVITY_REMINDER, NEW_ACTIVITY, COLLABORATOR_ADDED -> new ActivityAlert();
                case CONTACT_ASSIGNED, COMPANY_ASSIGNED, DEAL_ASSIGNED, PRODUCT_ASSIGNED -> new EntityAssignmentAlert();
                case CHAT_ASSIGNED, CHAT_INITIATED -> new SocialChatAlert();
                case CONTACT_NOTE_MENTIONED, COMPANY_NOTE_MENTIONED, DEAL_NOTE_MENTIONED,
                     ACTIVITY_PRIVATE_NOTE_MENTIONED, CONTACT_NOTE_COMMENT_MENTIONED, COMPANY_NOTE_COMMENT_MENTIONED,
                     DEAL_NOTE_COMMENT_MENTIONED -> new SourceMentionAlert();
                case EXPORTED -> new ExportAlert();
            };

            setNotificationAlert(alertRow, category, notificationAlert);
            notificationAlerts.add(notificationAlert);
        }

        return notificationAlerts;
    }

    /**
     * <p>
     * Converts {@link Row} into collection {@link NotificationAlert}.
     * </p>
     *
     * @param dataContainer {@link DataContainer}.
     * @param alertRows     Collection {@link Row}.
     * @return {@link ActivityAlert}.
     */
    private Collection<NotificationAlert> convertRowsToAlerts(final DataContainer dataContainer,
                                                              final Collection<Row> alertRows) {
        final Collection<NotificationAlert> notificationAlerts = new ArrayList<>(alertRows.size());
        final Table alertUserstable = getAlertUsersTable();
        final Column alertColumn = alertUserstable.getColumn(OMANNOTIFICATIONALERTUSERS.ALERT_ID);
        final Column userColumn = alertUserstable.getColumn(OMANNOTIFICATIONALERTUSERS.USER_ID);
        final Column isReadColumn = alertUserstable.getColumn(OMANNOTIFICATIONALERTUSERS.IS_READ);

        for (final Row alertRow : alertRows) {
            final NotificationAlert.Category category = NotificationAlert.Category.get(alertRow.get(OMANNOTIFICATIONALERT.CATEGORY));

            switch (category) {
                case ACTIVITY_REMINDER, NEW_ACTIVITY -> {
                    final ActivityAlert activityAlert = new ActivityAlert();
                    final Row alertUserRow = dataContainer.getRow(alertUserstable, new WhereClause(Criteria.eq(
                            alertColumn, alertRow.getPKValue())));

                    setNotificationAlert(alertRow, category, activityAlert);
                    activityAlert.setAssigneeId(alertUserRow.get(userColumn));
                    activityAlert.setRead(alertUserRow.get(isReadColumn));
                    notificationAlerts.add(activityAlert);
                }
                case COLLABORATOR_ADDED -> {
                    final ActivityAlert activityAlert = new ActivityAlert();

                    setNotificationAlert(alertRow, category, activityAlert);

                    dataContainer.getRows(alertUserstable, new WhereClause(Criteria.eq(
                            alertColumn, alertRow.getPKValue()))).forEach(alertUserRow -> {
                        activityAlert.setAssigneeId(alertUserRow.get(userColumn));
                        activityAlert.setRead(alertUserRow.get(isReadColumn));
                        notificationAlerts.add(activityAlert);
                    });
                }
                case CONTACT_ASSIGNED, COMPANY_ASSIGNED, DEAL_ASSIGNED, PRODUCT_ASSIGNED -> {
                    final EntityAssignmentAlert entityAssignmentAlert = new EntityAssignmentAlert();
                    final Row alertUserRow = dataContainer.getRow(alertUserstable, new WhereClause(Criteria.eq(
                            alertColumn, alertRow.getPKValue())));

                    setNotificationAlert(alertRow, category, entityAssignmentAlert);
                    entityAssignmentAlert.setAssigneeId(alertUserRow.get(userColumn));
                    entityAssignmentAlert.setRead(alertUserRow.get(isReadColumn));
                    notificationAlerts.add(entityAssignmentAlert);
                }
                case CHAT_ASSIGNED, CHAT_INITIATED -> {
                    final SocialChatAlert socialChatAlert = new SocialChatAlert();

                    setNotificationAlert(alertRow, category, socialChatAlert);
                    socialChatAlert.setUsersReadMap(dataContainer.getRows(alertUserstable, new WhereClause(Criteria.eq(
                            alertColumn, alertRow.getPKValue()))).collect(Collectors.toMap(
                            row -> row.get(userColumn), row -> row.get(isReadColumn))));
                    notificationAlerts.add(socialChatAlert);
                }
                case CONTACT_NOTE_MENTIONED, COMPANY_NOTE_MENTIONED, DEAL_NOTE_MENTIONED,
                     ACTIVITY_PRIVATE_NOTE_MENTIONED, CONTACT_NOTE_COMMENT_MENTIONED, COMPANY_NOTE_COMMENT_MENTIONED,
                     DEAL_NOTE_COMMENT_MENTIONED -> {
                    final SourceMentionAlert sourceMentionAlert = new SourceMentionAlert();
                    final Row alertUserRow = dataContainer.getRow(alertUserstable, new WhereClause(Criteria.eq(
                            alertColumn, alertRow.getPKValue())));

                    setNotificationAlert(alertRow, category, sourceMentionAlert);
                    sourceMentionAlert.setAssigneeId(alertUserRow.get(userColumn));
                    sourceMentionAlert.setRead(alertUserRow.get(isReadColumn));
                    notificationAlerts.add(sourceMentionAlert);
                }
                case EXPORTED -> {
                    final ExportAlert exportAlert = new ExportAlert();
                    final Row alertUserRow = dataContainer.getRow(alertUserstable, new WhereClause(Criteria.eq(
                            alertColumn, alertRow.getPKValue())));

                    setNotificationAlert(alertRow, category, exportAlert);
                    exportAlert.setAssigneeId(alertUserRow.get(userColumn));
                    exportAlert.setRead(alertUserRow.get(isReadColumn));
                    notificationAlerts.add(exportAlert);
                }
            }
        }

        return notificationAlerts;
    }

    /**
     * <p>
     * Sets {@link NotificationAlert}.
     * </p>
     *
     * @param notificationAlert {@link NotificationAlert}.
     * @param alertRow          {@link Row}.
     */
    private void setNotificationAlert(final Row alertRow, final NotificationAlert.Category category,
                                      final NotificationAlert notificationAlert) {
        notificationAlert.setOrgId(alertRow.getOrgId());
        notificationAlert.setId(alertRow.getPKValue());
        notificationAlert.setTimeStamp(alertRow.get(OMANNOTIFICATIONALERT.TIMESTAMP));
        notificationAlert.setContent(Json.object((String) alertRow.get(OMANNOTIFICATIONALERT.CONTENT)));
        notificationAlert.setSeverity(NotificationAlert.Severity.ALERT);
        notificationAlert.setCategory(category);
    }

    /**
     * <p>
     * Sets common alert row fields.
     * </p>
     *
     * @param alertRow {@link Row}.
     * @param alert    {@link NotificationAlert}.
     */
    private void setAlertRow(final Row alertRow, final NotificationAlert alert) {
        final Long timeStamp = alert.getTimeStamp();

        alertRow.setOrgId(alert.getOrgId());
        alertRow.set(OMANNOTIFICATIONALERT.TIMESTAMP, Objects.isNull(timeStamp) ?
                Instant.now().toEpochMilli() : alert.getTimeStamp());
        alertRow.set(OMANNOTIFICATIONALERT.CONTENT, alert.getContent().toString());
        alertRow.set(OMANNOTIFICATIONALERT.CATEGORY, alert.getCategory().getId());
        alertRow.set(OMANNOTIFICATIONALERT.SEVERITY, alert.getSeverity().getId());
    }

    /**
     * <p>
     * Sets alert users row fields.
     * </p>
     *
     * @param alertUsersRow {@link Row}.
     * @param alertId       Alert id.
     * @param userId        User id.
     */
    private void setAlertUsersRow(final Row alertUsersRow, final Long orgId, final Long alertId, final Long userId) {
        alertUsersRow.setOrgId(orgId);
        alertUsersRow.set(OMANNOTIFICATIONALERTUSERS.ALERT_ID, alertId);
        alertUsersRow.set(OMANNOTIFICATIONALERTUSERS.USER_ID, userId);
    }

    /**
     * <p>
     * Sets the alert for a particular user in an org.
     * </p>
     *
     * @param alert      {@link NotificationAlert}.
     * @param assigneeId Represents an assignee id.
     */
    private void setSingleUserAlertRow(final NotificationAlert alert, final Long assigneeId) {
        final Long orgId = alert.orgId();
        final Row alertRow = new Row(getAlertTable());
        final Row alertUsersRow = new Row(getAlertUsersTable());
        final DataContainer targetContainer = DataContainer.create();

        setAlertRow(alertRow, alert);
        setAlertUsersRow(alertUsersRow, orgId, alertRow.getPKValue(), assigneeId);
        targetContainer.updateRow(alertUsersRow);
        targetContainer.updateRow(alertRow);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Sets the alert for a set of users in an org.
     * </p>
     *
     * @param alert   {@link NotificationAlert}.
     * @param userIds set of users in an org.
     */
    private void setMultiUsersAlertRows(final NotificationAlert alert, final Collection<Long> userIds) {
        final Long orgId = alert.orgId();
        final Table alertTable = getAlertTable();
        final Table alertUsersTable = getAlertUsersTable();
        final Row alertRow = new Row(alertTable);
        final DataContainer targetContainer = DataContainer.create();
        final Long pkValue = alertRow.getPKValue();

        setAlertRow(alertRow, alert);
        targetContainer.updateRow(alertRow);

        userIds.forEach(userId -> {
            final Row alertUsersRow = new Row(alertUsersTable);

            setAlertUsersRow(alertUsersRow, orgId, pkValue, userId);
            targetContainer.updateRow(alertUsersRow);
        });
    }

    /**
     * <p>
     * Gets the alert table.
     * </p>
     *
     * @return alert {@link Table}.
     */
    private Table getAlertTable() {
        return Table.get(OMANNOTIFICATIONALERT.TABLE);
    }

    /**
     * <p>
     * Gets the alert users table.
     * </p>
     *
     * @return alert {@link Table}.
     */
    private Table getAlertUsersTable() {
        return Table.get(OMANNOTIFICATIONALERTUSERS.TABLE);
    }

    /**
     * <p>
     * Gets the activity reminder alert table.
     * </p>
     *
     * @return the activity reminder alert {@link Table}.
     */
    private Table getActivityReminderAlertTable() {
        return Table.get(OMANACTIVITYREMINDERALERT.TABLE);
    }
}