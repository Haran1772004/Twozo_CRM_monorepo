package com.tlc.crm.email.notification.email.api.manager;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.email.notification.email.api.model.DomainStatus;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;
import com.tlc.crm.email.notification.sql.resource.OMEMDOMAIN;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAIL;
import com.tlc.crm.email.notification.sql.resource.OMEMSYNCEDEMAILCONVERSATIONS;
import com.tlc.crm.email.notification.sql.resource.OMEMTEAMINBOX;
import com.tlc.crm.entity.dropdown.DropdownCriteria;

import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.email.fields.EmailFields;
import com.tlc.crm.email.notification.sql.resource.OMEMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMFROMADDRESS;
import com.tlc.crm.email.notification.sql.resource.OMEMUSERRECIPIENTS;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.email.common.model.address.EmailAddress;

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
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo.Type.SYNC_EMAIL;
import static com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo.Type.TEAM_INBOX;

/**
 * <p>
 * Provides the implementation for mail related DB actions.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class MailManager {

    private static class Instance {
        private static final MailManager INSTANCE = new MailManager();
    }

    public MailManager() {
    }

    /**
     * <p>
     * Gets the {@link OrgDataStore}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link OrgDataStore}
     */
    protected OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Gets the {@link ReadableDataStore}
     *
     * @return {@link ReadableDataStore}
     */
    protected ReadableDataStore readableDataStore() {
        return SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * <p>
     * Gets the {@link MailManager} Instance
     * </p>
     *
     * @return {@link MailManager}
     */
    public static MailManager getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the Email address table
     * </p>
     *
     * @return {@link Table}
     */
    public Table getEmailAddressTable() {
        return Table.get(OMEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the user recipients table
     * </p>
     *
     * @return {@link Table} of user recipients
     */
    private Table getUserRecipientsTable() {
        return Table.get(OMEMUSERRECIPIENTS.TABLE);
    }

    /**
     * <p>
     * Gets the from address table.
     * </p>
     *
     * @return {@link Table} of from address
     */
    public Table getFromAddressTable() {
        return Table.get(OMEMFROMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the team inbox table.
     * </p>
     *
     * @return {@link Table} of team inbox
     */
    public Table getTeamInboxTeamTable() {
        return Table.get(OMEMTEAMINBOX.TABLE);
    }

    /**
     * <p>
     * Gets the sync email conversation Table.
     * </p>
     *
     * @return {@link Table} of synced emails
     */
    public Table getSyncEmailConversationTable() {
        return Table.get(OMEMSYNCEDEMAILCONVERSATIONS.TABLE);
    }

    /**
     * <p>
     * Gets the team inbox table.
     * </p>
     *
     * @return {@link Table} of team inbox
     */
    public Table getDomainTable() {
        return Table.get(OMEMDOMAIN.TABLE);
    }

    /**
     * <p>
     * Gets the {@link EmailAddress} pk value associated with an email address from a given table,
     * considering the provided organization ID, user ID, criteria.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param whereClause Represents the {@link WhereClause}
     * @param table       Represents the {@link Table}
     * @return {@link EmailAddress}
     */
    public Long getEmailAddressPkValue(final long orgId, final WhereClause whereClause, final Table table) {
        final Table emailAddressTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS));
        selectQuery.addJoinClause(new JoinClause(table.getColumn(EmailFields.EMAIL_ID.name()), emailAddressTable.getPKColumn()));
        selectQuery.setWhereClause(whereClause);
        final Row emailAddressRow = orgDataStore(orgId).get(selectQuery).getRow(emailAddressTable);

        return Objects.isNull(emailAddressRow) ? null : emailAddressRow.getPKValue();
    }

    /**
     * <p>
     * Retrieves the primary key value of an {@link EmailAddress} record for the given
     * email address within the specified organization.
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param emailAddress Represents the email address whose primary key value is to be retrieved
     * @return The primary key value of the {@link EmailAddress}, or {@code null} if not found
     */
    public Long getEmailId(final long orgId, final String emailAddress) {
        final Table emailAddressTable = getEmailAddressTable();
        final DataContainer dataContainer = orgDataStore(orgId).get(getEmailAddressTable(),
                new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress)));
        final Row row = dataContainer.getRow(emailAddressTable);

        return Objects.isNull(row) ? null : row.getPKValue();
    }

    /**
     * <p>
     * Loads or creates {@link FromAddressInfo} related records for the given email address
     * and associates them with the provided user and organization.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param fromAddressInfo Represents the {@link FromAddressInfo} to be loaded or created
     * @param type            Represents the {@link FromAddressInfo.Type}
     * @param targetContainer Represents the {@link DataContainer} to which new rows will be added
     */
    public void loadFromAddressInfo(final Long orgId, final Long userId, final FromAddressInfo fromAddressInfo,
                                    final FromAddressInfo.Type type, final DataContainer targetContainer) {
        final Table fromAddressTable = getFromAddressTable();
        final Table emailAddressTable = getEmailAddressTable();

        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);
        final String fromEmailAddress = fromAddressInfo.getEmailAddress();
        final WhereClause criteria = new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), fromEmailAddress));

        selectQuery.addJoinClause(new JoinClause(emailAddressTable.getPKColumn(), fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(criteria);
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Row sourceEmailRow = dataContainer.getRow(emailAddressTable);
        final Row existingEmailRow = Objects.isNull(sourceEmailRow) ? targetContainer.getRow(emailAddressTable, criteria) : sourceEmailRow;
        final Row emailRow = Objects.isNull(existingEmailRow) ? new Row(emailAddressTable) : existingEmailRow;
        final Long emailPkValue = emailRow.getPKValue();

        if (emailRow.isNewRow()) {
            emailRow.set(OMEMADDRESS.EMAIL_ADDRESS, fromEmailAddress);
            targetContainer.addNewRow(emailRow);
        }
        fromAddressInfo.setCrmEmailId(emailPkValue);
        final short typeId = type.getId();
        final WhereClause fromAddressCriteria = new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE),
                typeId)).and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), emailPkValue));
        final Row sourceFromRow = dataContainer.getRow(fromAddressTable, fromAddressCriteria);
        final Row existingFromRow = Objects.isNull(sourceFromRow) ? targetContainer.getRow(fromAddressTable, fromAddressCriteria)
                : sourceFromRow;
        final Row fromRow = Objects.isNull(existingFromRow) ? new Row(fromAddressTable) : existingFromRow;

        if (fromRow.isNewRow()) {
            fromRow.set(OMEMFROMADDRESS.EMAIL_ID, emailPkValue);
            fromRow.set(OMEMFROMADDRESS.USER_ID, userId);
            fromRow.set(OMEMFROMADDRESS.TYPE, typeId);
            fromRow.set(OMEMFROMADDRESS.IS_ACTIVE, false);

            targetContainer.addNewRow(fromRow);
        }
        fromAddressInfo.setId(fromRow.getPKValue());
    }

    /**
     * <p>
     * Loads the email address
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param emailAddress    An entity that holds email attributes
     * @param targetContainer Holds the updated information of field values provided by user
     */
    public void loadEmailAddress(final Long orgId, final Long userId, final EmailAddress emailAddress,
                                 final DataContainer targetContainer) {
        final Table emailAddressTable = getEmailAddressTable();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), emailAddress.getEmailAddress())));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        loadEmailIntoContainer(orgId, userId, emailAddress, dataContainer, targetContainer);
    }

    /**
     * <p>
     * Loads the {@link FromAddressInfo} in to target {@link DataContainer}.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param type            Represents the {@link FromAddressInfo.Type}
     * @param emailAddressId  Represents the {@link EmailAddress}
     * @param targetContainer Holds the updated records
     */
    public void loadFromAddressIntoContainer(final long orgId, final long userId, final FromAddressInfo.Type type,
                                             final Long emailAddressId, final DataContainer targetContainer) {
        loadFromAddressIntoContainer(orgId, userId, true, type, emailAddressId, targetContainer);
    }

    /**
     * <p>
     * Loads the {@link FromAddressInfo} in to target {@link DataContainer}.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param isActive        Represents whether the status is active or not
     * @param type            Represents the {@link FromAddressInfo.Type}
     * @param emailAddressId  Represents the {@link EmailAddress}
     * @param targetContainer Holds the updated records
     */
    public void loadFromAddressIntoContainer(final long orgId, final long userId, final boolean isActive,
                                             final FromAddressInfo.Type type, final Long emailAddressId,
                                             final DataContainer targetContainer) {
        final Table fromAddressTable = getFromAddressTable();
        final short typeId = type.getId();
        final DataContainer dataContainer = orgDataStore(orgId).get(fromAddressTable,
                new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.USER_ID), userId))
                        .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE), typeId))
                        .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID), emailAddressId)));
        final Row fromAddressRow = dataContainer.isEmpty() ? new Row(fromAddressTable) : dataContainer.getRow(fromAddressTable);

        if (fromAddressRow.isNewRow()) {
            fromAddressRow.setOrgId(orgId);
            fromAddressRow.set(OMEMFROMADDRESS.USER_ID, userId);
        }
        fromAddressRow.set(OMEMFROMADDRESS.EMAIL_ID, emailAddressId);
        fromAddressRow.set(OMEMFROMADDRESS.TYPE, typeId);
        fromAddressRow.set(OMEMFROMADDRESS.IS_ACTIVE, isActive);

        targetContainer.updateRow(fromAddressRow);
    }

    /**
     * <p>
     * Fetches a paginated collection of {@link FromAddressInfo} for the given user and organization.
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param userId   Represents the user id
     * @param start    Pagination start index
     * @param limit    Maximum number of records to return
     * @param criteria Dropdown search/filter criteria
     * @return A collection of {@link FromAddressInfo} objects in paginated order
     */
    public Collection<FromAddressInfo> getFromAddresses(final Long orgId, final Long userId, final int start,
                                                        final int limit, final DropdownCriteria criteria) {
        final Collection<Long> emailIds = getEmailIds(orgId, userId, start, limit, criteria);
        final Map<Long, FromAddressInfo> syncedFromAddresses = getSyncedFromAddresses(orgId, userId, emailIds);
        final Collection<Long> teamInboxEmailIds = emailIds.stream().filter(id -> !syncedFromAddresses.containsKey(id))
                .collect(Collectors.toSet());
        final Map<Long, FromAddressInfo> teamInboxFromAddresses = getTeamInboxFromAddresses(orgId, teamInboxEmailIds);
        final Collection<FromAddressInfo> fromAddressInfos = new ArrayList<>();

        for (final Long id : emailIds) {
            if (syncedFromAddresses.containsKey(id)) {
                fromAddressInfos.add(syncedFromAddresses.get(id));
            } else {
                fromAddressInfos.add(teamInboxFromAddresses.get(id));
            }
        }

        return fromAddressInfos;
    }

    /**
     * <p>
     * Loads the email into container
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents an user id
     * @param address         An entity that holds email attributes
     * @param sourceContainer Holds the user defined values of existing mail
     * @param targetContainer Holds the updated information of field values provided by user
     */
    protected void loadEmailIntoContainer(final Long orgId, final Long userId, final EmailAddress address,
                                        final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table emailAddressTable = getEmailAddressTable();
        final Row emailRow = sourceContainer.isEmpty() ? new Row(emailAddressTable) : sourceContainer.getRow(emailAddressTable);
        final Row existingRow = targetContainer.getRow(emailAddressTable,
                new WhereClause(Criteria.eq(emailAddressTable.getColumn(OMEMADDRESS.EMAIL_ADDRESS), address.getEmailAddress())));

        if (Objects.isNull(existingRow)) {

            if (emailRow.isNewRow()) {
                emailRow.set(OMEMADDRESS.EMAIL_ADDRESS, address.getEmailAddress());
                emailRow.setOrgId(orgId);
            }
            address.setId(emailRow.getPKValue());
            targetContainer.updateRow(emailRow);
        } else {
            address.setId(existingRow.getPKValue());
        }
        loadUserRecipientsIntoContainer(orgId, userId, address, targetContainer);
    }

    /**
     * <p>
     * Loads the user recipients address into the container
     * </p>
     *
     * @param orgId           Represents the organization id
     * @param userId          Represents an user id
     * @param emailAddress    Represents the email address
     * @param targetContainer Represents the target {@link DataContainer}
     */
    private void loadUserRecipientsIntoContainer(final Long orgId, final Long userId, final EmailAddress emailAddress,
                                                 final DataContainer targetContainer) {
        final Table userRecipientsTable = getUserRecipientsTable();
        final SelectQuery selectQuery = SelectQuery.get(userRecipientsTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userRecipientsTable.getColumn(OMEMUSERRECIPIENTS.EMAIL_ID), emailAddress.id()))
                .and(Criteria.eq(userRecipientsTable.getColumn(OMEMUSERRECIPIENTS.USER_ID), userId)));
        final DataContainer sourceContainer = orgDataStore(orgId).get(selectQuery);
        final Row userRecipientsRow = sourceContainer.isEmpty() ? new Row(userRecipientsTable) : sourceContainer.getRow(userRecipientsTable);
        final Row existingRow = targetContainer.getRow(userRecipientsTable,
                new WhereClause(Criteria.eq(userRecipientsTable.getColumn(OMEMUSERRECIPIENTS.USER_ID), userId))
                        .and(Criteria.eq(userRecipientsTable.getColumn(OMEMUSERRECIPIENTS.ORG_ID), orgId))
                        .and(Criteria.eq(userRecipientsTable.getColumn(OMEMUSERRECIPIENTS.EMAIL_ID), emailAddress.id())));

        if (Objects.isNull(existingRow)) {

            if (userRecipientsRow.isNewRow()) {
                userRecipientsRow.set(OMEMUSERRECIPIENTS.EMAIL_ID, emailAddress.id());
                userRecipientsRow.set(OMEMUSERRECIPIENTS.USER_ID, userId);
                userRecipientsRow.setOrgId(orgId);

                targetContainer.updateRow(userRecipientsRow);
            }
        }
    }

    /**
     * <p>
     * Builds the {@link WhereClause} used to filter FromAddress records.
     * </p>
     *
     * @param userId           Represents the user id
     * @param dropdownCriteria Represents the dropdown filter criteria
     * @return {@link WhereClause} for filtering FromAddress records
     */
    private WhereClause getCriteria(final Long userId, final DropdownCriteria dropdownCriteria) {
        final Table fromAddressTable = getFromAddressTable();
        final Column typeColumn = fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE);

        final WhereClause typeCriteria = new WhereClause(Criteria.eq(typeColumn, TEAM_INBOX.getId()))
                .or(new WhereClause(Criteria.eq(typeColumn, SYNC_EMAIL.getId()))
                        .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.USER_ID), userId)));

        return new WhereClause(dropdownCriteria.build(getEmailAddressTable().getColumn(OMEMADDRESS.EMAIL_ADDRESS)))
                .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.IS_ACTIVE), true))
                .and(typeCriteria);
    }

    /**
     * <p>
     * Fetches paginated {@code EMAIL_ID}s based on FromAddress configuration.
     * </p>
     *
     * @param orgId            Represents the organization id
     * @param userId           Represents the user id
     * @param start            Pagination start index
     * @param limit            Maximum number of records to return
     * @param dropdownCriteria Represents the dropdown filter criteria
     * @return List of paginated email ids
     */
    private Collection<Long> getEmailIds(final Long orgId, final Long userId, final int start, final int limit,
                                         final DropdownCriteria dropdownCriteria) {
        final Long defaultEmailId = getDefaultEmailId(orgId, userId);
        final Table fromAddressTable = getFromAddressTable();
        final Table emailAddressTable = getEmailAddressTable();

        final Column emailIdColumn = emailAddressTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(emailAddressTable);

        selectQuery.addSelectClause(emailIdColumn.distinct());
        selectQuery.addJoinClause(new JoinClause(emailIdColumn, fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID)));
        final boolean prependDefaultEmail = Objects.nonNull(defaultEmailId) && start == NumericalConstant.ONE;

        WhereClause whereClause = getCriteria(userId, dropdownCriteria);

        if (prependDefaultEmail) {
            whereClause = whereClause.and(
                    new WhereClause(Criteria.notEq(emailIdColumn, defaultEmailId))
            );
        }
        selectQuery.setWhereClause(whereClause);
        selectQuery.addOrderByClause(new OrderByClause(emailIdColumn));

        selectQuery.setLimitClause(new LimitClause(start, prependDefaultEmail ? limit - NumericalConstant.ONE : limit));

        final List<Long> emailIds = new ArrayList<>(
                orgDataStore(orgId).get(selectQuery)
                        .getRows(emailAddressTable)
                        .map(Row::getPKValue)
                        .toList()
        );

        if (prependDefaultEmail) {
            emailIds.add(NumericalConstant.ZERO, defaultEmailId);
        }

        return emailIds;
    }

    /**
     * <p>
     * Fetches the default synced email id for the given user.
     * Only active and successfully synced email accounts are considered.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @return The default email id if available, otherwise {@code null}
     */
    private Long getDefaultEmailId(final Long orgId, final Long userId) {
        final Table syncedEmailTable = EmailSyncManager.getInstance().getTable();
        final SelectQuery selectQuery = SelectQuery.get(syncedEmailTable);

        selectQuery.addSelectClause(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.EMAIL_ID));

        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.USER_ID), userId))
                        .and(Criteria.eq(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.IS_DEFAULT), true))
                        .and(Criteria.eq(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.CONNECTION_STATUS), ConnectionStatus.ACTIVE.getId()))
                        .and(Criteria.in(syncedEmailTable.getColumn(OMEMSYNCEDEMAIL.SYNC_STATUS),
                                List.of(SyncStatus.SYNCED.getId(), SyncStatus.IN_QUEUE.getId())))
        );
        selectQuery.setLimitClause(new LimitClause(NumericalConstant.ONE, NumericalConstant.ONE));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(syncedEmailTable)
                .findFirst()
                .map(row -> (Long) row.get(OMEMSYNCEDEMAIL.EMAIL_ID))
                .orElse(null);
    }

    /**
     * <p>
     * These addresses are specific to the given user and always take priority over team inbox addresses (TYPE = 2).
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param userId   Represents the user id
     * @param emailIds Collection of EMAIL_IDs to fetch
     * @return Map of email ids to {@link FromAddressInfo}
     */
    private Map<Long, FromAddressInfo> getSyncedFromAddresses(final Long orgId, final Long userId, final Collection<Long> emailIds) {
        if (Objects.isNull(emailIds) || emailIds.isEmpty()) {
            return Map.of();
        }
        final Table fromAddressTable = getFromAddressTable();
        final Table emailAddressTable = getEmailAddressTable();

        final Column emailIdColumn = fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID);
        final SelectQuery selectQuery = SelectQuery.get(fromAddressTable);

        selectQuery.addSelectClause(fromAddressTable, emailAddressTable);
        selectQuery.addJoinClause(new JoinClause(emailIdColumn, emailAddressTable.getPKColumn()));
        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE), SYNC_EMAIL.getId()))
                        .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.USER_ID), userId))
                        .and(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.IS_ACTIVE), true))
                        .and(Criteria.in(emailIdColumn, emailIds))
        );
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, FromAddressInfo> result = new HashMap<>();

        for (final Row row : dataContainer.getRows(fromAddressTable).toList()) {
            final Long emailId = row.get(OMEMFROMADDRESS.EMAIL_ID);
            final Row emailRow = dataContainer.getRow(emailAddressTable, emailId);

            result.put(emailId, buildFromAddressInfo(row, emailRow, true));
        }

        return result;
    }

    /**
     * <p>
     * Loads the FromAddress records.
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param emailIds Collection of EMAIL_IDs to fetch
     * @return Map of email ids to {@link FromAddressInfo}
     */
    private Map<Long, FromAddressInfo> getTeamInboxFromAddresses(final Long orgId, final Collection<Long> emailIds) {
        if (Objects.isNull(emailIds) || emailIds.isEmpty()) {
            return Map.of();
        }
        final Table fromAddressTable = getFromAddressTable();
        final Table emailAddressTable = getEmailAddressTable();
        final Table teamInboxTeamTable = getTeamInboxTeamTable();
        final Table domainTable = getDomainTable();

        final SelectQuery selectQuery = SelectQuery.get(teamInboxTeamTable);
        final Column emailIdColumn = fromAddressTable.getColumn(OMEMFROMADDRESS.EMAIL_ID);

        selectQuery.addSelectClause(fromAddressTable, emailAddressTable, teamInboxTeamTable, domainTable);
        selectQuery.addJoinClause(
                List.of(
                        new JoinClause(teamInboxTeamTable.getColumn(OMEMTEAMINBOX.EMAIL_ID), emailAddressTable.getPKColumn()),
                        new JoinClause(emailAddressTable.getPKColumn(), emailIdColumn),
                        new JoinClause(teamInboxTeamTable.getColumn(OMEMTEAMINBOX.DOMAIN_ID), domainTable.getPKColumn(), JoinClause.JoinType.LEFT)
                )
        );
        selectQuery.setWhereClause(
                new WhereClause(Criteria.eq(fromAddressTable.getColumn(OMEMFROMADDRESS.TYPE), 2))
                        .and(Criteria.in(emailIdColumn, emailIds))
        );
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final Map<Long, FromAddressInfo> result = new HashMap<>();

        for (final Row row : dataContainer.getRows(fromAddressTable).toList()) {
            final Long emailId = row.get(OMEMFROMADDRESS.EMAIL_ID);
            final Row teamInboxRow = dataContainer.getRow(teamInboxTeamTable, new WhereClause(Criteria.eq(teamInboxTeamTable.getColumn(OMEMTEAMINBOX.EMAIL_ID), emailId)));
            final Row domainRow = dataContainer.getRow(domainTable, new WhereClause(Criteria.eq(domainTable.getPKColumn(), teamInboxRow.get(OMEMTEAMINBOX.DOMAIN_ID))));

            result.put(emailId,
                    buildFromAddressInfo(row,
                            dataContainer.getRow(emailAddressTable, emailId),
                            Objects.nonNull(domainRow) && DomainStatus.get(domainRow.get(OMEMDOMAIN.STATUS)).isVerified()
                    )
            );
        }

        return result;
    }

    /**
     * <p>
     * Builds a {@link FromAddressInfo} object from FromAddress and EmailAddress rows.
     * </p>
     *
     * @param fromAddressRow Represents the FromAddress row
     * @param emailRow       Represents the EmailAddress row
     * @param verified       Indicates whether the address/domain is verified
     * @return {@link FromAddressInfo} populated with values
     */
    private FromAddressInfo buildFromAddressInfo(final Row fromAddressRow, final Row emailRow, final boolean verified) {
        final FromAddressInfo fromAddressInfo = new FromAddressInfo(emailRow.get(OMEMADDRESS.EMAIL_ADDRESS));

        fromAddressInfo.setId(fromAddressRow.getPKValue());
        fromAddressInfo.setType(FromAddressInfo.Type.getType(fromAddressRow.get(OMEMFROMADDRESS.TYPE)));
        fromAddressInfo.setUserId(fromAddressRow.get(OMEMFROMADDRESS.USER_ID));
        fromAddressInfo.setVerified(verified);

        return fromAddressInfo;
    }
}