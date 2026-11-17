package com.tlc.crm.notifications.calendar.api.manager;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.sql.resource.OMCALCALENDARSYNCSETTINGS;

import com.tlc.crm.notifications.calendar.api.model.CalendarSyncSettings;
import com.tlc.crm.notifications.calendar.api.model.internal.CalendarSyncConfig;
import com.tlc.crm.notifications.calendar.internal.status.CalendarErrorCodes;

import com.tlc.notification.model.email.EmailAddress;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.crm.common.sync.SyncMode;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;

import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Collection;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tlc.notification.model.sync.ConnectionStatus.ACTIVE;

/**
 * <p>
 * Implements the calendar sync settings related DB actions
 * </p>
 *
 * @author Ajith venkadesh, Kowsalya SP
 * @version 1.0
 */
public final class CalendarSyncManager extends AbstractConfigManager<CalendarSyncSettings> { // TODO: Move calendar functionalities related to the event after separating the event module.

    private final ActivityTypeManager activityTypeManager;

    private CalendarSyncManager() {
        this.activityTypeManager = ActivityTypeManager.getInstance();
    }

    private static class Instance {
        private static final CalendarSyncManager INSTANCE = new CalendarSyncManager();
    }

    /**
     * <p>
     * Gets the {@link CalendarSyncManager} Instance
     * </p>
     *
     * @return {@link CalendarSyncManager}
     */
    public static CalendarSyncManager getInstance() {
        return CalendarSyncManager.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the calendar sync table.
     * </p>
     *
     * @return the calendar sync table.
     */
    public Table getTable() {
        return Table.get(OMCALCALENDARSYNCSETTINGS.TABLE);
    }

    @Override
    protected ProcessOutcome update(final Long orgId, final Collection<CalendarSyncSettings> models) {
        return null;
    }

    /**
     * <p>
     * Creates action to synchronize an calendar
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param calendarSyncSettings Collection of calendarSyncSettings
     * @return {@link ProcessOutcome}
     */
    public ProcessOutcome create(final long orgId, final Collection<CalendarSyncSettings> calendarSyncSettings) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();

        calendarSyncSettings.forEach(syncSettings -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadDefaultSyncSettingsIntoContainer(true, syncSettings, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(syncSettings.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Create action: Calendar sync create failed Org ID : {} reason : {}", orgId, errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED,
                        "i18n.common.create.failed")));
            }
        });

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.CREATE_ACTION_FAILED,
                    "i18n.common.create.failed")), failedEntries));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Loads the calendar sync settings into container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event
     * @param syncSettings    An entity that holds calendar sync settings attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadDefaultSyncSettingsIntoContainer(final boolean isCreateEvent, final CalendarSyncSettings syncSettings,
                                                      final DataContainer targetContainer) {
        final Table calendarSyncSettingsTable = getTable();
        final EmailAddress emailAddress = syncSettings.getAddress();
        final Long orgId = emailAddress.orgId();

        final DataContainer dataContainer = orgDataStore(orgId).get(calendarSyncSettingsTable,
                new WhereClause(Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS), emailAddress.getEmailId()))
                        .and(Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), syncSettings.getUserId())));

        loadDefaultSyncSettingsIntoContainer(isCreateEvent, syncSettings, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Checks whether the synced calendar id exists or not.
     * </p>
     *
     * @param orgId            Represents an organization id.
     * @param syncedCalendarId Represents the synced calendar id.
     * @return whether the synced calendar id exists or not
     */
    @Override
    public boolean exists(final Long orgId, final Long syncedCalendarId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), syncedCalendarId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    @Override
    public Collection<CalendarSyncSettings> get(final Long orgId, final Collection<Long> ids) {
        return null;
    }

    @Override
    public Collection<CalendarSyncSettings> partialGet(final Long orgId, final Collection<Long> ids) {
        return null;
    }

    @Override
    public CalendarSyncSettings create(final CalendarSyncSettings model) {
        return null;
    }

    /**
     * <p>
     * Checks whether the calendar synced exists or not for the given organization and user.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @param syncId Represents the synchronization id
     * @return true, if the calendar sync is exists; otherwise false
     */
    public boolean exists(final long orgId, final long userId, final Long syncId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS), ACTIVE.getId()));

        countQuery.setWhereClause(Objects.isNull(syncId) ? whereClause : whereClause.and(Criteria.eq(table.getPKColumn(), syncId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the {@link ConnectionStatus} for the specified organization, user, and email address.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link ConnectionStatus}
     */
    public ConnectionStatus getConnectionStatus(final long orgId, final long userId, final EmailAddress emailAddress) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                        .and(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS), emailAddress.getEmailId())),
                Set.of(table.getColumn(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS))).getRow(table);

        return Objects.isNull(row) ? null : ConnectionStatus.get(row.get(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS));
    }

    /**
     * <p>
     * Checks the existence of the provided email address
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param emailAddress Represents the email address to be synced
     * @return {@link Boolean} existence status
     */
    public boolean exists(final Long orgId, final EmailAddress emailAddress) {
        final Table syncTable = getTable();
        final CountQuery countQuery = CountQuery.get(syncTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(syncTable.getColumn(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS), emailAddress.getEmailId()))
                .and(Criteria.eq(syncTable.getColumn(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED), true)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the synced email addresses.
     * </p>
     *
     * @return a map where each email address is associated with its corresponding synced calendar information.
     */
    public Collection<CalendarSyncSettings> retrieveSyncedCalendarData() {
        final Table calendarSyncSettingsTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(calendarSyncSettingsTable);

        selectQuery.addSelectClause(calendarSyncSettingsTable);
        selectQuery.setWhereClause(
                new WhereClause(
                        Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED), true)
                ).and(
                        Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_MODE), SyncMode.TWO_WAY.id())
                )
        );

        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyList();
        }

        return dataContainer.getRows(calendarSyncSettingsTable).map(this::convertRowToModel).collect(Collectors.toList());
    }

    /**
     * <p>
     * Loads the default calendar sync settings into the target {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event
     * @param syncSettings    An entity that holds email sync settings attributes
     * @param sourceContainer Holds the user defined values of existing sync action
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadDefaultSyncSettingsIntoContainer(final boolean isCreateEvent, final CalendarSyncSettings syncSettings,
                                                      final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table calendarSyncSettingsTable = getTable();
        final Long orgId = syncSettings.orgId();
        final Long userId = syncSettings.getUserId();
        final Row syncRow = sourceContainer.isEmpty() ? new Row(calendarSyncSettingsTable) : sourceContainer.getRow(calendarSyncSettingsTable);

        if (!isCreateEvent && Objects.isNull(syncRow)) {
            throw ErrorCode.get(CalendarErrorCodes.EMAIL_NOT_FOUND, "i18n.notifications.calendar.invalid.emailAddress");
        }
        final Long syncRowPkColumn = syncRow.getPKValue();
        final CalendarSyncConfig calendarSyncConfig = syncSettings.getCalendarSyncConfig();

        if (syncRow.isNewRow()) {
            syncRow.setOrgId(orgId);
            syncRow.set(OMCALCALENDARSYNCSETTINGS.USER_ID, userId);
            syncRow.set(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED, false);
            syncRow.set(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS, syncSettings.getAddress().getEmailId());
            syncRow.set(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS, ACTIVE.getId());
        }
        syncRow.set(OMCALCALENDARSYNCSETTINGS.SYNC_MODE, SyncMode.getMode(calendarSyncConfig.getSyncType()).id());
        syncRow.set(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG, calendarSyncConfig.getConfigJsonAsString());
        syncRow.set(OMCALCALENDARSYNCSETTINGS.SYNC_STATUS, SyncStatus.NEW.getId());
        syncSettings.setId(syncRowPkColumn);
        disableSyncStatus(orgId, userId, new WhereClause(Criteria.notEq(calendarSyncSettingsTable.getPKColumn(), syncRowPkColumn)));

        targetContainer.updateRow(syncRow);
    }

    /**
     * <p>
     * Disables the sync {@link ConnectionStatus} and disables the calendar sync account.
     * </p>
     *
     * @param orgId       Represents an organization id.
     * @param userId      Represents the user id
     * @param whereClause Refers the {@link WhereClause} by which the data to be fetched.
     */
    public void disableSyncStatus(final long orgId, final long userId, final WhereClause whereClause) {
        final Table calendarSyncTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(calendarSyncTable);

        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED), false);
        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS), ConnectionStatus.INACTIVE.getId());
        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.LAST_SYNC_TIME), null);
        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_STATUS), SyncStatus.NEW.getId());
        updateQuery.setWhereClause(whereClause.and(Criteria.eq(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Gets the calendar sync settings configuration {@link CalendarSyncSettings}.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return {@link CalendarSyncSettings}
     */
    public CalendarSyncSettings getCalenderSyncSettings(final long orgId, final long userId) {

        if (exists(orgId, userId, null)) {
            final Table calendarSyncTable = getTable();
            final DataContainer dataContainer = orgDataStore(orgId).get(calendarSyncTable,
                    new WhereClause(Criteria.eq(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                            .and(Criteria.eq(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS), ACTIVE.getId())));
            final Row calendarSyncRow = dataContainer.getRow(calendarSyncTable);

            if (Objects.isNull(calendarSyncRow)) {
                return null;
            }
            final CalendarSyncSettings calendarSyncSettings = convertRowToModel(calendarSyncRow);

            loadActivityTypes(orgId, calendarSyncSettings);

            return calendarSyncSettings;
        }

        return null;
    }

    /**
     * <p>
     * Converts a database {@link Row} object into a {@link CalendarSyncSettings} model.
     * </p>
     *
     * @param row The database row containing calendar sync configuration details.
     * @return The {@link CalendarSyncSettings} model populated with data from the row.
     */
    private CalendarSyncSettings convertRowToModel(final Row row) {
        final CalendarSyncSettings calendarSyncSettings = new CalendarSyncSettings();

        calendarSyncSettings.setSyncStatus(SyncStatus.getStatus(row.get(OMCALCALENDARSYNCSETTINGS.SYNC_STATUS)));
        final String inProgressConfig = row.get(OMCALCALENDARSYNCSETTINGS.IN_PROGRESS_CONFIG);

        if (Objects.nonNull(inProgressConfig)) {
            calendarSyncSettings.setInProgressSyncConfig(new CalendarSyncConfig(inProgressConfig));
        }
        calendarSyncSettings.setCalendarSyncConfig(new CalendarSyncConfig(row.get(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG)));
        calendarSyncSettings.setId(row.getPKValue());
        calendarSyncSettings.setOrgId(row.getOrgId());
        calendarSyncSettings.setUserId(row.get(OMCALCALENDARSYNCSETTINGS.USER_ID));
        calendarSyncSettings.setSyncEnabled(row.get(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED));
        calendarSyncSettings.setAddress(new EmailAddress(row.get(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS)));
        calendarSyncSettings.setLastSyncTime(row.get(OMCALCALENDARSYNCSETTINGS.LAST_SYNC_TIME));

        return calendarSyncSettings;
    }

    /**
     * <p>
     * Loads and updates the activity types associated with the given calendar sync configuration.
     * </p>
     *
     * @param orgId                The organization ID.
     * @param calendarSyncSettings The {@link CalendarSyncSettings} to be updated with activity types.
     */
    private void loadActivityTypes(final long orgId, final CalendarSyncSettings calendarSyncSettings) {
        final CalendarSyncConfig calendarSyncConfig = calendarSyncSettings.getCalendarSyncConfig();
        final Collection<ActivityType> activityTypes = activityTypeManager.getActivityTypes(orgId, true);
        final Collection<Long> activityTypeIds = calendarSyncConfig.getActivityTypeIds();
        final Collection<Long> activeTypeIds = new HashSet<>();
        final ActivityType type;

        for (final ActivityType activityType : activityTypes) {
            final Long id = activityType.id();

            activityType.setActive(activityTypeIds.contains(id));
            activeTypeIds.add(id);
        }

        if (Objects.equals(SyncMode.TWO_WAY, SyncMode.getMode(calendarSyncConfig.getSyncType()))) {
            final Long calendarEventId = calendarSyncConfig.getCalendarEventId();

            type = activeTypeIds.contains(calendarEventId) ? activityTypeManager.get(orgId, calendarEventId)
                    : activityTypeManager.getDefaultActivityType(orgId);
        } else {
            type = null;
        }
        calendarSyncConfig.setSyncTypeConfig(type);
        calendarSyncConfig.setActivityTypes(activityTypes);
        calendarSyncSettings.setCalendarSyncConfig(calendarSyncConfig);
    }

    /**
     * <p>
     * Stops the synchronization process for a specific user and organization.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param syncId Represents the calendar sync id
     */
    public void stopSync(final long orgId, final long userId, final long syncId) {
        disableSyncStatus(orgId, userId, new WhereClause(Criteria.eq(getTable().getPKColumn(), syncId)));
    }

    /**
     * <p>
     * Loads the settings configuration of {@link CalendarSyncSettings} in to container.
     * </p>
     *
     * @param calendarSyncSettings Represents the {@link CalendarSyncSettings}
     */
    public void loadCalendarSyncSettingsIntoContainer(final CalendarSyncSettings calendarSyncSettings) {
        final Long orgId = calendarSyncSettings.orgId();
        final Long userId = calendarSyncSettings.getUserId();
        final Long syncSettingsId = calendarSyncSettings.id();
        final Long currentTime = Instant.now().toEpochMilli();

        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer targetContainer = DataContainer.create();

        final Table calendarSyncTable = getTable();
        final Row row = orgDataStore.get(calendarSyncTable, syncSettingsId);
        final Row calendarRow = Objects.isNull(row) ? new Row(calendarSyncTable) : row;
        final CalendarSyncConfig calendarSyncConfig = calendarSyncSettings.getCalendarSyncConfig();

        if (calendarRow.isNewRow()) {
            calendarRow.setOrgId(orgId);
            calendarRow.set(OMCALCALENDARSYNCSETTINGS.USER_ID, calendarSyncSettings.getUserId());
            calendarRow.set(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS, calendarSyncSettings.getAddress().id());
            calendarRow.set(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS, ACTIVE.getId());
        }
        calendarRow.set(OMCALCALENDARSYNCSETTINGS.SYNC_MODE, SyncMode.getMode(calendarSyncConfig.getSyncType()).id());
        calendarRow.set(OMCALCALENDARSYNCSETTINGS.LAST_SYNC_TIME, currentTime);
        calendarRow.set(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED, true);
        calendarRow.set(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG, calendarSyncConfig.getConfigJsonAsString());
        calendarSyncSettings.setLastSyncTime(currentTime);

        targetContainer.updateRow(calendarRow);
        disableSyncStatus(orgId, userId, new WhereClause(Criteria.notEq(calendarSyncTable.getPKColumn(), syncSettingsId)));
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * <p>
     * Updates the calendar {@link SyncStatus} for an respective calendar sync id.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    public void updateCalendarSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo) {
        final Table calendarsyncSettingsTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(calendarsyncSettingsTable);
        final SyncStatus syncStatus = syncInfo.syncStatus();

        if (!SyncStatus.NEW.equals(syncStatus)) {
            updateQuery.addUpdateColumn(calendarsyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.IN_PROGRESS_CONFIG), syncInfo.inProgressConfig());
        }
        updateQuery.addUpdateColumn(calendarsyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_STATUS), syncStatus.getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(calendarsyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(calendarsyncSettingsTable.getPKColumn(), syncInfo.syncId())));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Gets the calendar {@link SyncInfo} for an respective sync id.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return {@link SyncInfo}
     */
    public SyncInfo getCalendarSyncInfo(final long orgId, final long syncId) {
        final Row row = orgDataStore(orgId).get(getTable(), syncId);

        return new SyncInfo(row.getPKValue(), row.get(OMCALCALENDARSYNCSETTINGS.LAST_SYNC_TIME), SyncStatus
                .getStatus(row.get(OMCALCALENDARSYNCSETTINGS.SYNC_STATUS)), null);
    }

    /**
     * <p>
     * Gets the calendar sync email address for an respective organization and user.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @param id     Represents the sync id
     * @return the synced email address
     */
    public String getCalendarSyncEmailAddress(final long orgId, final long userId, final Long id) {
        final Table calendarSyncSettingsTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(calendarSyncSettingsTable);

        selectQuery.addSelectClause(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(calendarSyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(calendarSyncSettingsTable.getPKColumn(), id)));

        final Row row = orgDataStore(orgId).get(selectQuery).getRow(calendarSyncSettingsTable);

        return Objects.isNull(row) ? null : row.get(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS);
    }

    /**
     * <p>
     * Updates the default calendar sync configuration for the existing synced {@link EmailAddress}.
     * </p>
     *
     * @param orgId   Represents an organization id
     * @param userId  Represents the user id
     * @param address Represents the {@link EmailAddress}
     */
    public void updateDefaultConfiguration(final long orgId, final long userId, final EmailAddress address) {
        final Table calendarSyncTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(calendarSyncTable);

        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.CONNECTION_STATUS), ACTIVE.getId());
        updateQuery.addUpdateColumn(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG), CalendarSyncConfig
                .getDefaultConfigJsonAsString(activityTypeManager.getActivityTypes(orgId, true)));
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(calendarSyncTable.getColumn(OMCALCALENDARSYNCSETTINGS.EMAIL_ADDRESS), address.getEmailId())));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Gets the calendar sync config for an respective organization.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the sync config
     */
    public Optional<String> getCalendarSyncConfig(final long orgId, final long userId) {
        final Table table = getTable();
        final Column syncConfigColumn = table.getColumn(OMCALCALENDARSYNCSETTINGS.SYNC_CONFIG);
        final DataContainer dataContainer = orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCALCALENDARSYNCSETTINGS.IS_SYNC_ENABLED), true)), List.of(syncConfigColumn));
        final Row calendarSyncRow = Optional.ofNullable(dataContainer.getRow(table)).orElseGet(() -> null);

        if (Objects.isNull(calendarSyncRow)) {
            return Optional.empty();
        }

        return Optional.ofNullable(calendarSyncRow.get(syncConfigColumn));
    }

    /**
     * <p>
     * Updates the calendar {@link SyncStatus} for an respective calendar sync id.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents the user id
     * @param lastSyncTime Represents the last sync time
     */
    public void updateCalendarLastSyncTime(final long orgId, final long userId, final long lastSyncTime) {
        final Table calendarsyncSettingsTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(calendarsyncSettingsTable);

        updateQuery.addUpdateColumn(calendarsyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.LAST_SYNC_TIME), lastSyncTime);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(calendarsyncSettingsTable.getColumn(OMCALCALENDARSYNCSETTINGS.USER_ID), userId)));

        orgDataStore(orgId).update(updateQuery);
    }
}
