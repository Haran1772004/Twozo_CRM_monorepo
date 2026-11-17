package com.tlc.crm.email.notification.email.sync.api;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.pair.Pair;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.model.UserSyncAddressInfo;

import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAILCONVERSATIONS;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.email.server.service.EmailProviderService;
import com.tlc.email.server.sql.resource.MEMSYNCEDEMAILCONVERSATIONS;

import com.tlc.crm.email.notification.email.api.manager.MailManager;
import com.tlc.crm.email.notification.email.api.model.config.EmailFolderSyncDateConfig;
import com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig;
import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.email.status.EmailErrorCodes;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMFROMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAIL;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.server.sql.resource.MEMADDRESS;
import com.tlc.email.server.sql.resource.MEMSYNCSETTINGS;

import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.notification.model.sync.ConnectionStatus;

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
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.AbstractMap.SimpleEntry;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo.Type.SYNC_EMAIL;

/**
 * <p>
 * Implements the {@link EmailSyncSettings} related DB actions
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class EmailSyncManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSyncManager.class);
    private static final String COLON = StringUtil.Delimiter.COLON.getDelimiter();

    private final MailManager mailManager;

    private static class Instance {
        private static final EmailSyncManager INSTANCE = new EmailSyncManager();
    }

    private EmailSyncManager() {
        mailManager = MailManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link EmailSyncManager} Instance
     * </p>
     *
     * @return {@link EmailSyncManager}
     */
    public static EmailSyncManager getInstance() {
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
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link ReadableDataStore}
     * </p>
     *
     * @return {@link ReadableDataStore}
     */
    private ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * <p>
     * Gets the SyncedEmail table
     * </p>
     *
     * @return {@link Table}
     */
    public Table getTable() {
        return Table.get(OMEMSYNCEDEMAIL.TABLE);
    }

    /**
     * <p>
     * Gets the sync email table
     * </p>
     *
     * @return {@link Table}
     */
    public Table getEmailAddressTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the email table
     * </p>
     *
     * @return {@link Table}
     */
    private Table getEmailTable() {
        return Table.get(OMETEMAIL.TABLE);
    }

    /**
     * <p>
     * Creates action to synchronize an email
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param emails Collection of emails
     * @return {@link Map}
     */
    public Map<String, Object> create(final Long orgId, final Collection<EmailSyncSettings> emails) {
        final Map<String, Object> response = new HashMap<>();
        final List<Long> createdRecords = new ArrayList<>();
        final List<Map<String, Object>> failedEntries = new ArrayList<>();

        emails.forEach(syncSettings -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadEmailAddressIntoContainer(orgId, syncSettings, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                createdRecords.add(syncSettings.id());
            } catch (ErrorCode errorCode) {
                final Map<String, Object> failedEntry = new HashMap<>();

                failedEntry.put(EmailFields.REASON.getName(), errorCode.getMessage());
                failedEntries.add(failedEntry);
                throw ErrorCode.get(EmailErrorCodes.CREATE_ACTION_FAILED);
            }
        });

        response.put(EmailFields.CREATED_RECORDS.getName(), createdRecords);
        response.put(EmailFields.FAILED_RECORDS.getName(), failedEntries);

        return response;
    }

    /**
     * <p>
     * Update action for email sync
     * </p>
     *
     * @param orgId  Represents an Organization id
     * @param emails Contains the collection of {@link EmailSyncSettings}
     * @return {@link Map} response of update action
     */
    public Map<String, Object> update(final Long orgId, final Collection<EmailSyncSettings> emails) {
        final Map<String, Object> response = new HashMap<>();
        final List<Long> updatedRecords = new ArrayList<>();
        final List<Map<String, Object>> failedEntries = new ArrayList<>();

        emails.forEach(syncSettings -> {
            try {
                final DataContainer targetContainer = DataContainer.create();
                final Long id = syncSettings.id();

                loadEmailAddressIntoContainer(orgId, syncSettings, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                updatedRecords.add(id);
            } catch (ErrorCode errorCode) {
                final Map<String, Object> failedEntry = new HashMap<>();

                failedEntry.put(EmailFields.REASON.getName(), errorCode.getMessage());
                failedEntries.add(failedEntry);
            }
        });

        response.put(EmailFields.UPDATED_RECORDS.getName(), updatedRecords);
        response.put(EmailFields.FAILED_RECORDS.getName(), failedEntries);

        return response;
    }

    /**
     * <p>
     * Fetches the {@link EmailSyncSettings} by sync id and user id within the given organization.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @param syncId Represents the synchronization email id
     * @return {@link EmailSyncSettings} or null when not found
     */
    public EmailSyncSettings get(final long orgId, final long userId, final long syncId) {
        final Table syncTable = getTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(syncTable);

        selectQuery.addSelectClause(syncTable, emailTable);
        selectQuery.addJoinClause(new JoinClause(syncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(syncTable.getPKColumn(), syncId))
                .and(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : convertRowToModel(dataContainer.getRow(syncTable), dataContainer);
    }

    /**
     * <p>
     * Retrieves all sync ids for the given organization and user.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return {@link Collection} of sync ids
     */
    public Collection<Long> getSyncIds(final Long orgId, final Long userId) {
        final Table table = getTable();

        return orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId)))
                .getRows(table).map(Row::getPKValue).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves partial {@link EmailSyncSettings} for the given email address.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link Collection} of {@link EmailSyncSettings}
     */
    public Collection<EmailSyncSettings> getPartialSettings(final String emailAddress) {
        final Table table = getTable();
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table, emailAddressTable);
        selectQuery.addJoinClause(new JoinClause(table.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailAddressTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress))
                .and(Criteria.eq(table.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId())));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(table).map(row -> {
            final EmailSyncSettings syncSettings = new EmailSyncSettings();

            syncSettings.setOrgId(row.getOrgId());
            syncSettings.setUserId(row.get(OMEMSYNCEDEMAIL.USER_ID));
            syncSettings.setId(row.getPKValue());

            return syncSettings;
        }).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Retrieves partial {@link EmailSyncSettings} for the given email addresses.
     * </p>
     *
     * @param emailAddresses Represents the email addresses
     * @return {@link Map} of email address and its partial settings
     */
    public Map<String, Collection<EmailSyncSettings>> getPartialSettings(final Collection<String> emailAddresses) {
        return getPartialSettings(null, emailAddresses);
    }

    /**
     * <p>
     * Retrieves partial {@link EmailSyncSettings} for the given organization and email addresses.
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param emailAddresses Represents the email addresses
     * @return {@link Map} of email address and its partial settings
     */
    public Map<String, Collection<EmailSyncSettings>> getPartialSettings(final Long orgId, final Collection<String> emailAddresses) {
        if (Objects.isNull(emailAddresses) || emailAddresses.isEmpty()) {
            return Collections.emptyMap();
        }
        final Table table = getTable();
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column emailAddressColumn = emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS);
        final WhereClause whereClause = new WhereClause(Criteria.in(emailAddressColumn, emailAddresses))
                .and(Criteria.eq(table.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId()));

        if (Objects.nonNull(orgId)) {
            whereClause.and(Criteria.eq(table.getColumn(OMEMSYNCEDEMAIL.ORG_ID), orgId));
        }

        selectQuery.addSelectClause(table, emailAddressTable);
        selectQuery.addJoinClause(new JoinClause(table.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailAddressTable.getPKColumn()));
        selectQuery.setWhereClause(whereClause);
        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Long, String> emailAddressById = new HashMap<>();

        dataContainer.getRows(emailAddressTable).forEach(row -> emailAddressById.put(row.getPKValue(), row.get(emailAddressColumn)));
        final Map<String, Collection<EmailSyncSettings>> syncSettingsByEmailAddress = new HashMap<>();

        dataContainer.getRows(table).forEach(row -> {
            final Long emailId = row.get(OMEMSYNCEDEMAIL.EMAIL_ID);
            final String emailAddress = emailAddressById.get(emailId);

            if (Objects.nonNull(emailAddress)) {
                final EmailSyncSettings syncSettings = new EmailSyncSettings();

                syncSettings.setOrgId(row.getOrgId());
                syncSettings.setUserId(row.get(OMEMSYNCEDEMAIL.USER_ID));
                syncSettings.setId(row.getPKValue());
                syncSettingsByEmailAddress.computeIfAbsent(emailAddress, key -> new HashSet<>()).add(syncSettings);
            }
        });

        return syncSettingsByEmailAddress;
    }

    /**
     * <p>
     * Retrieves a collection of email addresses related to organization
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the collection of {@link EmailAddress} the related to organization
     */
    public Collection<EmailAddress> getEmailAddressesByOrgId(final Long orgId) {
        final Table emailTable = getEmailTable();

        return orgDataStore(orgId).get(SelectQuery.get(emailTable)).getRows(emailTable).map(row ->
                new EmailAddress(row.get(OMETEMAIL.VALUE).toString())).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Gets or initializes the email id for the given email address within the organization.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param emailAddress Represents the email address
     * @return the email id
     */
    public Long getOrInitEmailId(final Long orgId, final String emailAddress) {
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addSelectClause(emailAddressTable.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row row = dataContainer.getRow(emailAddressTable);

        if (Objects.nonNull(row)) {
            return row.getPKValue();
        }
        final DataContainer targetContainer = DataContainer.create();
        final Row newRow = new Row(emailAddressTable);

        newRow.set(OMEMADDRESS.EMAIL_ADDRESS, emailAddress);
        newRow.set(OMEMADDRESS.ORG_ID, orgId);

        targetContainer.addNewRow(newRow);

        orgDataStore(orgId).commitChanges(targetContainer);

        return newRow.getPKValue();
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
        final Table emailTable = mailManager.getEmailAddressTable();
        final CountQuery countQuery = CountQuery.get(syncTable);

        countQuery.addJoinClause(new JoinClause(syncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailTable.getPKColumn()));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress.getEmailAddress()))
                .and(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks whether synchronization is enabled for a specific user within the given organization.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return true if email is sync enable given organization id and user id, false otherwise
     */
    public boolean isEmailSyncEnabled(final Long orgId, final Long userId) {
        final Table syncTable = getTable();
        final CountQuery countQuery = CountQuery.get(syncTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId)).
                and(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED), true))
                .and(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks if a default synced email setting exists for the given user within the
     * specified organization. Queries the datastore to verify the presence of a record
     * with the matching user ID and default flag set to true.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id to check for a default setting
     * @return {@code true} if a default setting exists for the user, {@code false} otherwise
     */
    public boolean hasDefaultSetting(final Long orgId, final Long userId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMEMSYNCEDEMAIL.IS_DEFAULT), true)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Temporary backfill to load provider and compute default synced email for all organizations.
     * </p>
     *
     * @return number of synced email rows updated across organizations
     */
    public int loadProviderAndIsDefault() {
        final Table syncTable = getTable();
        final Collection<String> processedUsers = new HashSet<>();

        int totalRows = NumericalConstant.ZERO;
        int totalUpdatedRows = NumericalConstant.ZERO;
        int batchNo = NumericalConstant.ZERO;
        int start = NumericalConstant.ONE;

        while (true) {
            final SelectQuery selectQuery = SelectQuery.get(syncTable);

            selectQuery.addSelectClause(List.of(
                    syncTable.getColumn(OMEMSYNCEDEMAIL.ORG_ID),
                    syncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID))
            );
            selectQuery.addOrderByClause(new OrderByClause(syncTable.getPKColumn()));
            selectQuery.setLimitClause(new LimitClause(start, NumericalConstant.ONE_HUNDRED));
            final List<Row> batchRows = readableDataStore().get(selectQuery).getRows(syncTable).toList();

            if (batchRows.isEmpty()) {
                break;
            }
            totalRows += batchRows.size();
            batchNo++;
            final Collection<Map.Entry<Long, Long>> usersToProcess = new HashSet<>();

            batchRows.forEach(row -> {
                final Long orgId = row.getOrgId();
                final Long userId = row.get(OMEMSYNCEDEMAIL.USER_ID);

                if (Objects.nonNull(orgId) && Objects.nonNull(userId)) {
                    final String key = orgId + COLON + userId;

                    if (processedUsers.add(key)) {
                        usersToProcess.add(new SimpleEntry<>(orgId, userId));
                    }
                }
            });
            int batchUpdatedRows = NumericalConstant.ZERO;

            for (final Map.Entry<Long, Long> userKey : usersToProcess) {
                batchUpdatedRows += loadProviderAndIsDefaultForUser(userKey.getKey(), userKey.getValue());
            }
            totalUpdatedRows += batchUpdatedRows;
            LOGGER.debug("Sync Email Data Load: processed batch {} (rows {}-{}, users {}, updatedRows {})",
                    batchNo, start, start + batchRows.size() - NumericalConstant.ONE, usersToProcess.size(), batchUpdatedRows);

            start += NumericalConstant.ONE_HUNDRED;
        }

        if (batchNo == NumericalConstant.ZERO) {
            LOGGER.debug("Sync Email Data Load: no synced email rows found");
        }
        LOGGER.debug("Sync Email Data Load: totalRows {}, totalUpdatedRows {}, processedUsers {}",
                totalRows, totalUpdatedRows, processedUsers.size());

        return totalUpdatedRows;
    }

    /**
     * <p>
     * Temporary backfill to load provider and compute default synced email for a user.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return number of synced email rows updated for the user
     */
    private int loadProviderAndIsDefaultForUser(final Long orgId, final Long userId) {
        final Table syncTable = getTable();
        final Table emailTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(syncTable);

        selectQuery.addSelectClause(syncTable, emailTable);
        selectQuery.addJoinClause(new JoinClause(syncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailTable.getPKColumn()));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId)));
        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);

        if (sourceContainer.isEmpty()) {
            return NumericalConstant.ZERO;
        }
        final List<Row> syncRows = sourceContainer.getRows(syncTable).toList();
        final Map<Long, String> emailAddressMap = new HashMap<>();
        sourceContainer.getRows(emailTable).forEach(emailRow ->
                emailAddressMap.put(emailRow.getPKValue(), emailRow.get(OMEMADDRESS.EMAIL_ADDRESS))
        );

        final DataContainer targetContainer = DataContainer.create();
        Row defaultRow;

        if (syncRows.size() == NumericalConstant.ONE) {
            defaultRow = syncRows.get(NumericalConstant.ZERO);
        } else {
            final List<Row> eligibleRows = syncRows.stream().filter(this::isEligibleForDefault).toList();

            defaultRow = getLatestByLastSyncTime(eligibleRows.isEmpty() ? syncRows : eligibleRows);
        }

        syncRows.forEach(syncRow -> {
            final Long emailId = syncRow.get(OMEMSYNCEDEMAIL.EMAIL_ID);
            final String emailAddress = emailAddressMap.get(emailId);

            try {
                if (Objects.nonNull(emailAddress)) {
                    syncRow.set(OMEMSYNCEDEMAIL.PROVIDER, EmailProviderService.getProvider(new EmailAddress(emailAddress)).getId());
                }
                final boolean isDefault = Objects.nonNull(defaultRow) && Objects.equals(defaultRow.getPKValue(), syncRow.getPKValue())
                        && ((short) syncRow.get(OMEMSYNCEDEMAIL.SYNC_STATUS)) == SyncStatus.SYNCED.getId();

                syncRow.set(OMEMSYNCEDEMAIL.IS_DEFAULT, isDefault);
                targetContainer.updateRow(syncRow);
            } catch (final Exception e) {
                LOGGER.debug("Email Provider Load: Failed for Email Address : {} of User Id: {} in Org Id: {}. Error: {}", emailAddress, userId, orgId, e.getMessage());
            };
        });
        orgDataStore(orgId).commitChanges(targetContainer);

        return syncRows.size();
    }

    /**
     * <p>
     * Retrieves the user ids associated with synced emails for a given organizations
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param emailIds which holds the {@link Collection} of email ids
     * @return The {@link Collection} of user ids
     */
    public Collection<Long> getSyncedUserIds(final long orgId, final Collection<Long> emailIds) {
        final Table syncTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(syncTable);
        final Column syncUserIdColumn = syncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID);

        selectQuery.addSelectClause(syncUserIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(syncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailIds))
                .and(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId())));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? Collections.emptySet() : dataContainer.getRows(syncTable)
                .map(row -> (long) row.get(syncUserIdColumn)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Retrieves the email synchronized account settings for a user within a specific organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return {@link EmailSyncSettings} that holds synchronization email attributes
     */
    public EmailSyncSettings fetchEmailSyncSetting(final Long orgId, final Long userId) {
        final Table syncTable = getTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailTable);

        selectQuery.addJoinClause(new JoinClause(emailTable.getPKColumn(), syncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID)));
        selectQuery.addSelectClause(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS));
        selectQuery.addSelectClause(syncTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId))
                .and(Criteria.eq(syncTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : convertRowToModel(dataContainer.getRow(syncTable), dataContainer);
    }

    /**
     * <p>
     * Fetches the sync email address
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return the {@link UserSyncAddressInfo}
     */
    public Optional<UserSyncAddressInfo> getSyncEmailAddress(final Long orgId, final Long userId) {
        final Table emailSyncTable = getTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailSyncTable);
        final Column emailSyncTablePKColumn = emailSyncTable.getPKColumn();

        selectQuery.addJoinClause(new JoinClause(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailTable.getPKColumn()));
        selectQuery.addSelectClause(List.of(emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailSyncTablePKColumn));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId))
                .and(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED), true))
                .and(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId())));
        final Row emailRow = orgDataStore(orgId).get(selectQuery).getRow(emailTable);

        if (Objects.isNull(emailRow)) {
            return Optional.empty();
        } else {
            final UserSyncAddressInfo emailAddress = new UserSyncAddressInfo(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS));

            emailAddress.setCrmEmailId(emailRow.getPKValue());

            return Optional.of(emailAddress);
        }
    }

    /**
     * <p>
     * Fetches the sender information for the provided org id and user id,
     * using the default synced email address.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return {@link Pair} of the from-address id and the associated {@link UserSyncAddressInfo}
     */
    public Pair<Long, UserSyncAddressInfo> getSenderInfo(final Long orgId, final Long userId) {
        return getSenderInfo(orgId, userId, true, null);
    }

    /**
     * <p>
     * Fetches the sender information for the provided org id and user id,
     * with control over whether a default address is required and an optional
     * conversation context for resolving the synced email address.
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param userId          Represents the user id
     * @param requiredDefault Represents whether a default synced email address is required
     * @param conversationId  Represents the conversation id used to resolve the synced email address,
     *                        or {@code null} if no conversation context is needed
     * @return {@link Pair} of the from-address id and the associated {@link UserSyncAddressInfo}
     * @throws ErrorCode if no synced email address is found for the given user
     */
    public Pair<Long, UserSyncAddressInfo> getSenderInfo(final Long orgId, final Long userId, final boolean requiredDefault,
                                                         final Long conversationId) {
        final DataContainer dataContainer = getSyncedEmailData(orgId, userId, requiredDefault, conversationId);
        final Table emailAddressTable = mailManager.getEmailAddressTable();
        final Row emailRow = dataContainer.getRow(emailAddressTable);

        if (Objects.isNull(emailRow)) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_NOT_SYNCED);
        }
        final FromAddressInfo emailAddress = new FromAddressInfo(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS));

        emailAddress.setCrmEmailId(emailRow.getPKValue());

        return Pair.of(dataContainer.getRow(mailManager.getFromAddressTable()).getPKValue(), emailAddress);
    }

    /**
     * <p>
     * Fetches the sender information for the provided org id and user id,
     * resolved against the given conversation context without requiring a default
     * synced email address.
     * </p>
     *
     * @param orgId          Represents the organization id
     * @param userId         Represents the user id
     * @param conversationId Represents the conversation id used to resolve the synced email address
     * @return {@link Pair} of the from-address id and the associated {@link UserSyncAddressInfo}
     */
    public Pair<Long, UserSyncAddressInfo> getSenderInfo(final Long orgId, final Long userId, final Long conversationId) {
        return getSenderInfo(orgId, userId, false, conversationId);
    }

    /**
     * <p>
     * Loads synchronized email data into a target container
     * </p>
     *
     * @param syncJobType     Represents the sync job type
     * @param syncSettings    Represents the {@link EmailSyncSettings} that holds synchronization email attributes
     * @param targetContainer Holds the updated information of email defined
     */
    public void loadSyncedEmailIntoContainer(final SyncJobType syncJobType, final EmailSyncSettings syncSettings, final DataContainer targetContainer) {
        final Long orgId = syncSettings.orgId();
        final Long syncId = syncSettings.id();
        final Long userId = syncSettings.getUserId();
        final Table emailSyncTable = getTable();
        final EmailFolderSyncDateConfig emailFolderSyncDateConfig = syncSettings.getEmailFolderSyncDateConfig();
        final UpdateQuery updateQuery = UpdateQuery.get(emailSyncTable);
        final long lastSyncTime = Instant.now().toEpochMilli();

        updateQuery.addUpdateColumn(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.LAST_SYNC_TIME), lastSyncTime);
        updateQuery.addUpdateColumn(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED), true);
        updateQuery.addUpdateColumn(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.SYNC_DATE_CONFIG),
                Objects.isNull(emailFolderSyncDateConfig) ? null : emailFolderSyncDateConfig.configAsJson());

        if (!syncJobType.isAutoSync()) {
            updateQuery.addUpdateColumn(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.SYNC_SETTING_CONFIG),
                    syncSettings.getEmailSyncConfig().configAsJson());
        }
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(emailSyncTable.getPKColumn(), syncId))
                .and(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId)));

        orgDataStore(orgId).commitChanges(targetContainer);
        orgDataStore(orgId).update(List.of(updateQuery));

        syncSettings.setLastSyncTime(lastSyncTime);
    }

    /**
     * <p>
     * Fetches the synced email data
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param requiredDefault Represents whether a default synced email address is required
     * @param conversationId  Represents the conversation id used to resolve the synced email address,
     *                        or {@code null} if no conversation context is needed
     * @return {@link DataContainer}
     */
    private DataContainer getSyncedEmailData(final Long orgId, final Long userId, final boolean requiredDefault,
                                             final Long conversationId) {
        final Table emailSyncTable = getTable();
        final Table emailTable = mailManager.getEmailAddressTable();
        final Table fromAddressTable = mailManager.getFromAddressTable();
        final Column emailIdColumn = emailTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(emailSyncTable);
        final List<JoinClause> joinClauses = new ArrayList<>();
        WhereClause whereClause = new WhereClause(
                Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED), true))
                .and(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId))
                .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.IS_ACTIVE), true))
                .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE), SYNC_EMAIL.getId()));

        joinClauses.add(new JoinClause(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailIdColumn));
        joinClauses.add(new JoinClause(emailIdColumn, fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID)));

        if (Objects.nonNull(conversationId)) {
            final Table syncEmailConversationTable = mailManager.getSyncEmailConversationTable();

            joinClauses.add(new JoinClause(emailSyncTable.getPKColumn(),
                    syncEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID)));

            whereClause = whereClause.and(Criteria.eq(syncEmailConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID), conversationId));
            selectQuery.addSelectClause(syncEmailConversationTable);
        } else {
            whereClause = whereClause.and(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.IS_DEFAULT), requiredDefault));
        }
        selectQuery.addJoinClause(joinClauses);
        selectQuery.addSelectClause(List.of(
                emailTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailSyncTable.getPKColumn(), fromAddressTable.getPKColumn()
        ));
        selectQuery.setWhereClause(whereClause);

        return orgDataStore(orgId).get(selectQuery);
    }

    /**
     * <p>
     * Returns whether the row satisfies the temporary default eligibility rule.
     * </p>
     *
     * @param syncRow Represents the synced email row
     * @return true when row can be considered for default, false otherwise
     */
    private boolean isEligibleForDefault(final Row syncRow) {
        final Number syncStatus = syncRow.get(OMEMSYNCEDEMAIL.SYNC_STATUS);
        final Number connectionStatus = syncRow.get(OMEMSYNCEDEMAIL.CONNECTION_STATUS);

        return Objects.nonNull(syncStatus) && syncStatus.shortValue() == SyncStatus.SYNCED.getId()
                && Objects.nonNull(connectionStatus) && connectionStatus.shortValue() == ConnectionStatus.ACTIVE.getId()
                && Boolean.TRUE.equals(syncRow.get(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED));
    }

    /**
     * <p>
     * Picks the latest sync row by LAST_SYNC_TIME and falls back to id for deterministic ordering.
     * </p>
     *
     * @param syncRows Represents sync rows of a user
     * @return latest row by last sync time
     */
    private Row getLatestByLastSyncTime(final Collection<Row> syncRows) {
        return syncRows.stream().max((left, right) -> {
            final long leftTime = getTimeValue(left.get(OMEMSYNCEDEMAIL.LAST_SYNC_TIME));
            final long rightTime = getTimeValue(right.get(OMEMSYNCEDEMAIL.LAST_SYNC_TIME));

            if (leftTime != rightTime) {
                return Long.compare(leftTime, rightTime);
            }

            return Long.compare(left.getPKValue(), right.getPKValue());
        }).orElse(null);
    }

    /**
     * <p>
     * Safely retrieves the time value from the given object,
     * returning Long.MIN_VALUE if the value is not a Number.
     * </p>
     *
     * @param value Represents the object to retrieve the time value from
     * @return the time value as a long, or Long.MIN_VALUE if the value is not a Number
     */
    private long getTimeValue(final Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.MIN_VALUE;
    }

    /**
     * <p>
     * Converts the row to {@link EmailSyncSettings}
     * </p>
     *
     * @param row           Represents the selected row from the sync table that holds the synchronization data
     * @param dataContainer Represents the {@link DataContainer}
     * @return {@link EmailSyncSettings} that holds synchronization email attributes
     */
    private EmailSyncSettings convertRowToModel(final Row row, final DataContainer dataContainer) {
        final Table emailTable = mailManager.getEmailAddressTable();
        final Row emailRow = dataContainer.getRow(emailTable, new WhereClause(Criteria.eq(emailTable.getPKColumn(),
                row.get(OMEMSYNCEDEMAIL.EMAIL_ID))));
        final EmailSyncSettings syncSettings = new EmailSyncSettings();
        final UserSyncAddressInfo address = new UserSyncAddressInfo(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS));

        address.setCrmEmailId(emailRow.getPKValue());

        syncSettings.setId(row.getPKValue());
        syncSettings.setOrgId(row.getOrgId());
        syncSettings.setUserId(row.get(OMEMSYNCEDEMAIL.USER_ID));
        syncSettings.setSynced(row.get(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED));
        syncSettings.setAddress(address);
        syncSettings.setConnectionStatus(ConnectionStatus.get(row.get(OMEMSYNCEDEMAIL.CONNECTION_STATUS)));
        syncSettings.setLastSyncTime(row.get(OMEMSYNCEDEMAIL.LAST_SYNC_TIME));
        syncSettings.setSyncStatus(SyncStatus.getStatus(row.get(OMEMSYNCEDEMAIL.SYNC_STATUS)));
        syncSettings.setEmailProvider(EmailProvider.valueOf((short) row.get(OMEMSYNCEDEMAIL.PROVIDER)));
        syncSettings.setDefault(row.get(OMEMSYNCEDEMAIL.IS_DEFAULT));
        final String inProgressConfig = row.get(OMEMSYNCEDEMAIL.IN_PROGRESS_SETTING_CONFIG);

        if (Objects.nonNull(inProgressConfig)) {
            syncSettings.setInProgressSyncConfig(new EmailSyncConfig(inProgressConfig));
        }
        syncSettings.setEmailSyncConfig(new EmailSyncConfig((String) row.get(OMEMSYNCEDEMAIL.SYNC_SETTING_CONFIG)));
        final String syncDateConfigRow = row.get(OMEMSYNCEDEMAIL.SYNC_DATE_CONFIG);

        if (Objects.isNull(syncDateConfigRow)) {
            syncSettings.setSyncInterval(EmailSyncSettings.SyncInterval.LAST_ONE_MONTH);
        } else {
            syncSettings.setEmailFolderSyncDateConfig(new EmailFolderSyncDateConfig(syncDateConfigRow));
        }

        return syncSettings;
    }

    /**
     * <p>
     * Loads email address
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param syncSettings    An entity that holds email sync settings attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadEmailAddressIntoContainer(final Long orgId, final EmailSyncSettings syncSettings, final DataContainer targetContainer) {
        final Table emailSyncTable = getTable();
        final UserSyncAddressInfo emailAddress = (UserSyncAddressInfo) syncSettings.getAddress();
        final Long userId = syncSettings.getUserId();

        syncSettings.setOrgId(orgId);
        loadEmailAddressIntoContainer(orgId, emailAddress, targetContainer);
        final Long emailId = emailAddress.getCrmEmailId();

        final DataContainer dataContainer = orgDataStore(orgId).get(emailSyncTable,
                new WhereClause(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailId))
                        .and(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId)));

        loadSyncDataIntoContainer(emailId, syncSettings, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the email address
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param emailAddress    An entity that holds email attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadEmailAddressIntoContainer(final Long orgId, final UserSyncAddressInfo emailAddress,
                                               final DataContainer targetContainer) {
        final Table emailAddressTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress.getEmailAddress())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        loadEmailIntoContainer(orgId, emailAddress, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the email into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param address         An entity that holds email attributes
     * @param sourceContainer Holds the user defined values of existing mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadEmailIntoContainer(final Long orgId, final UserSyncAddressInfo address,
                                        final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table emailAddressTable = getEmailAddressTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), address.getEmailAddress()));
        final Row sourceRow = sourceContainer.getRow(emailAddressTable, whereClause);
        final Row existingRow = Objects.isNull(sourceRow) ? targetContainer.getRow(emailAddressTable, whereClause) : sourceRow;
        final Row emailRow = Objects.isNull(existingRow) ? new Row(emailAddressTable) : existingRow;

        if (emailRow.isNewRow()) {
            emailRow.set(OMEMADDRESS.EMAIL_ADDRESS, address.getEmailAddress());
            emailRow.setOrgId(orgId);

            targetContainer.addNewRow(emailRow);
        }
        address.setCrmEmailId(emailRow.getPKValue());
    }

    /**
     * <p>
     * Loads the email address into the target container
     * </p>
     *
     * @param syncSettings    An entity that holds email sync settings attributes
     * @param sourceContainer Holds the user defined values of existing sync action
     * @param targetContainer Holds the updated information of field values provided by user
     */
    private void loadSyncDataIntoContainer(final Long emailId, final EmailSyncSettings syncSettings,
                                           final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table emailSyncTable = getTable();
        final Long orgId = syncSettings.orgId();
        final Long userId = syncSettings.getUserId();
        final WhereClause whereClause = new WhereClause(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId))
                .and(Criteria.eq(emailSyncTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), emailId));
        final Row sourceRow = sourceContainer.getRow(emailSyncTable, whereClause);
        final Row existingRow = Objects.isNull(sourceRow) ? targetContainer.getRow(emailSyncTable, whereClause) : sourceRow;
        final Row syncRow = Objects.isNull(sourceRow) ? new Row(emailSyncTable) : existingRow;
        final Long syncRowPkColumn = syncRow.getPKValue();
        final UserSyncAddressInfo syncAddressInfo = (UserSyncAddressInfo) syncSettings.getAddress();

        if (syncRow.isNewRow()) {
            syncRow.setOrgId(orgId);
            syncRow.set(OMEMSYNCEDEMAIL.EMAIL_ID, syncAddressInfo.getCrmEmailId());
            syncRow.set(OMEMSYNCEDEMAIL.PROVIDER, syncSettings.getEmailProvider().getId());
        }
        syncRow.set(OMEMSYNCEDEMAIL.SYNC_SETTING_CONFIG, syncSettings.getEmailSyncConfig().configAsJson());
        final EmailSyncConfig emailSyncConfig = syncSettings.getInProgressSyncConfig();

        syncRow.set(OMEMSYNCEDEMAIL.IN_PROGRESS_SETTING_CONFIG, Objects.isNull(emailSyncConfig) ? null : emailSyncConfig.configAsJson());
        final EmailFolderSyncDateConfig emailFolderSyncDateConfig = syncSettings.getEmailFolderSyncDateConfig();

        syncRow.set(OMEMSYNCEDEMAIL.SYNC_DATE_CONFIG, Objects.isNull(emailFolderSyncDateConfig) ? null : emailFolderSyncDateConfig.configAsJson());
        syncRow.set(OMEMSYNCEDEMAIL.SYNC_STATUS, syncSettings.getSyncStatus().getId());
        syncRow.set(OMEMSYNCEDEMAIL.USER_ID, userId);
        syncRow.set(OMEMSYNCEDEMAIL.CONNECTION_STATUS, syncSettings.getConnectionStatus().getId());
        syncRow.set(OMEMSYNCEDEMAIL.IS_SYNC_ENABLED, syncSettings.isSynced());
        syncRow.set(OMEMSYNCEDEMAIL.IS_DEFAULT, syncSettings.isDefault());
        syncRow.set(OMEMSYNCEDEMAIL.LAST_SYNC_TIME, syncSettings.getLastSyncTime());

        if (Objects.nonNull(syncAddressInfo)) {
            syncAddressInfo.setCrmEmailId(syncRow.get(OMEMSYNCEDEMAIL.EMAIL_ID));
        }
        syncSettings.setId(syncRowPkColumn);
        targetContainer.updateRow(syncRow);
    }

    /**
     * <p>
     * Temporary backfill to load synced email conversations from email-server MEMSyncedEmailConversations.
     * Maps server-side conversations with their corresponding synced email IDs and populates OMEMSyncedEmailConversations.
     * Optimized for batch processing with pre-fetched email address mapping and batch inserts.
     * </p>
     *
     * @return number of conversations loaded into OMEMSyncedEmailConversations
     */
    public int loadSyncedEmailConversations() {
        final Table syncConversationsTable = Table.get(MEMSYNCEDEMAILCONVERSATIONS.TABLE);
        final Map<Long, List<Pair<Long, Long>>> emailSyncInfos = preloadEmailSyncSettingInfos();

        int totalRows = NumericalConstant.ZERO;
        int totalInsertedRows = NumericalConstant.ZERO;
        int batchNo = NumericalConstant.ZERO;
        int start = NumericalConstant.ONE;

        while (true) {
            final SelectQuery selectQuery = SelectQuery.get(syncConversationsTable);

            selectQuery.addOrderByClause(new OrderByClause(syncConversationsTable.getPKColumn()));
            selectQuery.setLimitClause(new LimitClause(start, NumericalConstant.THOUSAND));
            final List<Row> batchRows = readableDataStore().get(selectQuery).getRows(syncConversationsTable).toList();

            if (batchRows.isEmpty()) {
                break;
            }
            totalRows += batchRows.size();
            batchNo++;
            final Map<String, Map.Entry<Long, Long>> syncConversations = new LinkedHashMap<>();

            batchRows.forEach(row -> {
                final Long syncEmailId = row.get(MEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID);
                final Long conversationId = row.get(MEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID);
                final String key = syncEmailId + COLON + conversationId;

                syncConversations.putIfAbsent(key, new SimpleEntry<>(syncEmailId, conversationId));
            });
            int batchInsertedRows = loadConversationsBatch(syncConversations.values(), emailSyncInfos);

            totalInsertedRows += batchInsertedRows;
            LOGGER.debug("Synced Email Conversations Load: processed batch {} (rows {}-{}, syncs {}, insertedRows {})",
                    batchNo, start, start + batchRows.size() - NumericalConstant.ONE, syncConversations.size(), batchInsertedRows);

            start += NumericalConstant.THOUSAND;
        }

        if (batchNo == NumericalConstant.ZERO) {
            LOGGER.debug("Synced Email Conversations Load: no conversations found in email-server");
        }
        LOGGER.debug("Synced Email Conversations Load: totalRows {}, totalInsertedRows {}",
                totalRows, totalInsertedRows);

        return totalInsertedRows;
    }

    /**
     * <p>
     * Pre-loads MEM sync settings to CRM synced-email mapping into memory.
     * For each email address, maps MEMSyncSettings ID to all CRM {orgId, syncedEmailId} records.
     * </p>
     *
     * @return Map of MEMSyncSettings ID to CRM synced-email records
     */
    private Map<Long, List<Pair<Long, Long>>> preloadEmailSyncSettingInfos() {
        final Table omemSyncedEmailTable = getTable();
        final Table omemAddressTable = getEmailAddressTable();
        final Column omemEmailAddressColumn = omemAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS);
        final SelectQuery syncedEmailQuery = SelectQuery.get(omemSyncedEmailTable);

        syncedEmailQuery.addSelectClause(List.of(
                omemSyncedEmailTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID),
                omemSyncedEmailTable.getColumn(OMEMSYNCEDEMAIL.ORG_ID),
                omemEmailAddressColumn
        ));
        syncedEmailQuery.addJoinClause(new JoinClause(
                omemSyncedEmailTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID), omemAddressTable.getPKColumn())
        );
        final DataContainer syncedEmailContainer = readableDataStore().get(syncedEmailQuery);
        final Map<String, List<Pair<Long, Long>>> crmSyncInfosByEmail = new HashMap<>();

        if (!syncedEmailContainer.isEmpty()) {
            final Map<Long, String> crmEmailAddressById = new HashMap<>();

            syncedEmailContainer.getRows(omemAddressTable).forEach(row -> {
                final String normalizedEmailAddress = normalizeEmailAddress(row.get(omemEmailAddressColumn));

                if (Objects.nonNull(normalizedEmailAddress)) {
                    crmEmailAddressById.put(row.getPKValue(), normalizedEmailAddress);
                }
            });

            syncedEmailContainer.getRows(omemSyncedEmailTable).forEach(syncedEmailRow -> {
                final String emailAddress = crmEmailAddressById.get((Long) syncedEmailRow.get(OMEMSYNCEDEMAIL.EMAIL_ID));

                if (Objects.nonNull(emailAddress)) {
                    crmSyncInfosByEmail.computeIfAbsent(emailAddress, key -> new ArrayList<>())
                            .add(Pair.of(syncedEmailRow.getOrgId(), syncedEmailRow.getPKValue()));
                }
            });
        }
        final Table memSyncSettingsTable = Table.get(MEMSYNCSETTINGS.TABLE);
        final Table memAddressTable = Table.get(MEMADDRESS.TABLE);
        final Column memEmailAddressColumn = memAddressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS);
        final SelectQuery syncSettingsQuery = SelectQuery.get(memSyncSettingsTable);

        syncSettingsQuery.addSelectClause(List.of(
                memSyncSettingsTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID),
                memEmailAddressColumn
        ));
        syncSettingsQuery.addJoinClause(new JoinClause(
                memSyncSettingsTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID), memAddressTable.getPKColumn())
        );
        final DataContainer syncSettingsContainer = readableDataStore().get(syncSettingsQuery);
        final Map<Long, List<Pair<Long, Long>>> emailSyncSettingInfos = new HashMap<>();

        if (syncSettingsContainer.isEmpty() || crmSyncInfosByEmail.isEmpty()) {
            return emailSyncSettingInfos;
        }
        final Map<Long, String> emailAddressById = new HashMap<>();

        syncSettingsContainer.getRows(memAddressTable).forEach(row -> {
            final String normalizedEmailAddress = normalizeEmailAddress(row.get(memEmailAddressColumn));

            if (Objects.nonNull(normalizedEmailAddress)) {
                emailAddressById.put(row.getPKValue(), normalizedEmailAddress);
            }
        });

        syncSettingsContainer.getRows(memSyncSettingsTable).forEach(memSyncRow -> {
            final String emailAddress = emailAddressById.get((Long) memSyncRow.get(MEMSYNCSETTINGS.EMAIL_ID));
            final List<Pair<Long, Long>> crmSyncInfos = crmSyncInfosByEmail.get(emailAddress);

            if (Objects.nonNull(crmSyncInfos) && !crmSyncInfos.isEmpty()) {
                emailSyncSettingInfos.put(memSyncRow.getPKValue(), crmSyncInfos);
            }
        });

        return emailSyncSettingInfos;
    }

    /**
     * <p>
     * Batch loads conversations for multiple sync-conversation pairs.
     * Groups by organization and performs batch inserts to reduce DB hits.
     * </p>
     *
     * @param syncConversations     Collection of sync-conversation pairs
     * @param emailSyncSettingInfos Preloaded CRM sync mappings from email address
     * @return number of conversations inserted
     */
    private int loadConversationsBatch(final Collection<Map.Entry<Long, Long>> syncConversations,
                                       final Map<Long, List<Pair<Long, Long>>> emailSyncSettingInfos) {
        final Map<Long, Map<Long, Set<Long>>> sourceConversationIdsByOrg = new HashMap<>();
        final Table syncedConversationTable = Table.get(OMEMSYNCEDEMAILCONVERSATIONS.TABLE);
        final Table conversationsTable = Table.get(OMEMCONVERSATIONS.TABLE);
        final Column syncedEmailIdColumn = syncedConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.SYNCED_EMAIL_ID);
        final Column conversationIdColumn = syncedConversationTable.getColumn(OMEMSYNCEDEMAILCONVERSATIONS.CONVERSATION_ID);
        final Column sourceConversationIdColumn = conversationsTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID);

        for (final Map.Entry<Long, Long> syncConversation : syncConversations) {
            final Long syncSettingsId = syncConversation.getKey();
            final Collection<Pair<Long, Long>> crmSyncInfos = Objects.isNull(syncSettingsId) ? null : emailSyncSettingInfos.get(syncSettingsId);

            if (Objects.isNull(crmSyncInfos) || crmSyncInfos.isEmpty()) {
                LOGGER.debug("Synced Email Conversations Load: no CRM sync mapping found for emailServerId: {}", syncSettingsId);
                continue;
            }
            final Long sourceConversationId = syncConversation.getValue();

            if (Objects.isNull(sourceConversationId)) {
                continue;
            }

            for (final Pair<Long, Long> crmSyncInfo : crmSyncInfos) {
                final Long orgId = crmSyncInfo.getKey();
                final Long syncedEmailId = crmSyncInfo.getValue();

                if (Objects.isNull(orgId) || Objects.isNull(syncedEmailId)) {
                    continue;
                }
                sourceConversationIdsByOrg.computeIfAbsent(orgId, key -> new HashMap<>())
                        .computeIfAbsent(syncedEmailId, key -> new HashSet<>()).add(sourceConversationId);
            }
        }
        int insertCount = NumericalConstant.ZERO;

        for (final Map.Entry<Long, Map<Long, Set<Long>>> orgEntry : sourceConversationIdsByOrg.entrySet()) {
            final Map<Long, Set<Long>> sourceConversationIdsBySyncedEmailId = orgEntry.getValue();

            if (sourceConversationIdsBySyncedEmailId.isEmpty()) {
                continue;
            }
            final Set<Long> sourceConversationIds = sourceConversationIdsBySyncedEmailId.values().stream()
                    .flatMap(Collection::stream).collect(Collectors.toSet());
            final Map<Long, Long> crmConversationPkBySourceId = new HashMap<>();

            if (!sourceConversationIds.isEmpty()) {
                final SelectQuery conversationQuery = SelectQuery.get(conversationsTable);

                conversationQuery.addSelectClause(List.of(sourceConversationIdColumn));
                conversationQuery.setWhereClause(new WhereClause(Criteria.in(sourceConversationIdColumn, sourceConversationIds)));
                final DataContainer conversationContainer = orgDataStore(orgEntry.getKey()).get(conversationQuery);

                conversationContainer.getRows(conversationsTable).forEach(row ->
                        crmConversationPkBySourceId.putIfAbsent(row.get(sourceConversationIdColumn), row.getPKValue())
                );
            }
            final Map<Long, Set<Long>> resolvedConversationIdsBySyncedEmailId = new HashMap<>();

            sourceConversationIdsBySyncedEmailId.forEach((syncedEmailId, sourceIds) ->
                    sourceIds.forEach(sourceId -> {
                        final Long crmConversationId = crmConversationPkBySourceId.get(sourceId);

                        if (Objects.nonNull(crmConversationId)) {
                            resolvedConversationIdsBySyncedEmailId.computeIfAbsent(syncedEmailId, key -> new HashSet<>()).add(crmConversationId);
                        }
                    })
            );

            if (resolvedConversationIdsBySyncedEmailId.isEmpty()) {
                continue;
            }
            final Set<Long> syncedEmailIds = resolvedConversationIdsBySyncedEmailId.keySet();
            final Set<Long> conversationIds = resolvedConversationIdsBySyncedEmailId.values().stream()
                    .flatMap(Collection::stream).collect(Collectors.toSet());
            final Map<Long, Set<Long>> existingConversationIdsBySyncedEmailId = new HashMap<>();

            if (!conversationIds.isEmpty()) {
                final SelectQuery existingQuery = SelectQuery.get(syncedConversationTable);

                existingQuery.addSelectClause(List.of(syncedEmailIdColumn, conversationIdColumn));
                existingQuery.setWhereClause(new WhereClause(
                        Criteria.in(syncedEmailIdColumn, syncedEmailIds))
                        .and(Criteria.in(conversationIdColumn, conversationIds))
                );
                final DataContainer existingContainer = orgDataStore(orgEntry.getKey()).get(existingQuery);

                existingContainer.getRows(syncedConversationTable).forEach(row ->
                        existingConversationIdsBySyncedEmailId.computeIfAbsent(
                                row.get(syncedEmailIdColumn), key -> new HashSet<>()).add(row.get(conversationIdColumn))
                );
            }
            final DataContainer targetContainer = DataContainer.create();

            resolvedConversationIdsBySyncedEmailId.forEach((syncedEmailId, crmConversationIds) ->
                    crmConversationIds.forEach(crmConversationId -> {
                        final Set<Long> existingConversationIds = existingConversationIdsBySyncedEmailId.get(syncedEmailId);

                        if (Objects.nonNull(existingConversationIds) && existingConversationIds.contains(crmConversationId)) {
                            return;
                        }

                        final Row row = new Row(syncedConversationTable);

                        row.setOrgId(orgEntry.getKey());
                        row.set(syncedEmailIdColumn, syncedEmailId);
                        row.set(conversationIdColumn, crmConversationId);
                        targetContainer.addNewRow(row);
                    })
            );

            if (!targetContainer.isEmpty()) {
                orgDataStore(orgEntry.getKey()).commitChanges(targetContainer);
                insertCount += (int) targetContainer.getRows(syncedConversationTable).count();
            }
        }

        return insertCount;
    }


    /**
     * <p>
     * Normalizes the email address by trimming whitespace and converting to lowercase.
     * Returns null if the input email address is null.
     * </p>
     *
     * @param emailAddress the email address to normalize
     * @return the normalized email address, or null if the input was null
     */
    private String normalizeEmailAddress(final String emailAddress) {
        return Objects.isNull(emailAddress) ? null : emailAddress.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * <p>
     * Activates or deactivates the sync email from addresses of the users within the given organization.
     * The addresses to be deactivated are updated directly in a batch, whereas the addresses to be
     * activated are updated when they already exist and created otherwise.
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param enableByUser  Represents the email ids to be activated, grouped by user id
     * @param disableByUser Represents the email ids to be deactivated, grouped by user id
     */
    public void updateFromAddressStatus(final long orgId, final Map<Long, Collection<Long>> enableByUser,
                                        final Map<Long, Collection<Long>> disableByUser) {
        if (enableByUser.isEmpty() && disableByUser.isEmpty()) {
            return;
        }
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final Table fromAddressTable = mailManager.getFromAddressTable();

        if (!disableByUser.isEmpty()) {
            orgDataStore.update(getFromAddressUpdateQueries(fromAddressTable, disableByUser));
        }

        if (!enableByUser.isEmpty()) {
            enableFromAddresses(orgId, orgDataStore, fromAddressTable, enableByUser);
        }

        LOGGER.debug("Email sync from address status updated in batch for orgId: {}", orgId);
    }

    /**
     * <p>
     * Activates the existing from addresses and creates the missing ones for the given users.
     * Addresses which are already active are left untouched.
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param orgDataStore     Represents the {@link OrgDataStore} of the organization
     * @param fromAddressTable Represents the from address table
     * @param enableByUser     Represents the email ids to be activated, grouped by user id
     */
    private void enableFromAddresses(final long orgId, final OrgDataStore orgDataStore, final Table fromAddressTable,
                                     final Map<Long, Collection<Long>> enableByUser) {
        final DataContainer existingContainer = getFromAddressContainer(orgDataStore, fromAddressTable, enableByUser);
        final DataContainer targetContainer = DataContainer.create();
        final Map<Long, Collection<Long>> missingByUser = new HashMap<>();

        enableByUser.forEach((userId, emailIds) -> missingByUser.put(userId, new HashSet<>(emailIds)));

        existingContainer.getRows(fromAddressTable).forEach(row -> {
            final Collection<Long> missingEmailIds = missingByUser.get((Long) row.get(OMEMFROMADDRESS.USER_ID));

            if (Objects.nonNull(missingEmailIds)) {
                missingEmailIds.remove((Long) row.get(OMEMFROMADDRESS.EMAIL_ID));
            }

            if (!Boolean.TRUE.equals(row.get(OMEMFROMADDRESS.IS_ACTIVE))) {
                row.set(OMEMFROMADDRESS.IS_ACTIVE, true);

                targetContainer.updateRow(row);
            }
        });

        missingByUser.forEach((userId, emailIds) -> emailIds.forEach(emailId -> {
            final Row fromAddressRow = new Row(fromAddressTable);

            fromAddressRow.setOrgId(orgId);
            fromAddressRow.set(OMEMFROMADDRESS.USER_ID, userId);
            fromAddressRow.set(OMEMFROMADDRESS.EMAIL_ID, emailId);
            fromAddressRow.set(OMEMFROMADDRESS.TYPE, SYNC_EMAIL.getId());
            fromAddressRow.set(OMEMFROMADDRESS.IS_ACTIVE, true);

            targetContainer.addNewRow(fromAddressRow);
        }));

        if (!targetContainer.isEmpty()) {
            orgDataStore.commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Fetches the existing sync email from address rows for the given users and email ids.
     * </p>
     *
     * @param orgDataStore     Represents the {@link OrgDataStore} of the organization
     * @param fromAddressTable Represents the from address table
     * @param emailIdsByUser   Represents the email ids grouped by user id
     * @return {@link DataContainer} that holds the matching from address rows
     */
    private DataContainer getFromAddressContainer(final OrgDataStore orgDataStore, final Table fromAddressTable,
                                                  final Map<Long, Collection<Long>> emailIdsByUser) {
        WhereClause userClause = null;

        for (final Map.Entry<Long, Collection<Long>> entry : emailIdsByUser.entrySet()) {

            if (entry.getValue().isEmpty()) {
                continue;
            }
            final WhereClause clause = new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.USER_ID), entry.getKey()))
                    .and(Criteria.in(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), entry.getValue()));

            userClause = Objects.isNull(userClause) ? clause : userClause.or(clause);
        }

        if (Objects.isNull(userClause)) {
            return DataContainer.create();
        }
        final SelectQuery selectQuery = SelectQuery.get(fromAddressTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE), SYNC_EMAIL.getId()))
                .and(userClause));

        return orgDataStore.get(selectQuery);
    }

    /**
     * <p>
     * Builds the queries that deactivate the sync email from addresses of the given users.
     * </p>
     *
     * @param fromAddressTable Represents the from address table
     * @param emailIdsByUser   Represents the email ids grouped by user id
     * @return {@link List} of {@link UpdateQuery}
     */
    private List<UpdateQuery> getFromAddressUpdateQueries(final Table fromAddressTable,
                                                          final Map<Long, Collection<Long>> emailIdsByUser) {
        final List<UpdateQuery> updateQueries = new ArrayList<>();

        emailIdsByUser.forEach((userId, emailIds) -> {

            if (Objects.nonNull(emailIds) && !emailIds.isEmpty()) {
                final UpdateQuery updateQuery = UpdateQuery.get(fromAddressTable);

                updateQuery.addUpdateColumn(fromAddressTable.getColumn(OMEMFROMADDRESS.IS_ACTIVE), false);
                updateQuery.setWhereClause(new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.USER_ID), userId))
                        .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE), SYNC_EMAIL.getId()))
                        .and(Criteria.in(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), emailIds)));

                updateQueries.add(updateQuery);
            }
        });

        return updateQueries;
    }
}