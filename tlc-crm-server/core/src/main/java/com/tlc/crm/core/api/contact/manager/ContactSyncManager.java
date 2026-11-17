package com.tlc.crm.core.api.contact.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.ContactSourceDetails;
import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.core.features.api.displayPicture.DisplayPictureManager;
import com.tlc.crm.core.internal.contact.status.ContactErrorCodes;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.OMCOCONTACTSYNCSETTINGS;
import com.tlc.crm.core.sql.resource.OMCOEXTERNALPERSON;
import com.tlc.crm.core.sql.resource.OMCOSYNCEDCONTACT;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.entity.models.displayPicture.SourceDisplayPicture;
import com.tlc.commons.page.Page;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.filter.date.DateIntervals;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;
import com.tlc.notification.model.TokenType;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.notification.service.contact.ContactTokenService;
import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;

import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.Collections;

import static com.tlc.crm.core.api.contact.model.Contact.OriginSource.PROVIDER;
import static com.tlc.crm.core.api.contact.model.Contact.OriginSource.TWOZO;

/**
 * <p>
 * Implements the contact sync related DB actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class ContactSyncManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ContactSyncManager.class);

    private static final int DEFAULT_LIMIT = 20;

    private final ContactTokenService contactTokenService;
    private final DisplayPictureManager displayPictureManager;
    private final DateTimeUtil dateTimeUtil;

    private ContactSyncManager() {
        contactTokenService = ContactTokenService.getInstance();
        displayPictureManager = DisplayPictureManager.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final ContactSyncManager INSTANCE = new ContactSyncManager();
    }

    /**
     * <p>
     * Gets the {@link ContactSyncManager} Instance
     * </p>
     *
     * @return {@link ContactSyncManager}
     */
    public static ContactSyncManager getInstance() {
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
     * Gets the {@link AdminDataStore}
     * </p>
     *
     * @return {@link AdminDataStore}
     */
    private ReadableDataStore readableDataStore() {
        return DataStoreProvider.getReadableDataStore();
    }

    /**
     * <p>
     * Gets the contact sync settings table
     * </p>
     *
     * @return the {@link Table}
     */
    private Table getTable() {
        return Table.get(OMCOCONTACTSYNCSETTINGS.TABLE);
    }

    /**
     * <p>
     * Gets the synced contact table
     * </p>
     *
     * @return the {@link Table}
     */
    private Table getSyncedContactTable() {
        return Table.get(OMCOSYNCEDCONTACT.TABLE);
    }

    /**
     * <p>
     * Gets the synced contact table
     * </p>
     *
     * @return the {@link Table}
     */
    private Table getExternalPersonTable() {
        return Table.get(OMCOEXTERNALPERSON.TABLE);
    }

    /**
     * <p>
     * Gets the synced contact table
     * </p>
     *
     * @return the {@link Table}
     */
    private Table getContactTable() {
        return ContactManager.getInstance().getTable();
    }

    /**
     * <p>
     * Fetches the total count that are present for the contact sync settings
     * </p>
     *
     * @return the total pages
     */
    public long getTotalCountForSyncSettings() {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED), true)));

        return readableDataStore().get(countQuery);
    }

    /**
     * <p>
     * Gets the total contacts that has to be synced
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the contact to be synced count
     */
    public long fetchContactCount(final long orgId, final long userId) {
        final Table externalPersonTable = getExternalPersonTable();
        final Table contactTable = getContactTable();
        final CountQuery countQuery = CountQuery.get(contactTable);

        countQuery.addJoinClause(new JoinClause(contactTable.getPKColumn(), externalPersonTable.getColumn(OMCOEXTERNALPERSON.CONTACT_ID), JoinClause.JoinType.LEFT));
        countQuery.setWhereClause(new WhereClause(Criteria.eq(externalPersonTable.getPKColumn(), null))
                .and(Criteria.eq(contactTable.getColumn(MCOCONTACT.OWNER_ID), userId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the already existing contact by the contact label
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param externalPersonId Represents the external person id
     * @return the {@link Contact}
     */
    public Contact getContact(final long orgId, final String externalPersonId) {
        final Table externalPersonTable = getExternalPersonTable();
        final SelectQuery externalPersonSelectQuery = SelectQuery.get(externalPersonTable);

        externalPersonSelectQuery.setWhereClause(new WhereClause(Criteria.eq(externalPersonTable.getColumn(OMCOEXTERNALPERSON.EXTERNAL_PERSON_ID), externalPersonId)));
        final Row externalPersonRow = orgDataStore(orgId).get(externalPersonSelectQuery).getRow(externalPersonTable);

        if (Objects.isNull(externalPersonRow)) {
            return null;
        }
        final Contact contact = ContactManager.getInstance().get(orgId, (Long) externalPersonRow.get(OMCOEXTERNALPERSON.CONTACT_ID));

        Optional.ofNullable(contact).ifPresent(existingContact -> {
            Optional.ofNullable(existingContact.getPhones()).ifPresent(phones -> phones.forEach(phone -> phone.setCountry(null)));
            Optional.ofNullable(existingContact.getEmails()).ifPresent(emails -> emails.forEach(email -> email.setType(Email.Type.WORK)));
            Optional.ofNullable(existingContact.getAddress()).ifPresent(address -> {
                address.setLatitude(null);
                address.setLongitude(null);
            });
        });

        return contact;
    }

    /**
     * <p>
     * Gets the synced contact address
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the {@link EmailAddress}
     */
    public EmailAddress getSyncedAddress(final long orgId, final long userId) {
        final Table table = getTable();
        final SelectQuery externalPersonSelectQuery = SelectQuery.get(table);

        externalPersonSelectQuery.addSelectClause(table.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS));
        externalPersonSelectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED), true)));
        final DataContainer dataContainer = orgDataStore(orgId).get(externalPersonSelectQuery);

        return dataContainer.isEmpty() ? null : new EmailAddress(dataContainer.getRow(table).get(table.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS)));
    }

    /**
     * <p>
     * Gets the connected address used for contact sync
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the {@link EmailAddress}
     */
    public EmailAddress getConnectedAddress(final long orgId, final long userId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS), true)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.isEmpty() ? null : new EmailAddress(dataContainer.getRow(table).get(table.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS)));
    }

    /**
     * <p>
     * Gets the contact id from the container
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param externalPersonId Represents the external person id
     * @return the contact id
     */
    public Long getContactId(final long orgId, final String externalPersonId) {
        final Table externalPersonTable = getExternalPersonTable();
        final SelectQuery externalPersonQuery = SelectQuery.get(externalPersonTable);

        externalPersonQuery.setWhereClause(new WhereClause(Criteria.eq(
                externalPersonTable.getColumn(OMCOEXTERNALPERSON.EXTERNAL_PERSON_ID), externalPersonId)));
        final Row externalPersonRow = orgDataStore(orgId).get(externalPersonQuery).getRow(externalPersonTable);

        return Objects.isNull(externalPersonRow) ? null : externalPersonRow.get(OMCOEXTERNALPERSON.CONTACT_ID);
    }

    /**
     * <p>
     * Fetches the sync id.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the synced contact id
     */
    public Long getSyncId(final long orgId, final long userId) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED), true)));
        final DataContainer contactSyncContainer = orgDataStore(orgId).get(selectQuery);

        if (contactSyncContainer.isEmpty()) {
            return null;
        }
        final Row row = contactSyncContainer.getRow(table);

        return Objects.isNull(row) ? null : row.getPKValue();
    }

    /**
     * <p>
     * Updates the sync time whenever any sync happens
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param userId       Represents an owner id
     * @param emailAddress Represents the {@link EmailAddress}
     */
    public void updateLastSyncTime(final long orgId, final long userId, final EmailAddress emailAddress) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMCOCONTACTSYNCSETTINGS.LAST_SYNC_TIME), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS), emailAddress.getEmailId()))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED), true)));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Loads the synced contacted into container
     * </p>
     *
     * @param contact Represents the {@link Contact}.
     * @param syncId  Represents the synced id
     */
    public void loadSyncedContactRowIntoContainer(final Contact contact, final Long syncId, final DataContainer targetContainer) {
        final Table syncedContactTable = getSyncedContactTable();
        final Table externalPersonTable = getExternalPersonTable();
        final Long contactId = contact.id();
        final Long orgId = contact.orgId();
        final Long modifiedTime = dateTimeUtil.getTimeStamp(contact.getModifiedTime());
        final long currentTime = Instant.now().toEpochMilli();
        final long ownerId = contact.getOwnerId();
        final SelectQuery externalPersonQuery = SelectQuery.get(externalPersonTable);
        final int originSourceId = contact.getOriginSource().getId();

        externalPersonQuery.addSelectClause(syncedContactTable, externalPersonTable);
        externalPersonQuery.addJoinClause(new JoinClause(externalPersonTable.getPKColumn(), syncedContactTable.getColumn(OMCOSYNCEDCONTACT.EXTERNAL_PERSON_ID), JoinClause.JoinType.LEFT));
        externalPersonQuery.setWhereClause(new WhereClause(Criteria.eq(externalPersonTable.getColumn(OMCOEXTERNALPERSON.CONTACT_ID), contactId)));
        final DataContainer externalPersonContainer = orgDataStore(orgId).get(externalPersonQuery);
        final Row exteralPersonRow;
        final Row syncedContactRow;

        if (externalPersonContainer.isEmpty()) {
            syncedContactRow = new Row(syncedContactTable);
            exteralPersonRow = new Row(externalPersonTable);
        } else {
            final Row row = externalPersonContainer.getRow(syncedContactTable);

            syncedContactRow = Objects.isNull(row) ? new Row(syncedContactTable) : row;
            exteralPersonRow = externalPersonContainer.getRow(externalPersonTable);
        }

        if (syncedContactRow.isNewRow()) {
            syncedContactRow.setOrgId(orgId);
            syncedContactRow.set(OMCOSYNCEDCONTACT.OWNER_ID, ownerId);
            syncedContactRow.set(OMCOSYNCEDCONTACT.CREATED_TIME, currentTime);
            syncedContactRow.set(OMCOSYNCEDCONTACT.SYNC_ID, syncId);
            syncedContactRow.set(OMCOSYNCEDCONTACT.IS_ACTIVE, true);

            if (exteralPersonRow.isNewRow()) {
                exteralPersonRow.setOrgId(orgId);
                syncedContactRow.set(OMCOSYNCEDCONTACT.CREATION_ORIGIN, originSourceId);
                exteralPersonRow.set(OMCOEXTERNALPERSON.CONTACT_ID, contactId);
            } else {
                syncedContactRow.set(OMCOSYNCEDCONTACT.IS_MODIFIED, true);
                syncedContactRow.set(OMCOSYNCEDCONTACT.UPDATION_ORIGIN, originSourceId);
            }
        } else {
            syncedContactRow.set(OMCOSYNCEDCONTACT.IS_MODIFIED, true);
            syncedContactRow.set(OMCOSYNCEDCONTACT.UPDATION_ORIGIN, originSourceId);
        }
        syncedContactRow.set(OMCOSYNCEDCONTACT.MODIFIED_TIME, Objects.isNull(modifiedTime) ? currentTime : modifiedTime);
        syncedContactRow.set(OMCOSYNCEDCONTACT.EXTERNAL_PERSON_ID, exteralPersonRow.getPKValue());
        final String externalPersonId = contact.getExternalPersonId();

        if (Objects.nonNull(externalPersonId)) {
            exteralPersonRow.set(OMCOEXTERNALPERSON.EXTERNAL_PERSON_ID, externalPersonId);
        }
        final Consumer<DataContainer> containerLoader = (dataContainer) -> {
            final SourceDisplayPicture displayPicture = contact.getDisplayPicture();

            if (Objects.nonNull(displayPicture)) {
                displayPicture.setEntityId(contactId);
                displayPictureManager.saveDisplayPictureInfo(displayPicture, dataContainer);
            }
            dataContainer.updateRow(exteralPersonRow);
            dataContainer.updateRow(syncedContactRow);
        };

        if (Objects.isNull(targetContainer)) {
            final DataContainer dataContainer = DataContainer.create();

            containerLoader.accept(dataContainer);
            orgDataStore(orgId).commitChanges(dataContainer);
        } else {
            containerLoader.accept(targetContainer);
        }
    }

    /**
     * <p>
     * Fetches contact ids based on org and user's id.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     */
    public Collection<Long> fetchContactIds(final long orgId, final long userId, final int start) {
        final Table contactTable = getContactTable();
        final Table externalPersonTable = getExternalPersonTable();
        final SelectQuery innerSelectQuery = SelectQuery.get(externalPersonTable);
        final SelectQuery contactTableQuery = SelectQuery.get(contactTable);
        final Column contactTablePKColumn = contactTable.getPKColumn();

        innerSelectQuery.addSelectClause(externalPersonTable.getColumn(OMCOEXTERNALPERSON.CONTACT_ID));
        contactTableQuery.addSelectClause(contactTablePKColumn);
        contactTableQuery.setWhereClause(new WhereClause(Criteria.notIn(contactTablePKColumn, innerSelectQuery))
                .and(Criteria.eq(contactTable.getColumn(MCOCONTACT.OWNER_ID), userId)));
        contactTableQuery.addOrderByClause(new OrderByClause(contactTablePKColumn));
        contactTableQuery.setLimitClause(new LimitClause(start, DEFAULT_LIMIT));
        final DataContainer externalPersonContainer = orgDataStore(orgId).get(contactTableQuery);

        return externalPersonContainer.isEmpty() ? Collections.emptyList() : externalPersonContainer.getRows(contactTable).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Creates the contact sync settings entry
     * </p>
     *
     * @param orgId               Represents an organization id
     * @param contactSyncSettings Represents the {@link ContactSyncSettings}
     * @return the response for create action
     */
    public ProcessOutcome create(final long orgId, final Collection<ContactSyncSettings> contactSyncSettings) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();

        contactSyncSettings.forEach(syncSettings -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadSyncSettingsIntoContainer(syncSettings, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(syncSettings.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Create action: Contact sync create failed Org ID : {} reason : {}", orgId, errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(ContactErrorCodes.CREATE_ACTION_FAILED,
                        "i18n.common.create.failed")));
            }
        });

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(ContactErrorCodes.CREATE_ACTION_FAILED,
                    "i18n.common.create.failed")), failedEntries));
        }

        return processOutcome;
    }

    /**
     * <p>
     * Checks whether the connection is already enabled.
     * </p>
     *
     * @param orgId  Represents an organization id.
     * @param userId Represents the user id.
     * @return whether the connect status is enabled.
     */
    public boolean isAlreadyConnected(final long orgId, final long userId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS), true)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Checks whether any contact is synced for the user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return true, if exists
     */
    public boolean exists(final long orgId, final long userId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS), true)));
        return orgDataStore(orgId).get(countQuery) > 0;
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

        countQuery.setWhereClause(new WhereClause(Criteria.eq(syncTable.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS), emailAddress.getEmailId()))
                .and(Criteria.eq(syncTable.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED), true)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the external person ids from the container
     * </p>
     *
     * @param orgId Represents an organization id
     * @return the contact labels
     */
    public Collection<String> getExternalPersonIds(final long orgId, final long userId) {
        final Table externalPersonTable = getExternalPersonTable();
        final DataContainer dataContainer = fetchSyncedContactContainer(orgId, userId);

        return dataContainer.getRows(externalPersonTable)
                .map(row -> (String) row.get(OMCOEXTERNALPERSON.EXTERNAL_PERSON_ID)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Gets the contact labels for the given contact id
     * </p>
     *
     * @param orgId     Represents an organization id
     * @param contactId Represents the contact id
     * @return the contact label
     */
    public String getExternalPersonId(final long orgId, final long contactId) {
        final Table externalPersonTable = getExternalPersonTable();
        final SelectQuery selectQuery = SelectQuery.get(externalPersonTable);

        selectQuery.addSelectClause(externalPersonTable.getColumn(OMCOEXTERNALPERSON.EXTERNAL_PERSON_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(externalPersonTable.getColumn(OMCOEXTERNALPERSON.CONTACT_ID), contactId)));
        final Row row = orgDataStore(orgId).get(selectQuery).getRow(externalPersonTable);

        return Objects.isNull(row) ? null : row.get(OMCOEXTERNALPERSON.EXTERNAL_PERSON_ID);
    }

    /**
     * <p>
     * Checks whether the email is synced for the contact synchronization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an owner id
     * @return true, if contact is synced
     */
    public boolean isSyncEnabled(final long orgId, final long userId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED), true)));
        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Fetches the contact data container for the given organization and user
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return {@link DataContainer}
     */
    private DataContainer fetchSyncedContactContainer(final long orgId, final long userId) {
        final Table syncedContactTable = getSyncedContactTable();
        final SelectQuery syncedContactQuery = SelectQuery.get(syncedContactTable);

        syncedContactQuery.addJoinClause(new JoinClause(syncedContactTable.getColumn(OMCOSYNCEDCONTACT.EXTERNAL_PERSON_ID),
                getExternalPersonTable().getPKColumn()));
        syncedContactQuery.setWhereClause(new WhereClause(Criteria.eq(syncedContactTable.getColumn(OMCOSYNCEDCONTACT.OWNER_ID), userId)));

        return orgDataStore(orgId).get(syncedContactQuery);
    }

    /**
     * <p>
     * Checks whether the external person id exists.
     * </p>
     *
     * @param orgId            Represents an organization id
     * @param externalPersonId Represents the external person id
     * @return true, if exists
     */
    public boolean externalPersonIdExists(final long orgId, final String externalPersonId) {
        final Table externalPersonTable = getExternalPersonTable();
        final CountQuery countQuery = CountQuery.get(externalPersonTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(externalPersonTable.getColumn(OMCOEXTERNALPERSON.EXTERNAL_PERSON_ID), externalPersonId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Retrieves a page of current settings for contact synchronization
     * </p>
     *
     * @param start      Represents the starting index of the records in the page
     * @param limit      Represents the maximum number of records to retrieve in the page
     * @return the {@link Page<ContactSyncSettings>}
     */
    public Page<ContactSyncSettings> getCurrentSettingsPage(final int start, final int limit) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column isSyncEnabledColumn = table.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS), true))
                .and(Criteria.eq(isSyncEnabledColumn, true)));
        selectQuery.addSelectClause(List.of(table.getColumn(OMCOCONTACTSYNCSETTINGS.ORG_ID), table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID),
                table.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS), table.getColumn(OMCOCONTACTSYNCSETTINGS.SYNC_CONFIG), isSyncEnabledColumn));
        selectQuery.setLimitClause(new LimitClause(start, limit));
        selectQuery.addOrderByClause(new OrderByClause(table.getPKColumn()));
        final DataContainer dataContainer = readableDataStore().get(selectQuery);
        final Page<ContactSyncSettings> page = new Page<>(start);

        if (!dataContainer.isEmpty()) {
            page.setData(dataContainer.getRows(table).map(this::convertRowToModel).toList());
        }
        return page;
    }

    /**
     * <p>
     * Fetches the external person id for the respective contact id
     * </p>
     *
     * @param orgId      Represents an organization id
     * @param contactIds Represents the contact ids
     * @return the external person id with contact id
     */
    public Map<Long, String> fetchExternalPersonIds(final long orgId, final Collection<Long> contactIds) {
        final Table externalPersonTable = getExternalPersonTable();
        final SelectQuery selectQuery = SelectQuery.get(externalPersonTable);

        selectQuery.addSelectClause(List.of(externalPersonTable.getColumn(OMCOEXTERNALPERSON.EXTERNAL_PERSON_ID), externalPersonTable.getColumn(OMCOEXTERNALPERSON.CONTACT_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(externalPersonTable.getColumn(OMCOEXTERNALPERSON.CONTACT_ID), contactIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        if (dataContainer.isEmpty()) {
            return Collections.emptyMap();
        }

        return dataContainer.getRows(externalPersonTable).collect(Collectors.toMap(
                row -> row.get(OMCOEXTERNALPERSON.CONTACT_ID), row -> row.get(OMCOEXTERNALPERSON.EXTERNAL_PERSON_ID)));
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link ContactSyncSettings} model
     * </p>
     *
     * @param row Represents the {@link Row}
     * @return the {@link ContactSyncSettings}
     */
    private ContactSyncSettings convertRowToModel(final Row row) {
        final ContactSyncSettings contactSyncSettings = new ContactSyncSettings();
        final Long orgId = row.getOrgId();

        contactSyncSettings.setId(row.getPKValue());
        contactSyncSettings.setUserId(row.get(OMCOCONTACTSYNCSETTINGS.USER_ID));
        contactSyncSettings.setOrgId(orgId);
        contactSyncSettings.setAddress(new EmailAddress(orgId, row.get(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS)));
        contactSyncSettings.setConfig(row.get(OMCOCONTACTSYNCSETTINGS.SYNC_CONFIG));
        contactSyncSettings.setSyncStatus(row.get(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED));

        return contactSyncSettings;
    }

    /**
     * <p>
     * Loads the sync settings into the container
     * </p>
     *
     * @param syncSettings    Represents the {@link ContactSyncSettings}
     * @param targetContainer Represents the target {@link DataContainer}
     */
    private void loadSyncSettingsIntoContainer(final ContactSyncSettings syncSettings, final DataContainer targetContainer) {
        final Table table = getTable();
        final Long orgId = syncSettings.orgId();
        final String emailAddress = syncSettings.getAddress().getEmailId();
        final long userId = syncSettings.getUserId();
        final boolean isSynced = syncSettings.syncStatus();
        final Row syncRow;

        if (isSynced) {
            syncRow = orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS), emailAddress))
                    .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))).getRow(table);

            if (Objects.isNull(syncRow)) {
                throw ErrorCode.get(ContactErrorCodes.ENTITY_NOT_FOUND, "i18n.entity.not.found");
            }
        } else {
            syncRow = new Row(table);
        }
        final Long syncRowPkColumn = syncRow.getPKValue();

        if (syncRow.isNewRow()) {
            syncRow.setOrgId(orgId);
            syncRow.set(OMCOCONTACTSYNCSETTINGS.USER_ID, syncSettings.getUserId());
            syncRow.set(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS, syncSettings.getAddress().getEmailId());
            syncRow.set(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED, false);
        }
        syncRow.set(OMCOCONTACTSYNCSETTINGS.SYNC_STATUS, SyncStatus.NEW.getId());
        syncRow.set(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS, true);
        syncRow.set(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED, isSynced);
        syncRow.set(OMCOCONTACTSYNCSETTINGS.SYNC_CONFIG, syncSettings.getConfig().getConfigAsJson());
        syncSettings.setId(syncRowPkColumn);

        if (isSynced) {
            syncRow.set(OMCOCONTACTSYNCSETTINGS.LAST_SYNC_TIME, Instant.now().toEpochMilli());
        }
        targetContainer.updateRow(syncRow);
    }

    /**
     * <p>
     * Retrieves the {@link ContactSyncSettings} for a specific user within the given organization and contact synchronization id
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return The {@link ContactSyncSettings}
     */
    public Optional<ContactSyncSettings> getSettings(final long orgId, final long userId) {
        final Table contactSyncTable = getTable();
        final SelectQuery contactSyncSelectQuery = SelectQuery.get(contactSyncTable);

        contactSyncSelectQuery.addSelectClause(List.of(contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS),
                contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.SYNC_CONFIG),
                contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED),
                contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.LAST_SYNC_TIME), contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.IN_PROGRESS_CONFIG)));
        contactSyncSelectQuery.setWhereClause(new WhereClause(Criteria.eq(contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS), true)));

        final DataContainer contactSyncContainer = orgDataStore(orgId).get(contactSyncSelectQuery);
        final Row contactSyncRow = contactSyncContainer.getRow(contactSyncTable);

        if (Objects.nonNull(contactSyncRow)) {
            final ContactSyncSettings contactSyncSettings = new ContactSyncSettings();

            contactSyncSettings.setInProgressConfig(contactSyncRow.get(OMCOCONTACTSYNCSETTINGS.IN_PROGRESS_CONFIG));
            contactSyncSettings.setAddress(new EmailAddress(contactSyncRow.get(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS)));
            contactSyncSettings.setConfig(contactSyncRow.get(OMCOCONTACTSYNCSETTINGS.SYNC_CONFIG));
            contactSyncSettings.setSyncStatus(contactSyncRow.get(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED));
            contactSyncSettings.setLastSyncTime(contactSyncRow.get(OMCOCONTACTSYNCSETTINGS.LAST_SYNC_TIME));
            contactSyncSettings.setId(contactSyncRow.getPKValue());
            contactSyncSettings.setOrgId(orgId);
            contactSyncSettings.setUserId(userId);

            return Optional.of(contactSyncSettings);
        }

        return Optional.empty();
    }

    /**
     * <p>
     * Checks whether the sync id exists.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param syncId Represents the synced email id
     * @return if the synchronized email exists means return true, otherwise false
     */
    public boolean isSyncIdExists(final long orgId, final long syncId) {
        final Table contactSyncTable = getTable();
        final CountQuery contactSyncCountQuery = CountQuery.get(contactSyncTable);

        contactSyncCountQuery.setWhereClause(new WhereClause(Criteria.eq(contactSyncTable.getPKColumn(), syncId)));

        return orgDataStore(orgId).get(contactSyncCountQuery) > 0;
    }

    /**
     * <p>
     * Disables and terminates contact synchronization for a specific user and organization.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents a user id
     */
    public void stopSync(final long orgId, final long userId) {
        final EmailAddress emailAddress = getConnectedAddress(orgId, userId);
        final Table contactSyncSettingsTable = getTable();
        final Table contactSyncTable = getSyncedContactTable();
        final Column syncIdColumn = contactSyncTable.getColumn(OMCOSYNCEDCONTACT.SYNC_ID);
        final SelectQuery contactSyncSelectQuery = SelectQuery.get(contactSyncSettingsTable);
        final DataContainer targetContainer = DataContainer.create();

        contactSyncSelectQuery.addSelectClause(contactSyncSettingsTable);
        contactSyncSelectQuery.addSelectClause(List.of(contactSyncTable.getPKColumn(), syncIdColumn));
        contactSyncSelectQuery.addJoinClause(new JoinClause(contactSyncSettingsTable.getPKColumn(), syncIdColumn, JoinClause.JoinType.LEFT));
        contactSyncSelectQuery.setWhereClause(new WhereClause(Criteria.eq(contactSyncSettingsTable.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(contactSyncSettingsTable.getColumn(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS), true)));

        final DataContainer contactSyncContainer = orgDataStore(orgId).get(contactSyncSelectQuery);

        if (!contactSyncContainer.isEmpty()) {
            final Row row = contactSyncContainer.getRow(contactSyncSettingsTable);

            if (Objects.nonNull(row)) {
                updateSyncStatus(row, targetContainer);
                deleteMappedContact(row.getPKValue(), contactSyncContainer, targetContainer);

                orgDataStore(orgId).commitChanges(targetContainer);
            }
        }
        emailAddress.setOrgId(orgId);
        final String tokenValue = StringUtil.join(StringUtil.Delimiter.UNDER_SCORE, String.valueOf(orgId), emailAddress.getEmailId());

        contactTokenService.clearToken(tokenValue, TokenType.PAGE_TOKEN);
        contactTokenService.clearToken(tokenValue, TokenType.SYNC_TOKEN);
    }

    /**
     * <p>
     * Updates synchronization status for a contact sync setting in the specified target data container
     * </p>
     *
     * @param row             The row representing the contact sync setting to update
     * @param targetContainer The data container where the updated row will be stored
     */
    private void updateSyncStatus(final Row row, final DataContainer targetContainer) {
        final Table contactSyncSettingsTable = getTable();

        row.set(contactSyncSettingsTable.getColumn(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS), false);
        row.set(contactSyncSettingsTable.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED), false);
        row.set(contactSyncSettingsTable.getColumn(OMCOCONTACTSYNCSETTINGS.LAST_SYNC_TIME), null);
        row.set(contactSyncSettingsTable.getColumn(OMCOCONTACTSYNCSETTINGS.SYNC_STATUS), SyncStatus.NEW.getId());

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Deletes mapped contacts from the target data container based on the provided primary key value
     * </p>
     *
     * @param contactId       The primary key value used to identify the mapped contacts
     * @param sourceContainer The source data container where the mapping information is stored
     * @param targetContainer The target data container from which the mapped contacts will be deleted.
     */
    private void deleteMappedContact(final Long contactId, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table contactSyncTable = getSyncedContactTable();

        sourceContainer.getRows(contactSyncTable, new WhereClause(Criteria.eq(
                contactSyncTable.getColumn(OMCOSYNCEDCONTACT.SYNC_ID), contactId))).forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Retrieves the contact synchronization configuration for a specific user within the given organization
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return A String representing the contact synchronization configuration.
     */
    public ContactSyncSettings.Config getSyncConfig(final long orgId, final long userId) {
        final Table table = getTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(table,
                new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                        .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED), true)),
                List.of(table.getColumn(OMCOCONTACTSYNCSETTINGS.SYNC_CONFIG)));

        return new ContactSyncSettings.Config(dataContainer.getRow(table).get(OMCOCONTACTSYNCSETTINGS.SYNC_CONFIG));
    }

    /**
     * <p>
     * Retrieves detailed information about added, updated, or deleted records within a specified time range for a specific organization and user
     * </p>
     *
     * @param contactSyncSettings Represents {@link ContactSyncSettings}.
     * @param contactIds          Represents the contact ids
     * @param action              The action (added, updated, or deleted) to retrieve information for
     * @return The {@link ContactSourceDetails}
     */
    public ContactSourceDetails getSourceInfo(final ContactSyncSettings contactSyncSettings, final Collection<Long> contactIds, final Action action) {
        final Interval<Long, Long> dateInterval = DateIntervals.TODAY.getInterval();
        final long startTime = dateInterval.start();
        final long endTime = dateInterval.end();
        final Table syncContactTable = getSyncedContactTable();
        final Table externalPersonTable = getExternalPersonTable();
        final SelectQuery selectQuery = SelectQuery.get(syncContactTable);
        final Column originColumn = switch (action) {
            case CREATE -> syncContactTable.getColumn(OMCOSYNCEDCONTACT.CREATION_ORIGIN);
            case UPDATE -> syncContactTable.getColumn(OMCOSYNCEDCONTACT.UPDATION_ORIGIN);
            case DELETE -> syncContactTable.getColumn(OMCOSYNCEDCONTACT.DELETION_ORIGIN);
            default -> throw ErrorCode.get(ContactErrorCodes.INVALID_SOURCE, "i18n.contact.sync.invalid.source");
        };
        final WhereClause whereClause = new WhereClause(Criteria.eq(syncContactTable.getColumn(OMCOSYNCEDCONTACT.OWNER_ID), contactSyncSettings.getUserId()))
                .and(Criteria.eq(syncContactTable.getColumn(OMCOSYNCEDCONTACT.SYNC_ID), contactSyncSettings.id()));

        selectQuery.addJoinClause(new JoinClause(syncContactTable.getColumn(OMCOSYNCEDCONTACT.EXTERNAL_PERSON_ID), externalPersonTable.getPKColumn()));
        selectQuery.addSelectClause(List.of(externalPersonTable.getColumn(OMCOEXTERNALPERSON.CONTACT_ID), originColumn));
        selectQuery.setWhereClause(switch (action) {
            case CREATE -> whereClause.and(Criteria.between(syncContactTable.getColumn(OMCOSYNCEDCONTACT.CREATED_TIME), startTime, endTime));
            case UPDATE -> whereClause.and(Criteria.eq(syncContactTable.getColumn(OMCOSYNCEDCONTACT.IS_MODIFIED), true))
                    .and(Criteria.between(syncContactTable.getColumn(OMCOSYNCEDCONTACT.MODIFIED_TIME), startTime, endTime));
            case DELETE -> whereClause.and(Criteria.eq(syncContactTable.getColumn(OMCOSYNCEDCONTACT.IS_ACTIVE), false))
                    .and(Criteria.between(syncContactTable.getColumn(OMCOSYNCEDCONTACT.DELETED_TIME), startTime, endTime));
            default -> null;
        });
        final DataContainer dataContainer = orgDataStore(contactSyncSettings.orgId()).get(selectQuery);

        contactIds.addAll(dataContainer.getRows(externalPersonTable).map(externalPersonRow ->
                (Long) externalPersonRow.get(OMCOEXTERNALPERSON.CONTACT_ID)).collect(Collectors.toUnmodifiableSet()));
        final Long twozoCount = dataContainer.getRows(syncContactTable, new WhereClause(Criteria.eq(originColumn, TWOZO.getId()))).count();
        final Long providerCount = dataContainer.getRows(syncContactTable, new WhereClause(Criteria.eq(originColumn, PROVIDER.getId()))).count();

        return new ContactSourceDetails(twozoCount, providerCount, twozoCount + providerCount);
    }

    /**
     * <p>
     * Checks the existence of a specific email address within the context of the given organization and owner id.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param ownerId      Represents an owner id
     * @param emailAddress Represents the email address
     * @return true, if exists
     */
    public boolean exists(final long orgId, final long ownerId, final EmailAddress emailAddress) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), ownerId))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS), emailAddress.getEmailId())));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Updates the connection status.
     * </p>
     *
     * @param orgId        Represents an organization id.
     * @param userId       Represents a user id
     * @param emailAddress Represents the email id
     */
    public void updateConnectionStatus(final long orgId, final long userId, final EmailAddress emailAddress) {
        final Table contactSyncTable = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(contactSyncTable);

        updateQuery.addUpdateColumn(contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS), true);
        updateQuery.addUpdateColumn(contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.IS_SYNC_ENABLED), false);
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(contactSyncTable.getColumn(OMCOCONTACTSYNCSETTINGS.EMAIL_ADDRESS), emailAddress.getEmailId())));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Updates the contact {@link SyncStatus} for an respective contact sync id.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    public void updateContactSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo, final String config) {
        final Table table = getTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);

        updateQuery.addUpdateColumn(table.getColumn(OMCOCONTACTSYNCSETTINGS.IN_PROGRESS_CONFIG), config);
        updateQuery.addUpdateColumn(table.getColumn(OMCOCONTACTSYNCSETTINGS.SYNC_STATUS), syncInfo.syncStatus().getId());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getPKColumn(), syncInfo.syncId())));

        orgDataStore(orgId).update(updateQuery);
    }

    /**
     * <p>
     * Gets the contact {@link SyncInfo} for an respective sync id.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return {@link SyncInfo}
     */
    public SyncInfo getContactSyncInfo(final long orgId, final Long syncId) {
        final Row row = orgDataStore(orgId).get(getTable(), syncId);

        return new SyncInfo(row.getPKValue(), dateTimeUtil.getTimeStamp(row.get(OMCOCONTACTSYNCSETTINGS.LAST_SYNC_TIME)),
                SyncStatus.getStatus(row.get(OMCOCONTACTSYNCSETTINGS.SYNC_STATUS)), null);
    }

    /**
     * <p>
     * Checks whether the contact synced exists or not for the given organization and user.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents an user id
     * @param syncId Represents the synchronization id
     * @return true, if the contact sync is exists; otherwise false
     */
    public boolean exists(final long orgId, final long userId, final Long syncId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.USER_ID), userId))
                .and(Criteria.eq(table.getColumn(OMCOCONTACTSYNCSETTINGS.CONNECTION_STATUS), true));

        countQuery.setWhereClause(Objects.isNull(syncId) ? whereClause : whereClause.and(Criteria.eq(table.getPKColumn(), syncId)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the total synced contacts for the given user in an organization
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param userId Represents the user id
     * @return the total contacts synced
     */
    public long getTotalSyncedContacts(final long orgId, final long userId) {
        final Table syncedContactTable = getSyncedContactTable();
        final CountQuery countQuery = CountQuery.get(syncedContactTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(syncedContactTable.getColumn(OMCOSYNCEDCONTACT.OWNER_ID), userId)));
        return orgDataStore(orgId).get(countQuery);
    }
}