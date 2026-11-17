package com.tlc.crm.notifications.api.manager;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserNotificationPreference;
import com.tlc.crm.common.models.NotificationState;
import com.tlc.crm.core.features.api.activity.manager.SourceActivityManager;
import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notifications.api.model.AppNotification;
import com.tlc.crm.notifications.api.model.NotificationUser;
import com.tlc.crm.notifications.service.AppNotificationManager;
import com.tlc.crm.notifications.sql.resource.OMANNOTIFICATION;
import com.tlc.crm.notifications.sql.resource.OMANUSERS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * A {@link AppNotificationManagerImpl} that provides the various methods for the {@link AppNotificationManager}
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 * @see AppNotificationManager
 */
public class AppNotificationManagerImpl extends AbstractNotificationManager implements AppNotificationManager {

    private final SourceActivityManager activityManager;
    private final UserPreferenceManager preferenceManager;

    private AppNotificationManagerImpl() {
        activityManager = SourceActivityManager.getInstance();
        preferenceManager = UserPreferenceManager.getInstance();
    }

    private static class Instance {
        private static final AppNotificationManagerImpl INSTANCE = new AppNotificationManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link AppNotificationManagerImpl} Instance
     * </p>
     *
     * @return The instance of {@link AppNotificationManagerImpl}
     */
    public static AppNotificationManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the app notification table
     * </p>>
     *
     * @return the notification {@link Table}
     */
    private Table getTable() {
        return Table.get(OMANNOTIFICATION.TABLE);
    }

    /**
     * <p>
     * Gets the user notification table
     * </p>
     *
     * @return the user notification {@link Table}
     */
    private Table getUserNotificationTable() {
        return Table.get(OMANUSERS.TABLE);
    }

    /**
     * <p>
     * Gets the crm user table
     * </p>
     *
     * @return the crm user {@link Table}
     */
    private Table getCrmUserTable() {
        return Table.get(OMCRMUSER.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     */
    @Override
    public void markAllAsRead(final long orgId, final long userId) {
        final Table userNotificationTable = getUserNotificationTable();
        final UpdateQuery updateQuery = UpdateQuery.get(userNotificationTable);

        updateQuery.addUpdateColumn(userNotificationTable.getColumn(OMANUSERS.IS_READ), true);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.USER_ID), userId)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return true, when user has any notification
     */
    @Override
    public boolean exists(final long orgId, final long userId) {
        final Table userNotificationTable = getUserNotificationTable();
        final CountQuery countQuery = CountQuery.get(userNotificationTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.USER_ID), userId)));
        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /***
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param id     Represents the notification id
     * @return true, if notification is present
     */
    @Override
    public boolean exists(final long orgId, final long userId, final long id) {
        final Table userNotificationTable = getUserNotificationTable();
        final CountQuery countQuery = CountQuery.get(userNotificationTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.USER_ID), userId))
                .and(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.NOTIFICATION_ID), id)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param id     Represents the notification id
     */
    @Override
    public void markAsRead(final long orgId, final long userId, final long id) {
        final Table userNotificationTable = getUserNotificationTable();
        final UpdateQuery updateQuery = UpdateQuery.get(userNotificationTable);

        updateQuery.addUpdateColumn(userNotificationTable.getColumn(OMANUSERS.IS_READ), true);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.NOTIFICATION_ID), id))
                .and(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.USER_ID), userId)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represents an organization id
     * @param userId            Represents the user id
     * @param notificationState Represents the notification state
     */
    @Override
    public void updateNotificationState(final long orgId, final long userId, final NotificationState notificationState) {
        final UserNotificationPreference notificationPreference = new UserNotificationPreference(notificationState);

        notificationPreference.setOrgId(orgId);
        notificationPreference.setUserId(userId);
        preferenceManager.updatePreference(notificationPreference);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the notification state (on/off)
     */
    @Override
    public NotificationState getNotificationState(final long orgId, final long userId) {
        final UserNotificationPreference userNotificationPreference = preferenceManager.get(orgId, userId).get(UserNotificationPreference.TYPE);

        return userNotificationPreference.getNotificationState();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the pending notification count
     */
    @Override
    public int getPendingNotificationCount(final long orgId, final long userId) {
        final Table userTable = getCrmUserTable();
        final SelectQuery selectQuery = SelectQuery.get(userTable);

        selectQuery.addSelectClause(userTable.getColumn(OMCRMUSER.PENDING_NOTIFICATION_COUNT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userTable.getPKColumn(), userId)));
        final Optional<Row> optionalRow = Optional.ofNullable(orgDataStore(orgId).get(selectQuery).getRow(userTable));

        return optionalRow.map(row -> (Integer) row.get(OMCRMUSER.PENDING_NOTIFICATION_COUNT)).orElseGet(() -> NumericalConstant.ZERO);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the total count
     */
    @Override
    public long getTotalCount(final long orgId, final long userId) {
        final Table userNotificationTable = getUserNotificationTable();
        final CountQuery countQuery = CountQuery.get(userNotificationTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.USER_ID), userId)));
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param start  Represents the start value
     * @param limit  Represents the limit value
     * @return the collection of {@link AppNotification}
     */
    @Override
    public Collection<AppNotification> get(final long orgId, final long userId, final int start, final int limit) {
        return get(orgId, userId, start, limit, new WhereClause(Criteria.eq(getUserNotificationTable().getColumn(OMANUSERS.USER_ID), userId)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id
     * @param activityId Represents the activity id
     * @return the icon id
     */
    @Override
    public Long getIconId(final long orgId, final long activityId) {
        return activityManager.getIconId(orgId, activityId);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param start  Represents the start value
     * @param limit  Represents the limit value
     * @return the {@link AppNotification}
     */
    @Override
    public Collection<AppNotification> getTopNotifications(final long orgId, final long userId, final int start, final int limit) {
        final Table userNotificationTable = getUserNotificationTable();

        return get(orgId, userId, start, limit, new WhereClause(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.USER_ID), userId))
                .and(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.IS_READ), false)));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId            An organization's id.
     * @param appNotifications Represents the {@link AppNotification} in a collection.
     */
    @Override
    public void storeNotification(final Long orgId, final Collection<AppNotification> appNotifications) {
        final DataContainer dataContainer = DataContainer.create();
        final Table table = getTable();

        for (final AppNotification appNotification : appNotifications) {
            final Row row = new Row(table);
            final String notification = appNotification.getNotification();
            final Collection<NotificationUser> recipients = appNotification.getRecipients();
            final boolean isRead = Objects.nonNull(appNotification.isRead()) && appNotification.isRead();

            row.setOrgId(orgId);
            row.set(OMANNOTIFICATION.NOTIFICATION_TYPE, appNotification.getType().getId());
            row.set(OMANNOTIFICATION.TIMESTAMP, appNotification.getActionTime());
            row.set(OMANNOTIFICATION.NOTIFICATION, notification);

            dataContainer.addNewRow(row);
            loadUsers(isRead, orgId, row.getPKValue(), dataContainer, recipients);
            updatePendingNotification(orgId, recipients);
        }

        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     */
    @Override
    public void resetPendingNotificationCount(final long orgId, final long userId) {
        final Table crmUserTable = getCrmUserTable();
        final UpdateQuery updateQuery = UpdateQuery.get(crmUserTable);

        updateQuery.addUpdateColumn(crmUserTable.getColumn(OMCRMUSER.PENDING_NOTIFICATION_COUNT), NumericalConstant.ZERO);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(crmUserTable.getPKColumn(), userId)));
        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Updates the pending notification status to true whenever the user gets new notification
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param recipients Represents the recipients
     */
    private void updatePendingNotification(final Long orgId, final Collection<NotificationUser> recipients) {
        final Table userTable = getCrmUserTable();
        final DataContainer targetContainer = DataContainer.create();
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final Column notificationCountColumn = userTable.getColumn(OMCRMUSER.PENDING_NOTIFICATION_COUNT);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(userTable.getPKColumn(), recipients.stream().map(NotificationUser::id).collect(Collectors.toUnmodifiableSet()))));

        orgDataStore(orgId).get(selectQuery).getRows(userTable).forEach(userRow -> {
            userRow.set(OMCRMUSER.PENDING_NOTIFICATION_COUNT, (int) userRow.get(notificationCountColumn) + 1);

            targetContainer.updateRow(userRow);
        });
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the notification users into the container
     * </p>
     *
     * @param isRead        Whether the notification is read.
     * @param orgId         Represents an organization id
     * @param pkValue       Represents the notification id
     * @param users         Represents the Collection of {@link NotificationUser}
     * @param dataContainer Represent the {@link DataContainer}
     */
    private void loadUsers(final boolean isRead, final long orgId, final Long pkValue, final DataContainer dataContainer,
                           final Collection<NotificationUser> users) {
        final Table userNotificationTable = getUserNotificationTable();

        for (final NotificationUser user : users) {
            final Row userRow = new Row(userNotificationTable);

            userRow.setOrgId(orgId);
            userRow.set(OMANUSERS.IS_READ, isRead);
            userRow.set(OMANUSERS.USER_ID, user.id());
            userRow.set(OMANUSERS.NOTIFICATION_ID, pkValue);
            dataContainer.addNewRow(userRow);
        }
    }

    /**
     * <p>
     * Converts the row to app notification
     * </p>
     *
     * @param userId        Represents the user id
     * @param row           Represents the {@link Row}
     * @param dataContainer Represents the {@link DataContainer}
     * @return the {@link AppNotification}
     */
    private AppNotification convertRowToModel(final long userId, final Row row, final DataContainer dataContainer) {
        final AppNotification appNotification = new AppNotification();
        final Table userNotificationTable = getUserNotificationTable();
        final Long pkValue = row.getPKValue();

        appNotification.setId(pkValue);
        appNotification.setOrgId(row.getOrgId());
        appNotification.setType(AppNotificationEventType.get(row.get(OMANNOTIFICATION.NOTIFICATION_TYPE)));
        appNotification.setActionTime(row.get(OMANNOTIFICATION.TIMESTAMP));
        appNotification.setNotification(row.get(OMANNOTIFICATION.NOTIFICATION));
        final Row userRow = dataContainer.getRow(userNotificationTable, new WhereClause(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.USER_ID), userId))
                .and(Criteria.eq(userNotificationTable.getColumn(OMANUSERS.NOTIFICATION_ID), pkValue)));

        if (Objects.nonNull(userRow)) {
            appNotification.setRead(userRow.get(OMANUSERS.IS_READ));
        }

        return appNotification;
    }

    /**
     * <p>
     * Gets the app notifications for the given criteria
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param start  Represents the start value
     * @param limit  Represents the limit value
     * @return the collection of {@link AppNotification}
     */
    private Collection<AppNotification> get(final long orgId, final long userId, final int start, final int limit, final WhereClause whereClause) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addJoinClause(new JoinClause(table.getPKColumn(), getUserNotificationTable().getColumn(OMANUSERS.NOTIFICATION_ID)));
        selectQuery.setWhereClause(whereClause);
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(table.getColumn(OMANNOTIFICATION.TIMESTAMP), OrderByClause.OrderType.DESCENDING));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return null;
        }

        return dataContainer.getRows(table).map(row -> convertRowToModel(userId, row, dataContainer)).toList();
    }
}
