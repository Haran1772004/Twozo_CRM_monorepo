package com.tlc.email.server.internal.manager;

import com.tlc.email.common.converter.FolderIntervalConverter;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.config.FolderInterval;
import com.tlc.email.common.request.SyncRequest;

import com.tlc.email.server.internal.sync.interval.SyncIntervalHandler;
import com.tlc.email.server.model.SyncSetting;
import com.tlc.email.server.service.EmailProviderService;
import com.tlc.email.server.sql.resource.MEMADDRESS;
import com.tlc.email.server.sql.resource.MEMSYNCSETTINGS;

import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;

import java.time.Instant;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.tlc.notification.model.sync.SyncStatus.NEW;

/**
 * <p>
 * Provides the email sync settings db related actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class SyncSettingsManagerImpl extends AbstractEmailManager<SyncSetting> implements SyncSettingsManager {

    private final SyncIntervalHandler syncIntervalHandler;
    private final FolderIntervalConverter folderIntervalConverter;

    private SyncSettingsManagerImpl() {
        this.syncIntervalHandler = SyncIntervalHandler.getInstance();
        this.folderIntervalConverter = FolderIntervalConverter.getInstance();
    }

    private static class Instance {
        private static final SyncSettingsManagerImpl INSTANCE = new SyncSettingsManagerImpl();
    }

    public static SyncSettingsManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table}
     */
    @Override
    public Table getTable() {
        return getSyncSetingsTable();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the email id.
     * @return {@link SyncSetting}
     */
    @Override
    public Optional<SyncSetting> get(final long id) {
        final Table syncSetingsTable = getTable();
        final DataContainer dataContainer = getSyncSettingsContainer(List.of(id));
        final Row row = dataContainer.getRow(syncSetingsTable);

        return Optional.ofNullable(row).map(syncRow -> convertRowToModel(syncRow, dataContainer));
    }

    /**
     * {@inheritDoc}
     *
     * @param syncIds Refers the email ids.
     * @return {@link Collection} of {@link SyncSetting}
     */
    @Override
    public Collection<SyncSetting> get(final Collection<Long> syncIds) {
        final Table syncSetingsTable = getTable();
        final DataContainer dataContainer = getSyncSettingsContainer(syncIds);

        return dataContainer.getRows(syncSetingsTable).map(row ->
                convertRowToModel(row, dataContainer)).toList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncRequest     Represents the {@link SyncRequest}
     * @param targetContainer Represents the container to load the updated email related actions
     */
    @Override
    public void loadSyncSettingsIntoContainer(final SyncRequest syncRequest, final DataContainer targetContainer) {
        loadSyncSettings(syncRequest, targetContainer);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncRequest Represents the {@link SyncRequest}
     */
    @Override
    public void loadSyncSettingsIntoContainer(final SyncRequest syncRequest) {
        final DataContainer targetContainer = DataContainer.create();

        loadSyncSettings(syncRequest, targetContainer);

        adminDataStore().commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param syncId        The unique identifier for the sync process.
     * @param folderName    The name of the folder being updated.
     * @param emailInterval The email interval containing start and end times to be added.
     */
    @Override
    public void updateDateConfigIntoContainer(final Long syncId, final String folderName, final EmailInterval emailInterval) {
        final DataContainer dataContainer = DataContainer.create();
        final Table table = getSyncSetingsTable();
        final Row syncSettingsRow = getExistingContainer(syncId).getRow(table);

        if (Objects.nonNull(syncSettingsRow)) {
            syncSettingsRow.set(MEMSYNCSETTINGS.LAST_SYNC_TIME, Instant.now().toEpochMilli());
            syncSettingsRow.set(MEMSYNCSETTINGS.DATE_CONFIG, syncIntervalHandler.getUpdatedDateConfig(syncSettingsRow
                    .get(MEMSYNCSETTINGS.DATE_CONFIG), folderName, emailInterval));

            dataContainer.updateRow(syncSettingsRow);
            adminDataStore().commitChanges(dataContainer);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>>
     *
     * @param emailAddress The {@link EmailAddress} containing the email address to search for.
     * @return An {@link Optional} containing the date configuration if found, otherwise an empty {@link Optional}.
     */
    @Override
    public Optional<String> getDateConfig(final String emailAddress) {
        final Table syncSetingsTable = getTable();
        final Table emailAddressTable = getAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addSelectClause(syncSetingsTable.getColumn(MEMSYNCSETTINGS.DATE_CONFIG));
        selectQuery.addJoinClause(getJoinClause());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailAddressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final Row row = adminDataStore().get(selectQuery).getRow(syncSetingsTable);

        return Optional.ofNullable(row).map(syncRow -> syncRow.get(MEMSYNCSETTINGS.DATE_CONFIG));
    }

    /**
     * <p>
     * Gets the sync email id for the given email address.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return the sync email id
     */
    @Override
    public Optional<Long> getId(final String emailAddress) {
        final Table emailAddressTable = getAddressTable();
        final Table syncSetingsTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addSelectClause(syncSetingsTable.getPKColumn());
        selectQuery.addJoinClause(getJoinClause());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailAddressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final Row row = adminDataStore().get(selectQuery).getRow(syncSetingsTable);

        return Optional.ofNullable(row).map(Row::getPKValue);
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link SyncSetting}.
     * </p>
     *
     * @param limit Represents the limit
     * @return {@link Collection} of {@link SyncSetting}
     */
    @Override
    public Collection<SyncSetting> get(final short limit) {
        final Table table = getTable();
        final Table emailAddressTable = getAddressTable();
        final Column syncStatusColumn = table.getColumn(MEMSYNCSETTINGS.SYNC_STATUS);
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addJoinClause(getJoinClause());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MEMSYNCSETTINGS.IS_SYNC_ENABLED), true))
                .and(new WhereClause(Criteria.notIn(table.getColumn(MEMSYNCSETTINGS.PROVIDER), EmailProvider.getOauthProviders().stream().map(EmailProvider::getId).collect(Collectors.toSet()))))
                .and(new WhereClause(Criteria.eq(syncStatusColumn, SyncStatus.SYNCED.getId()))
                        .or(Criteria.eq(syncStatusColumn, SyncStatus.FAILED.getId()))));
        selectQuery.setLimitClause(new LimitClause(limit));
        selectQuery.addOrderByClause(
                List.of(
                        new OrderByClause(table.getColumn(MEMSYNCSETTINGS.LAST_SYNC_TIME)),
                        new OrderByClause(table.getPKColumn())
                )
        );
        final DataContainer dataContainer = adminDataStore().get(selectQuery);

        return dataContainer.getRows(table).map(row -> convertRowToModel(row, dataContainer)).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param syncStatusInfo {@link SyncStatusInfo}
     */
    @Override
    public void updateSyncStatus(final SyncStatusInfo syncStatusInfo) {
        final Table syncSettingTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(syncSettingTable);
        final SyncStatus syncStatus = syncStatusInfo.getSyncStatus();

        if (SyncStatus.NEW.equals(syncStatus)) {
            updateQuery.addUpdateColumn(syncSettingTable.getColumn(MEMSYNCSETTINGS.IS_SYNC_ENABLED), true);
        }
        updateQuery.addUpdateColumn(syncSettingTable.getColumn(MEMSYNCSETTINGS.SYNC_STATUS), syncStatus.getId());
        updateQuery.addUpdateColumn(syncSettingTable.getColumn(MEMSYNCSETTINGS.LAST_SYNC_TIME), syncStatusInfo.getLastSyncedTime());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(syncSettingTable.getPKColumn(), syncStatusInfo.getSyncId())));

        adminDataStore().update(updateQuery);
    }

    /**
     * <p>
     * Gets the {@link SyncStatusInfo} for an respective sync id.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link SyncStatusInfo}
     */
    @Override
    public SyncStatusInfo getSyncInfo(final String emailAddress) {
        final Table syncSetingsTable = getTable();
        final Table addressTable = getAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(addressTable);

        selectQuery.addSelectClause(List.of(syncSetingsTable.getColumn(MEMSYNCSETTINGS.SYNC_STATUS),
                syncSetingsTable.getColumn(MEMSYNCSETTINGS.LAST_SYNC_TIME),
                syncSetingsTable.getColumn(MEMSYNCSETTINGS.DATE_CONFIG)));
        selectQuery.addJoinClause(getJoinClause());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(addressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final Row row = adminDataStore().get(selectQuery).getRow(syncSetingsTable);

        if (Objects.nonNull(row)) {
            final SyncStatusInfo syncStatusInfo = new SyncStatusInfo(row.getPKValue(), emailAddress,
                    SyncStatus.getStatus(row.get(MEMSYNCSETTINGS.SYNC_STATUS)));

            syncStatusInfo.setLastSyncedTime(row.get(MEMSYNCSETTINGS.LAST_SYNC_TIME));
            syncStatusInfo.setFolderIntervals(folderIntervalConverter.getFolderIntervals((String) row.get(MEMSYNCSETTINGS.DATE_CONFIG)));

            return syncStatusInfo;
        }

        return null;
    }

    /**
     * <p>
     * Converts the sync setting row to {@link SyncSetting}.
     * </p>
     *
     * @param row           Represents the sync setting row
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link SyncSetting}
     */
    @Override
    public SyncSetting convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Table emailAddressTable = getAddressTable();
        final SyncSetting syncSetting = new SyncSetting();
        final Long syncPkValue = row.getPKValue();

        syncSetting.setId(syncPkValue);
        final Row emailAddressRow = dataContainer.getRow(emailAddressTable, (Long) row.get(MEMSYNCSETTINGS.EMAIL_ID));

        if (Objects.nonNull(emailAddressRow)) {
            syncSetting.setEmailAddress(new SyncAddressInfo(emailAddressRow.getPKValue(), emailAddressRow.get(MEMADDRESS.EMAIL_ADDRESS)));
        }
        final String dateConfig = row.get(MEMSYNCSETTINGS.DATE_CONFIG);

        if (Objects.nonNull(dateConfig)) {
            final Map<FolderType, List<EmailInterval>> emailIntervalsByFolder = syncIntervalHandler
                    .retrieveEmailIntervalsByFolderType(dateConfig);

            for (final Map.Entry<FolderType, List<EmailInterval>> folderTypeEntry : emailIntervalsByFolder.entrySet()) {
                final FolderInterval interval = new FolderInterval();

                interval.setFolder(folderTypeEntry.getKey());
                interval.setEmailIntervals(folderTypeEntry.getValue());

                syncSetting.add(interval);
            }
        }

        syncSetting.setLastSyncTime(row.get(MEMSYNCSETTINGS.LAST_SYNC_TIME));
        syncSetting.setConnectionStatus(ConnectionStatus.get(row.get(MEMSYNCSETTINGS.CONNECTION_STATE)));
        syncSetting.setSyncStatus(SyncStatus.getStatus(row.get(MEMSYNCSETTINGS.SYNC_STATUS)));

        return syncSetting;
    }

    /**
     * <p>
     * Gets the existing sync settings {@link DataContainer}.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link DataContainer}
     */
    private DataContainer getExistingContainer(final String emailAddress) {
        final Table emailAddressTable = getAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addJoinClause(getJoinClause());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailAddressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Gets the existing sync settings {@link DataContainer}.
     * </p>
     *
     * @param syncId Represents the sync email id
     * @return {@link DataContainer}
     */
    private DataContainer getExistingContainer(final Long syncId) {
        final Table table = getTable();
        final Table emailAddressTable = getAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addJoinClause(getJoinClause());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), syncId)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * Loads the configuration into the container for the given email properties.
     * Checks the date configuration for an existing record associated with the email address
     * and loads or creates a new row in the target container.
     *
     * @param syncRequest     Represents the {@link SyncRequest}
     * @param targetContainer Holds the updated sync settings into the target container
     */
    private void loadSyncSettings(final SyncRequest syncRequest, final DataContainer targetContainer) {
        final EmailReceiverConfiguration configuration = syncRequest.getReceiverConfiguration();
        final SyncAddressInfo syncAddressInfo = configuration.getSyncEmailAddress();
        final Collection<FolderType> folderTypes = configuration.getFolderTypes();
        final String emailId = syncAddressInfo.getEmailAddress();
        final DataContainer dataContainer = getExistingContainer(emailId);
        final Table emailAddressTable = getAddressTable();
        final Table table = getSyncSetingsTable();
        final Row existingAddressRow = dataContainer.getRow(emailAddressTable);
        final Row emailAddressRow = Objects.isNull(existingAddressRow) ? new Row(emailAddressTable) : existingAddressRow;
        final long currentTime = Instant.now().toEpochMilli();

        if (emailAddressRow.isNewRow()) {
            emailAddressRow.set(MEMADDRESS.EMAIL_ADDRESS, emailId);

            targetContainer.updateRow(emailAddressRow);
        }
        final Row existingSyncSettingsRow = dataContainer.getRow(table);
        final Row syncSettingsRow = Objects.isNull(existingSyncSettingsRow) ? new Row(table) : existingSyncSettingsRow;

        if (syncSettingsRow.isNewRow()) {
            syncSettingsRow.set(MEMSYNCSETTINGS.EMAIL_ID, emailAddressRow.getPKValue());
            syncSettingsRow.set(MEMSYNCSETTINGS.CREATED_TIME, currentTime);
            syncSettingsRow.set(MEMSYNCSETTINGS.CONNECTION_STATE, ConnectionStatus.ACTIVE.getId());
            syncSettingsRow.set(MEMSYNCSETTINGS.SYNC_STATUS, NEW.getId());
            syncSettingsRow.set(MEMSYNCSETTINGS.IS_DRAFT_ENABLED, Objects.nonNull(folderTypes) && folderTypes.contains(FolderType.DRAFT));
            syncSettingsRow.set(MEMSYNCSETTINGS.PROVIDER, EmailProviderService.getProvider(syncAddressInfo).getId());
        }
        syncSettingsRow.set(MEMSYNCSETTINGS.IS_SYNC_ENABLED, true);
        syncSettingsRow.set(MEMSYNCSETTINGS.LAST_SYNC_TIME, currentTime);

        syncRequest.setId(syncSettingsRow.getPKValue());
        syncAddressInfo.setId(emailAddressRow.getPKValue());

        targetContainer.updateRow(syncSettingsRow);
    }

    /**
     * Gets the {@link JoinClause} which inter linked with the email.
     *
     * @return {@link JoinClause}
     */
    private JoinClause getJoinClause() {
        return new JoinClause(getAddressTable().getPKColumn(), getTable().getColumn(MEMSYNCSETTINGS.EMAIL_ID), JoinClause.JoinType.LEFT);
    }

    /**
     * Gets the {@link DataContainer} of sync settings.
     *
     * @param syncIds Represents the {@link Collection} of sync ids
     * @return {@link DataContainer} of sync settings
     */
    private DataContainer getSyncSettingsContainer(final Collection<Long> syncIds) {
        final SelectQuery selectQuery = getSelectQuery();

        selectQuery.setWhereClause(new WhereClause(Criteria.in(getSyncSetingsTable().getPKColumn(), syncIds)));

        return adminDataStore().get(selectQuery);
    }

    /**
     * <p>
     * Constructs and returns a {@link SelectQuery} for retrieving email address–related data.
     * </p>
     *
     * @return a {@link SelectQuery} configured with the base table and necessary joins
     */
    private SelectQuery getSelectQuery() {
        final Table emailAddressTable = getAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addJoinClause(getJoinClause());

        return selectQuery;
    }
}
