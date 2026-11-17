package com.tlc.crm.core.notification.email.settings.api;

import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.email.notification.AbstractNotificationManager;

import com.tlc.crm.security.sql.resource.OMCRMUSER;

import com.tlc.crm.core.api.contact.manager.LifeCycleStageManager;

import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSettings;
import com.tlc.crm.email.notification.email.api.model.settings.EmailSignature;
import com.tlc.crm.email.notification.email.api.model.settings.EmailTrackerSettings;
import com.tlc.crm.email.notification.email.api.model.address.UserBlockedEmailAddress;
import com.tlc.crm.email.notification.sql.resource.OMEMBLOCKEDADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMUSEREMAILSETTINGS;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Implements the email settings related DB actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailSettingsManager extends AbstractNotificationManager<EmailSettings> {

    private final LifeCycleStageManager lifeCycleStageManager;
    private final LifeCycleStatusManager lifeCycleStatusManager;

    private static class Instance {
        private static final EmailSettingsManager INSTANCE = new EmailSettingsManager();
    }

    private EmailSettingsManager() {
        lifeCycleStageManager = LifeCycleStageManager.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
    }

    /**
     * <p>
     * Gets the {@link EmailSettingsManager} Instance
     * </p>
     *
     * @return {@link EmailSettingsManager}
     */
    public static EmailSettingsManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the email settings table
     * </p>
     *
     * @return the email settings table
     */
    @Override
    public Table getTable() {
        return Table.get(OMEMUSEREMAILSETTINGS.TABLE);
    }

    /**
     * <p>
     * Gets the blocked address table
     * </p>
     *
     * @return the blocked address table
     */
    private Table getBlockedAddressTable() {
        return Table.get(OMEMBLOCKEDADDRESS.TABLE);
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link EmailSettings}
     * </p>
     *
     * @param row Represents the email settings {@link Row}
     * @return The {@link EmailSettings}
     */
    @Override
    public EmailSettings convertRowToModel(final Row row) {
        final EmailSettings emailSettings = new EmailSettings();
        final String emailSignatureValue = row.get(OMEMUSEREMAILSETTINGS.EMAIL_SIGNATURE);
        final Long orgId = row.getOrgId();
        final Long userId = row.get(OMEMUSEREMAILSETTINGS.USER_ID);

        if (Objects.nonNull(emailSignatureValue)) {
            final EmailSignature emailSignature = new EmailSignature();

            emailSignature.setOrgId(orgId);
            emailSignature.setUserId(userId);
            emailSignature.setEmailSignature(emailSignatureValue);
            emailSettings.setEmailSignature(emailSignature);
        }
        emailSettings.setId(row.getPKValue());
        emailSettings.setOrgId(orgId);
        emailSettings.setUserId(userId);
        emailSettings.setEmailLinkerSettings(new EmailLinkerSettings((String) row.get(OMEMUSEREMAILSETTINGS.EMAIL_LINKER_SETTINGS)));
        emailSettings.setEmailTrackerSettings(new EmailTrackerSettings((String) row.get(OMEMUSEREMAILSETTINGS.EMAIL_TRACKER_SETTINGS)));

        return emailSettings;
    }

    /**
     * <p>
     * Updates the email settings
     * </p>
     *
     * @param emailSettings Represents the {@link EmailSettings}
     */
    public void updateEmailSettings(final EmailSettings emailSettings) {
        final Long orgId = emailSettings.orgId();
        final Long userId = emailSettings.getUserId();
        final EmailSignature emailSignature = emailSettings.getEmailSignature();
        final EmailLinkerSettings emailLinkerSettings = emailSettings.getEmailLinkerSettings();
        final EmailTrackerSettings emailTrackerSettings = emailSettings.getEmailTrackerSettings();
        final Table emailSettingsTable = getTable();
        final SelectQuery selectQuery = buildSelectQueryToFetchEmailSettings(userId);

        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();
        final Row settingsRow = sourceContainer.getRow(emailSettingsTable);
        final Row emailSettingsRow = Objects.isNull(settingsRow) ? new Row(emailSettingsTable) : settingsRow;

        if (emailSettingsRow.isNewRow()) {
            emailSettingsRow.setOrgId(orgId);
            emailSettingsRow.set(OMEMUSEREMAILSETTINGS.USER_ID, userId);
        }

        if (Objects.nonNull(emailSignature)) {
            emailSettingsRow.set(OMEMUSEREMAILSETTINGS.EMAIL_SIGNATURE, emailSignature.getEmailSignature());
        } else {
            emailSettingsRow.set(OMEMUSEREMAILSETTINGS.EMAIL_SIGNATURE, null);
        }

        if (Objects.nonNull(emailLinkerSettings)) {
            emailSettingsRow.set(OMEMUSEREMAILSETTINGS.EMAIL_LINKER_SETTINGS, emailLinkerSettings.getLinkingSettingsJson());
        }

        if (Objects.nonNull(emailTrackerSettings)) {
            emailSettingsRow.set(OMEMUSEREMAILSETTINGS.EMAIL_TRACKER_SETTINGS, emailTrackerSettings.getTrackingSettingsJson());
        }
        targetContainer.updateRow(emailSettingsRow);

        loadBlockedAddressesIntoContainer(orgId, userId, emailSettings.getUserBlockedEmailAddresses(), sourceContainer, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Builds the select query to fetch email settings
     * </p>
     *
     * @param userId Represents a user id
     * @return Entity of{@link SelectQuery}
     */
    private SelectQuery buildSelectQueryToFetchEmailSettings(final long userId) {
        final Table emailSettingsTable = getTable();
        final Table blockedAddressTable = getBlockedAddressTable();
        final Table userTable = Table.get(OMCRMUSER.TABLE);
        final Column userTablePkColumn = userTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(userTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(
                new JoinClause(userTablePkColumn, emailSettingsTable.getColumn(OMEMUSEREMAILSETTINGS.USER_ID)),
                new JoinClause(userTablePkColumn, blockedAddressTable.getColumn(OMEMBLOCKEDADDRESS.USER_ID), leftJoin)));
        selectQuery.addSelectClause(blockedAddressTable, emailSettingsTable);
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userTablePkColumn, userId)));

        return selectQuery;
    }

    /**
     * <p>
     * Loads the blocked address into the container.
     * </p>
     *
     * @param orgId                     Represents an organization id.
     * @param userId                    Represents a user id.
     * @param userBlockedEmailAddresses Which contains the collection of user block email address
     * @param sourceContainer           Holds the user defined and system field values of existing source
     * @param targetContainer           Holds the updated information of user defined and system field values
     */
    private void loadBlockedAddressesIntoContainer(final long orgId, final long userId, final Collection<UserBlockedEmailAddress> userBlockedEmailAddresses,
                                                   final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<Long> ids = new HashSet<>();

        if (Objects.nonNull(userBlockedEmailAddresses) && !userBlockedEmailAddresses.isEmpty()) {
            final Table blockedAddressTable = getBlockedAddressTable();

            for (final UserBlockedEmailAddress userBlockedEmailAddress : userBlockedEmailAddresses) {
                final String address = userBlockedEmailAddress.getEmailId();
                final Row row = sourceContainer.getRow(blockedAddressTable, new WhereClause(
                        Criteria.eq(blockedAddressTable.getColumn(OMEMBLOCKEDADDRESS.EMAIL_ADDRESS), address))
                        .and(Criteria.eq(blockedAddressTable.getColumn(OMEMBLOCKEDADDRESS.USER_ID), userId)));
                final Row blockedAddressRow = Objects.isNull(row) ? new Row(blockedAddressTable) : row;

                if (blockedAddressRow.isNewRow()) {
                    blockedAddressRow.set(OMEMBLOCKEDADDRESS.USER_ID, userId);
                    blockedAddressRow.setOrgId(orgId);
                }
                blockedAddressRow.set(OMEMBLOCKEDADDRESS.EMAIL_ADDRESS, address);

                ids.add(blockedAddressRow.getPKValue());
                targetContainer.updateRow(blockedAddressRow);
            }
        }
        deleteUnmappedBlockedAddresses(userId, ids, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Deletes the unmapped blocked addresses.
     * </p>
     *
     * @param userId          Represents a user id.
     * @param ids             Represents the collection of ids of newly created blocked address.
     * @param sourceContainer Holds the user defined and system field values of existing source
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void deleteUnmappedBlockedAddresses(final long userId, final Collection<Long> ids,
                                                final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table blockedAddressTable = getBlockedAddressTable();
        final WhereClause condition = new WhereClause(Criteria.eq(blockedAddressTable.getColumn(OMEMBLOCKEDADDRESS.USER_ID), userId));
        final WhereClause whereClause = ids.isEmpty() ? condition : condition.and(Criteria.notIn(blockedAddressTable.getPKColumn(), ids));
        final Stream<Row> blockedAddressRows = sourceContainer.getRows(blockedAddressTable, whereClause);

        blockedAddressRows.forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Fetches the email settings based on organization and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents a user id
     * @return the email settings.
     */
    public EmailSettings retrieveEmailSettings(final long orgId, final long userId) {
        final Table emailSettingsTable = getTable();
        final Table blockedAddressTable = getBlockedAddressTable();
        final SelectQuery selectQuery = buildSelectQueryToFetchEmailSettings(userId);

        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);
        final Row emailSettingsRow = sourceContainer.getRow(emailSettingsTable);
        final EmailSettings emailSettings;

        if (Objects.nonNull(emailSettingsRow)) {
            emailSettings = convertRowToModel(emailSettingsRow);
        } else {
            emailSettings = new EmailSettings();
            final EmailTrackerSettings emailTrackerSettings = new EmailTrackerSettings();
            final EmailLinkerSettings emailLinkerSettings = new EmailLinkerSettings();
            final Long defaultLifeCycleStageId = lifeCycleStageManager.getDefaultStageId(orgId);

            emailTrackerSettings.setEmailTrackerEnabled(false);
            emailTrackerSettings.setLinkTrackerEnabled(false);

            emailLinkerSettings.setLifecycleStageId(defaultLifeCycleStageId);
            emailLinkerSettings.setLifecycleStatusId(lifeCycleStatusManager.getDefaultStatusId(orgId, defaultLifeCycleStageId));
            emailLinkerSettings.setContactLinkingEnabled(true);
            emailLinkerSettings.setContactCreationEnabled(true);
            emailLinkerSettings.setDealLinkingEnabled(true);
            emailLinkerSettings.setContactLinkingEnabled(true);

            emailSettings.setEmailTrackerSettings(emailTrackerSettings);
            emailSettings.setEmailLinkerSettings(emailLinkerSettings);
            emailSettings.setOrgId(orgId);
            emailSettings.setUserId(userId);
        }
        final Stream<Row> blockedAddressRows = sourceContainer.getRows(blockedAddressTable);

        emailSettings.setUserBlockedEmailAddresses(blockedAddressRows.map(this::convertRowToBlockedEmailAddress).collect(Collectors.toSet()));

        return emailSettings;
    }

    /**
     * <p>
     * Gets the blocked {@link UserBlockedEmailAddress} for the given org id, user id
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @return The {@link UserBlockedEmailAddress}
     */
    public Collection<UserBlockedEmailAddress> getBlockedAddresses(final long orgId, final long userId) {
        final Table blockedAddressTable = getBlockedAddressTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(blockedAddressTable,
                new WhereClause(Criteria.eq(blockedAddressTable.getColumn(OMEMBLOCKEDADDRESS.USER_ID), userId)));

        return dataContainer.isEmpty() ? Collections.emptySet() :
                dataContainer.getRows(blockedAddressTable).map(this::convertRowToBlockedEmailAddress).collect(Collectors.toSet());
    }

    /**
     * <p>
     * Converts the {@link Row} into {@link UserBlockedEmailAddress}
     * </p>
     *
     * @param row Represents the email address {@link Row}
     * @return The {@link UserBlockedEmailAddress}
     */
    public UserBlockedEmailAddress convertRowToBlockedEmailAddress(final Row row) {
        final UserBlockedEmailAddress userBlockedEmailAddress = new UserBlockedEmailAddress(row.get(OMEMBLOCKEDADDRESS.EMAIL_ADDRESS));

        userBlockedEmailAddress.setId(row.getPKValue());
        userBlockedEmailAddress.setOrgId(row.getOrgId());
        userBlockedEmailAddress.setUserId(row.get(OMEMBLOCKEDADDRESS.USER_ID));

        return userBlockedEmailAddress;
    }

    /**
     * <p>
     * Checks whether the contact linking is enabled or not.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return true, if the contact linking is enabled; otherwise false
     */
    public boolean isContactLinkingEnabled(final long orgId, final long userId) {
        final EmailSettings emailSettings = retrieveEmailSettings(orgId, userId);

        return emailSettings.getEmailLinkerSettings().isContactLinkingEnabled();
    }

    /**
     * <p>
     * Migrates every user's stored email linker lifecycle reference (the "Auto create person with
     * stage ... from emails" selection) off a stage/status that is being deleted, onto the given
     * default stage/status. This mirrors {@code LifeCycleMigrationHandler}'s handling of contacts
     * and lifecycle rules, so that a deleted stage/status does not leave the email settings dropdown
     * pointing at a non-existent id (which otherwise renders empty instead of falling back to default).
     * </p>
     *
     * @param orgId              Represents the organisation id.
     * @param stageIdsToMigrate  Holds the lifecycle stage ids being removed; pass {@link Collections#emptySet()} if a stage removal is not in progress.
     * @param statusIdsToMigrate Holds the lifecycle status ids being removed; pass {@link Collections#emptySet()} if a status removal is not in progress.
     * @param defaultStageId     Represents the default lifecycle stage id to fall back to.
     * @param defaultStatusId    Represents the default lifecycle status id to fall back to.
     */
    public void migrateEmailLinkerLifecycleReference(final long orgId, final Collection<Long> stageIdsToMigrate,
                                                     final Collection<Long> statusIdsToMigrate,
                                                     final Long defaultStageId, final Long defaultStatusId) {
        final Table emailSettingsTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(emailSettingsTable);
        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        sourceContainer.getRows(emailSettingsTable).forEach(row -> {
            final String linkerSettingsJson = row.get(OMEMUSEREMAILSETTINGS.EMAIL_LINKER_SETTINGS);

            if (Objects.nonNull(linkerSettingsJson)) {
                final EmailLinkerSettings emailLinkerSettings = new EmailLinkerSettings(linkerSettingsJson);
                final boolean stageWasRemoved = stageIdsToMigrate.contains(emailLinkerSettings.getLifecycleStageId());
                final boolean statusWasRemoved = statusIdsToMigrate.contains(emailLinkerSettings.getLifecycleStatusId());

                if (stageWasRemoved || statusWasRemoved) {
                    emailLinkerSettings.setLifecycleStageId(defaultStageId);
                    emailLinkerSettings.setLifecycleStatusId(defaultStatusId);
                    row.set(OMEMUSEREMAILSETTINGS.EMAIL_LINKER_SETTINGS, emailLinkerSettings.getLinkingSettingsJson());
                    targetContainer.updateRow(row);
                }
            }
        });

        if (!targetContainer.isEmpty()) {
            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }
}