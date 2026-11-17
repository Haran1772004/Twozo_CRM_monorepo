package com.tlc.crm.core.api.marketinglist.manager;

import com.google.common.base.Supplier;
import com.tlc.commons.pair.Pair;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.models.LimitConfig;
import com.tlc.crm.core.api.contact.manager.CompanyManager;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.field.marketinglist.MarketingListFields;
import com.tlc.crm.core.sql.resource.MCOCOMPANY;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.sql.resource.MCONCOMPANYFIELDS;
import com.tlc.crm.core.sql.resource.OMKLMARKETINGLISTCONTACT;
import com.tlc.crm.entity.dropdown.DropdownCriteria;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.fieldaudit.FieldAuditHandler;
import com.tlc.crm.entity.fieldaudit.models.FieldAuditInfo;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.manager.field.type.EmailManager;
import com.tlc.crm.entity.manager.field.type.PhoneManager;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.sql.resource.OMKLMARKETINGLIST;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the marketing list contact related operations
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class MarketingListContactManager {

    private static final int CONTACT_BATCH_SIZE = 250;

    private final FieldAuditHandler fieldAuditHandler;
    private final EmailManager emailManager;
    private final PhoneManager phoneManager;
    private final CompanyManager companyManager;

    private MarketingListContactManager() {
        this.fieldAuditHandler = FieldAuditHandler.getInstance();
        this.emailManager = EmailManager.getInstance();
        this.phoneManager = PhoneManager.getInstance();
        this.companyManager = CompanyManager.getInstance();
    }

    /**
     * Holds the instance of {@link MarketingListContactManager}
     */
    private static class InstanceHolder {
        private static final MarketingListContactManager INSTANCE = new MarketingListContactManager();
    }

    /**
     * Gets the {@link MarketingListContactManager} instance
     *
     * @return The {@link MarketingListContactManager} instance
     */
    public static MarketingListContactManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Gets the {@link OrgDataStore} for the given organization
     *
     * @param orgId The organization id
     * @return The {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Gets the marketing list table
     *
     * @return The marketing list {@link Table}
     */
    public Table getMarketingListTable() {
        return Table.get(OMKLMARKETINGLIST.TABLE);
    }

    /**
     * Gets the marketing list contact table
     *
     * @return The marketing list contact {@link Table}
     */
    public Table getMarketingListContactTable() {
        return Table.get(OMKLMARKETINGLISTCONTACT.TABLE);
    }

    /**
     * Gets the contact table
     *
     * @return The contact {@link Table}
     */
    private Table getContactTable() {
        return Table.get(MCOCONTACT.TABLE);
    }

    /**
     * <p>
     * Adds contacts to the marketing list
     * </p>
     *
     * @param orgId           The organization id
     * @param userId          The user id who is adding the contacts
     * @param marketingListId The marketing list id
     * @param contactIds      The collection of contact ids to be added
     */
    public void addContacts(final Long orgId, final Long userId, final Long marketingListId, final Collection<Long> contactIds) {
        final Table marketingListContactTable = getMarketingListContactTable();
        final Long currentTime = Instant.now().toEpochMilli();
        final Collection<Collection<Long>> batchedContactIds = JavaUtil.splitCollection(contactIds, NumericalConstant.TWENTY_FIVE);

        for (final Collection<Long> batch : batchedContactIds) {
            final Map<Long, Row> existingContactRowMap = getExistingContactRows(orgId, marketingListId, batch);
            final DataContainer targetContainer = DataContainer.create();

            for (final Long contactId : batch) {
                final Row row;

                if (existingContactRowMap.containsKey(contactId)) {
                    row = existingContactRowMap.get(contactId);

                    row.set(OMKLMARKETINGLISTCONTACT.UPDATED_BY, userId);
                    row.set(OMKLMARKETINGLISTCONTACT.UPDATED_AT, currentTime);
                } else {
                    row = new Row(marketingListContactTable);

                    row.setOrgId(orgId);
                    row.set(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID, marketingListId);
                    row.set(OMKLMARKETINGLISTCONTACT.CONTACT_ID, contactId);
                    row.set(OMKLMARKETINGLISTCONTACT.CREATED_BY, userId);
                    row.set(OMKLMARKETINGLISTCONTACT.UPDATED_BY, userId);
                    row.set(OMKLMARKETINGLISTCONTACT.CREATED_AT, currentTime);
                    row.set(OMKLMARKETINGLISTCONTACT.UPDATED_AT, currentTime);
                }

                targetContainer.updateRow(row);
            }
            orgDataStore(orgId).commitChanges(targetContainer);
        }
    }

    /**
     * <p>
     * Gets the existing contact rows in the marketing list as a map of contact id to row
     * </p>
     *
     * @param orgId           The organization id
     * @param marketingListId The marketing list id
     * @param contactIds      The collection of contact ids to check
     * @return Map of contact id to existing row
     */
    private Map<Long, Row> getExistingContactRows(final Long orgId, final Long marketingListId,
                                                  final Collection<Long> contactIds) {
        final Table table = getMarketingListContactTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListId))
                .and(Criteria.in(table.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), contactIds)));

        return orgDataStore(orgId).get(selectQuery).getRows(table).collect(Collectors
                .toMap(row -> row.get(OMKLMARKETINGLISTCONTACT.CONTACT_ID), row -> row));
    }

    /**
     * <p>
     * Gets the existing contact rows in the marketing list as a map of contact id to row
     * </p>
     *
     * @param orgId            The organization id
     * @param marketingListIds The collection of marketing list id
     * @param contactId        The contact id to check
     * @return Map of marketing list id to existing row
     */
    private Map<Long, Row> getExistingContactRows(final Long orgId, final Collection<Long> marketingListIds, final Long contactId) {
        final Table table = getMarketingListContactTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListIds))
                .and(Criteria.eq(table.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), contactId)));

        return orgDataStore(orgId).get(selectQuery).getRows(table).collect(Collectors
                .toMap(row -> row.get(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), row -> row));
    }

    /**
     * Gets marketing lists with their contact counts
     *
     * @param orgId       The organization id
     * @param limitConfig The limit config for pagination
     * @param searchText  The search text to filter marketing lists by name
     * @return Collection of {@link Pair} containing {@link MarketingList} and contact count
     */
    public Collection<Pair<MarketingList, Long>> getListWithContactCounts(final Long orgId, final LimitConfig limitConfig, final String searchText, final Long excludedMarketingListId) {
        final Table marketingListTable = getMarketingListTable();
        final Table marketingListContactTable = getMarketingListContactTable();
        final SelectQuery selectQuery = SelectQuery.get(marketingListTable);
        final Column marketingListPkColumn = marketingListTable.getPKColumn();
        final Column nameColumn = marketingListTable.getColumn(OMKLMARKETINGLIST.NAME);
        WhereClause whereClause = null;

        selectQuery.addSelectClause(List.of(marketingListPkColumn, nameColumn, marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID, MarketingListFields.CONTACT_COUNT).count()));
        selectQuery.addJoinClause(new JoinClause(marketingListPkColumn, marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), JoinClause.JoinType.LEFT));

        if (Objects.nonNull(excludedMarketingListId)) {
            whereClause = new WhereClause(Criteria.notEq(marketingListPkColumn, excludedMarketingListId));
        }

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            final WhereClause searchCriteria = new WhereClause(Criteria.contains(nameColumn, searchText));

            whereClause = Objects.nonNull(whereClause) ? whereClause.and(searchCriteria) : searchCriteria;
        }

        selectQuery.setWhereClause(whereClause);
        selectQuery.addGroupByClause(new GroupByClause(marketingListPkColumn));
        selectQuery.setLimitClause(new LimitClause(limitConfig.start(), limitConfig.limit()));
        selectQuery.addOrderByClause(new OrderByClause(marketingListTable.getColumn(OMKLMARKETINGLIST.CREATED_AT)));

        return orgDataStore(orgId).getData(selectQuery).stream()
                .map(row -> {
                    final MarketingList marketingList = new MarketingList();

                    marketingList.setId((Long) row.get(OMKLMARKETINGLIST.ID));
                    marketingList.setOrgId(orgId);
                    marketingList.setName((String) row.get(OMKLMARKETINGLIST.NAME));

                    return Pair.of(marketingList, (Long) row.get(MarketingListFields.CONTACT_COUNT));
                }).collect(Collectors.toList());
    }

    /**
     * Gets a single marketing list with its contact count
     *
     * @param orgId           The organization id
     * @param marketingListId The marketing list id
     * @return Optional of {@link Pair} containing {@link MarketingList} and contact count, or empty if not found
     */
    public Optional<Pair<MarketingList, Long>> fetchWithContactCount(final Long orgId, final Long marketingListId) {
        final Table marketingListTable = getMarketingListTable();
        final Table marketingListContactTable = getMarketingListContactTable();
        final SelectQuery selectQuery = SelectQuery.get(marketingListTable);
        final Column marketingListPkColumn = marketingListTable.getPKColumn();
        final Column nameColumn = marketingListTable.getColumn(OMKLMARKETINGLIST.NAME);

        selectQuery.addSelectClause(List.of(marketingListPkColumn, nameColumn, marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID, MarketingListFields.CONTACT_COUNT).count()));
        selectQuery.addJoinClause(new JoinClause(marketingListPkColumn, marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(marketingListPkColumn, marketingListId)));
        selectQuery.addGroupByClause(new GroupByClause(marketingListPkColumn));

        return orgDataStore(orgId).getData(selectQuery).stream()
                .map(row -> {
                    final MarketingList marketingList = new MarketingList();

                    marketingList.setId((Long) row.get(OMKLMARKETINGLIST.ID));
                    marketingList.setOrgId(orgId);
                    marketingList.setName((String) row.get(OMKLMARKETINGLIST.NAME));

                    return Pair.of(marketingList, (Long) row.get(MarketingListFields.CONTACT_COUNT));
                }).findFirst();
    }

    /**
     * Transfers contacts from source marketing list to target marketing list and deletes from source
     *
     * @param orgId                   The organization id
     * @param userId                  The user id performing the transfer
     * @param sourceMarketingListId   The source marketing list id
     * @param targetMarketingListId   The target marketing list id
     */
    public void transferContactsAndDelete(final Long orgId, final Long userId, final Long sourceMarketingListId,
                                          final Long targetMarketingListId) {
        if (Objects.nonNull(targetMarketingListId)) {
            final long totalContactCount = getTotalContactCount(orgId, sourceMarketingListId);
            int start = 1;

            while (start <= totalContactCount) {
                final Collection<Long> contactIds = getContactIds(orgId, sourceMarketingListId, new LimitConfig(start, CONTACT_BATCH_SIZE));

                if (!contactIds.isEmpty()) {
                    addContacts(orgId, userId, targetMarketingListId, contactIds);
                }

                start += CONTACT_BATCH_SIZE;
            }
        }

        orgDataStore(orgId).delete(getMarketingListTable(), sourceMarketingListId);
    }

    /**
     * Gets the total count of contacts in a marketing list
     *
     * @param orgId           The organization id
     * @param marketingListId The marketing list id
     * @return The total contact count
     */
    public long getTotalContactCount(final Long orgId, final Long marketingListId) {
        final Table table = getMarketingListContactTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListId)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Gets all contact ids from a marketing list
     *
     * @param orgId           The organization id
     * @param marketingListId The marketing list id
     * @param limitConfig     The limit config for pagination
     * @return Collection of contact ids
     */
    private Collection<Long> getContactIds(final Long orgId, final Long marketingListId, final LimitConfig limitConfig) {
        return getContactIds(orgId, marketingListId, Collections.emptyList(), limitConfig);
    }

    /**
     * Gets contact ids from a marketing list, excluding specified contact ids
     *
     * @param orgId              The organization id
     * @param marketingListId    The marketing list id
     * @param excludedContactIds The collection of contact ids to exclude
     * @return Collection of contact ids
     */
    public Collection<Long> getContactIds(final Long orgId, final Long marketingListId, final Collection<Long> excludedContactIds, final LimitConfig limitConfig) {
        final Table table = getMarketingListContactTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column contactIdColumn = table.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListId));

        selectQuery.addSelectClause(contactIdColumn);
        selectQuery.setWhereClause(!excludedContactIds.isEmpty() ? whereClause.and(Criteria.notIn(contactIdColumn, excludedContactIds)) : whereClause);

        if (Objects.nonNull(limitConfig)) {
            selectQuery.setLimitClause(limitConfig.getLimitClause());
            selectQuery.addOrderByClause(new OrderByClause(contactIdColumn));
        }

        return orgDataStore(orgId).get(selectQuery).getRows(table).map(row -> (Long) row.get(OMKLMARKETINGLISTCONTACT.CONTACT_ID))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Removes contacts from the marketing list
     * </p>
     *
     * @param orgId           The organization id
     * @param marketingListId The marketing list id
     * @param contactIds      The collection of contact ids to be removed
     */
    public void removeContacts(final Long orgId, final Long marketingListId, final Collection<Long> contactIds) {
        final Table marketingListContactTable = getMarketingListContactTable();
        final DeleteQuery deleteQuery = DeleteQuery.get(marketingListContactTable);

        deleteQuery.setWhereClause(new WhereClause(Criteria.eq(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListId))
                .and(Criteria.in(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), contactIds)));

        orgDataStore(orgId).delete(deleteQuery);
    }

    /**
     * <p>
     * Moves a contact from source marketing list to target marketing list
     * </p>
     *
     * @param orgId                 The organization id
     * @param userId                The user id performing the move
     * @param sourceMarketingListId The source marketing list id
     * @param targetMarketingListId The target marketing list id
     * @param contactId             The contact id to be moved
     */
    public void moveContact(final Long orgId, final Long userId, final Long sourceMarketingListId,
                            final Long targetMarketingListId, final Long contactId) {
        final Map<Long, Row> existingContactRowMap = getExistingContactRows(orgId, List.of(sourceMarketingListId, targetMarketingListId), contactId);
        final Row sourceRow = existingContactRowMap.get(sourceMarketingListId);
        final Row targetRow = existingContactRowMap.get(targetMarketingListId);
        final DataContainer targetContainer = DataContainer.create();

        if (Objects.nonNull(targetRow)) {
            targetRow.set(OMKLMARKETINGLISTCONTACT.UPDATED_BY, userId);
            targetRow.set(OMKLMARKETINGLISTCONTACT.UPDATED_AT, Instant.now().toEpochMilli());

            targetContainer.updateRow(targetRow);
            targetContainer.deleteRow(sourceRow);
        } else {
            sourceRow.set(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID, targetMarketingListId);
            sourceRow.set(OMKLMARKETINGLISTCONTACT.UPDATED_BY, userId);
            sourceRow.set(OMKLMARKETINGLISTCONTACT.UPDATED_AT, Instant.now().toEpochMilli());
            targetContainer.updateRow(sourceRow);
        }

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the join and select clauses for marketing list contacts into the given {@link SelectQuery}.
     * </p>
     *
     * @param contactTable The contact {@link Table}
     * @param selectQuery  The {@link SelectQuery} to load joins and select clauses into
     */
    public void loadJoinsAndSelectClause(final Table contactTable, final SelectQuery selectQuery) {
        final Table marketingListTable = getMarketingListTable();
        final Table marketingListContactTable = getMarketingListContactTable();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addJoinClause(List.of(new JoinClause(contactTable.getPKColumn(), marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), leftJoin),
                new JoinClause(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListTable.getPKColumn(), leftJoin)));
        selectQuery.addSelectClause(marketingListContactTable, marketingListTable);
    }

    /**
     * <p>
     * Maps contacts with marketing lists. For create events, it creates new rows for all the given marketing lists.
     * </p>
     *
     * @param orgId           Refers the organization id
     * @param userId          Refers the user id performing the operation
     * @param isCreateEvent   Refers whether the mapping is for create event or update event
     * @param contactId       Refers the contact id to be mapped
     * @param marketingLists  Refers the collection of marketing lists to be mapped with contact
     * @param sourceContainer Refers the source data container containing existing mapping information
     * @param targetContainer Refers the target data container where the mapping will be loaded
     */
    public void mapContactsWithMarketingList(final Long orgId, final Long userId, final boolean isCreateEvent, final Long contactId,
                                             final Collection<MarketingList> marketingLists, final DataContainer sourceContainer,
                                             final DataContainer targetContainer) {
        mapContacts(orgId, userId, isCreateEvent, contactId, marketingLists, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Maps contacts with marketing lists. For create events, it creates new rows for all the given marketing lists.
     * </p>
     *
     * @param orgId         Refers the organization id
     * @param userId        Refers the user id performing the operation
     * @param contactId     Refers the contact id to be mapped
     * @param marketingList Refers the marketing list to be mapped with contact
     */
    public void mapContactsWithMarketingList(final Long orgId, final Long userId, final Long contactId, final MarketingList marketingList) {
        final Table marketingListContactTable = getMarketingListContactTable();
        final DataContainer sourceContainer = orgDataStore(orgId).get(marketingListContactTable,
                new WhereClause(Criteria.eq(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), contactId))
                        .and(Criteria.eq(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingList.id())));
        final DataContainer targetContainer = DataContainer.create();

        mapContacts(orgId, userId, false, contactId, List.of(marketingList), sourceContainer, targetContainer);
        loadMarketingFieldAudit(orgId, userId, contactId, marketingList.id(), targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the field audit entry for marketing list field changes.
     * </p>
     *
     * @param orgId              Refers the organization id
     * @param userId             Refers the user id performing the operation
     * @param contactId          Refers the contact id for which the marketing list field is changed
     * @param newMarketingListId Refers the new marketing list id that is being mapped with the contact
     * @param targetContainer    Refers the target data container where the field audit entry will be loaded
     */
    private void loadMarketingFieldAudit(final Long orgId, final Long userId, final Long contactId, final Long newMarketingListId,
            final DataContainer targetContainer) {
        final Map<Long, String> valueMap = getNames(orgId, contactId);
        final String oldValues = String.join(",", valueMap.values());

        getNames(orgId, List.of(newMarketingListId)).forEach(name -> valueMap.put(newMarketingListId, name));
        final String newValues = String.join(",", valueMap.values());

        fieldAuditHandler.loadFieldAuditEntry(orgId, Source.CONTACT, contactId, userId, FieldAuditInfo.defaultContext(), MarketingListFields.MARKETING_LIST,
               oldValues, newValues, targetContainer);
    }

    /**
     * <p>
     * Builds a collection of {@link MarketingList} from the data container for the given contact
     * </p>
     *
     * @param orgId         The organization id
     * @param contactId     The contact id
     * @param dataContainer The data container containing marketing list contact rows
     * @return Collection of {@link MarketingList} containing only id, name, and orgId
     */
    public Collection<MarketingList> buildMarketingLists(final Long orgId, final Long contactId, final DataContainer dataContainer) {
        final Table marketingListContactTable = getMarketingListContactTable();
        final Table marketingListTable = getMarketingListTable();
        final Collection<MarketingList> marketingLists = new ArrayList<>();

        dataContainer.getRows(marketingListContactTable, new WhereClause(Criteria.eq(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), contactId)))
                .forEach(marketingListContactRow -> {
                    final Long marketingListId = marketingListContactRow.get(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID);
                    final Row marketingListRow = dataContainer.getRow(marketingListTable, marketingListId);

                    if (Objects.nonNull(marketingListRow)) {
                        final MarketingList marketingList = new MarketingList();

                        marketingList.setId(marketingListId);
                        marketingList.setOrgId(orgId);
                        marketingList.setName(marketingListRow.get(OMKLMARKETINGLIST.NAME));
                        marketingLists.add(marketingList);
                    }
                });

        return marketingLists;
    }

    /**
     * <p>
     * Maps contacts with marketing lists. Creates new rows for create events,
     * or updates existing rows for update events.
     * </p>
     *
     * @param orgId           The organization id
     * @param userId          The user id performing the operation
     * @param isCreateEvent   A flag indicating whether this is a create event
     * @param contactId       The contact id to be mapped
     * @param marketingLists  The collection of marketing lists to map the contact with
     * @param sourceContainer The source data container containing existing mapping information
     * @param targetContainer The target data container where the mapping will be loaded
     */
    private void mapContacts(final Long orgId, final Long userId, final boolean isCreateEvent, final Long contactId,
                            final Collection<MarketingList> marketingLists, final DataContainer sourceContainer,
                            final DataContainer targetContainer) {
        final Table marketingListContactTable = getMarketingListContactTable();
        final Column contactIdColumn = marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID);
        final Column marketingListIdColumn = marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID);
        final Supplier<Row> rowCreator = () -> new Row(marketingListContactTable);
        final Long currentTime = Instant.now().toEpochMilli();

        for (final MarketingList marketingList : marketingLists) {
            final Long marketingListId = marketingList.id();
            final Row marketingListContactRow = isCreateEvent ? rowCreator.get() : Optional.ofNullable(sourceContainer
                    .getRow(marketingListContactTable, new WhereClause(Criteria.eq(contactIdColumn, contactId))
                            .and(Criteria.eq(marketingListIdColumn, marketingListId)))).orElseGet(rowCreator);

            if (marketingListContactRow.isNewRow()) {
                marketingListContactRow.setOrgId(orgId);
                marketingListContactRow.set(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID, marketingListId);
                marketingListContactRow.set(OMKLMARKETINGLISTCONTACT.CONTACT_ID, contactId);
                marketingListContactRow.set(OMKLMARKETINGLISTCONTACT.CREATED_BY, userId);
                marketingListContactRow.set(OMKLMARKETINGLISTCONTACT.UPDATED_BY, userId);
                marketingListContactRow.set(OMKLMARKETINGLISTCONTACT.CREATED_AT, currentTime);
                marketingListContactRow.set(OMKLMARKETINGLISTCONTACT.UPDATED_AT, currentTime);

                targetContainer.addNewRow(marketingListContactRow);
            } else {
                marketingListContactRow.set(OMKLMARKETINGLISTCONTACT.UPDATED_BY, userId);
                marketingListContactRow.set(OMKLMARKETINGLISTCONTACT.UPDATED_AT, currentTime);

                targetContainer.updateRow(marketingListContactRow);
            }
        }
    }

    /**
     * <p>
     * Deletes the unmapped marketing list contact rows from the target container
     * </p>
     *
     * @param contactId       The contact id
     * @param marketingLists  The collection of marketing lists
     * @param sourceContainer The source data container containing existing mapping information
     * @param targetContainer The target data container where the deletions will be loaded
     */
    public void deleteUnmappedMarketingLists(final Long contactId, final Collection<MarketingList> marketingLists,
                                              final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<Long> marketingListIds = marketingLists.stream().map(MarketingList::id).collect(Collectors.toUnmodifiableSet());
        final Table marketingListContactTable = getMarketingListContactTable();
        final Criteria contactCriteria = Criteria.eq(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), contactId);
        final WhereClause whereClause = marketingListIds.isEmpty()
                ? new WhereClause(contactCriteria)
                : new WhereClause(contactCriteria).and(Criteria.notIn(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListIds));

        sourceContainer.getRows(marketingListContactTable, whereClause).forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Gets the count of contacts that are not assigned to the specified marketing list.
     * Uses LEFT JOIN with a derived table for better performance with large datasets.
     * </p>
     *
     * @param orgId            The organization id
     * @param marketingListId  The marketing list id
     * @param dropdownCriteria The dropdown criteria for filtering
     * @return The count of unassigned contacts
     */
    public long getUnassignedContactCount(final Long orgId, final Long marketingListId, final DropdownCriteria dropdownCriteria) {
        final Table contactTable = getContactTable();
        final CountQuery countQuery = CountQuery.get(contactTable);
        final Table assignedContactsDerivedTable = getDerivedTable(marketingListId);
        final Column derivedContactIdColumn = assignedContactsDerivedTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID);

        countQuery.addJoinClause(new JoinClause(contactTable.getPKColumn(), derivedContactIdColumn, JoinClause.JoinType.LEFT));
        countQuery.setWhereClause(getUnassignedContactsCriteria(derivedContactIdColumn, contactTable, dropdownCriteria));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * <p>
     * Gets the contacts (id and name only) that are not assigned to the specified marketing list.
     * </p>
     *
     * @param orgId            The organization id
     * @param marketingListId  The marketing list id
     * @param dropdownLimit    The dropdown limit for pagination
     * @param dropdownCriteria The dropdown criteria for filtering
     * @return Collection of contacts containing id and name
     */
    public Collection<Contact> getUnassignedContacts(final Long orgId, final Long marketingListId, final DropdownLimit dropdownLimit,
                                                     final DropdownCriteria dropdownCriteria) {
        final ContactManager contactManager = ContactManager.getInstance();
        final Table contactTable = getContactTable();
        final SelectQuery selectQuery = SelectQuery.get(contactTable);
        final Column contactPkColumn = contactTable.getPKColumn();
        final Table assignedContactsDerivedTable = getDerivedTable(marketingListId);
        final Column derivedContactIdColumn = assignedContactsDerivedTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID);
        final Table emailTable = emailManager.getTable();
        final Table phoneTable = phoneManager.getTable();
        final Table companyTable = companyManager.getTable();
        final Table contactCompanyTable = contactManager.getContactCompanyTable();
        final Column primaryEmailColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL);
        final Column primaryPhoneColumn = contactTable.getColumn(MCOCONTACT.PRIMARY_PHONE);
        final Column companyPkColumn = companyTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(List.of(contactPkColumn, contactTable.getColumn(MCOCONTACT.FIRST_NAME),
                contactTable.getColumn(MCOCONTACT.LAST_NAME), primaryEmailColumn, primaryPhoneColumn));
        selectQuery.addSelectClause(List.of(emailTable.getPKColumn(), emailTable.getColumn(OMETEMAIL.VALUE)));
        selectQuery.addSelectClause(List.of(phoneTable.getPKColumn(), phoneTable.getColumn(OMETPHONE.NUMBER)));
        selectQuery.addSelectClause(List.of(companyPkColumn, companyTable.getColumn(MCOCOMPANY.NAME)));
        selectQuery.addSelectClause(contactCompanyTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(contactPkColumn, derivedContactIdColumn, leftJoin),
                new JoinClause(primaryEmailColumn, emailTable.getPKColumn(), leftJoin),
                new JoinClause(primaryPhoneColumn, phoneTable.getPKColumn(), leftJoin),
                new JoinClause(contactPkColumn, contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), leftJoin),
                new JoinClause(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.COMPANY_ID), companyPkColumn, leftJoin)));
        selectQuery.setWhereClause(getUnassignedContactsCriteria(derivedContactIdColumn, contactTable, dropdownCriteria));
        selectQuery.setLimitClause(new LimitClause(dropdownLimit.start(), dropdownLimit.limit()));
        selectQuery.addOrderByClause(new OrderByClause(contactPkColumn));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(contactTable)
                .map(row -> {
                    final Contact contact = new Contact();

                    contact.setId(row.get(MCOCONTACT.ID));
                    contact.setOrgId(orgId);
                    contact.setFirstName(row.get(MCOCONTACT.FIRST_NAME));
                    contact.setLastName(row.get(MCOCONTACT.LAST_NAME));

                    final Optional<Long> primaryEmailId = Optional.ofNullable(row.get(MCOCONTACT.PRIMARY_EMAIL));
                    final Optional<Long> primaryPhoneId = Optional.ofNullable(row.get(MCOCONTACT.PRIMARY_PHONE));
                    final Optional<Row> emailRow = primaryEmailId.map(id -> dataContainer.getRow(emailTable, id));
                    final Optional<Row> phoneRow = primaryPhoneId.map(id -> dataContainer.getRow(phoneTable, id));
                    final Optional<Row> contactCompanyRow = Optional.ofNullable(dataContainer.getRow(contactCompanyTable,
                            new WhereClause(Criteria.eq(contactCompanyTable.getColumn(MCONCOMPANYFIELDS.ENTITY_ID), row.getPKValue()))));
                    final Optional<Row> companyRow = contactCompanyRow.map(contactCompanyRowValue ->
                            dataContainer.getRow(companyTable, (Long) contactCompanyRowValue.get(MCONCOMPANYFIELDS.COMPANY_ID)));

                    emailRow.ifPresent(email -> contact.setPrimaryEmail(emailManager.convertRowToModel(email)));
                    phoneRow.ifPresent(phone -> contact.setPrimaryPhone(phoneManager.convertRowToModel(phone)));
                    companyRow.ifPresent(company -> contact.setCompany(companyManager.convertRowToPartialModel(company)));

                    return contact;
                }).collect(Collectors.toList());
    }

    /**
     * Get marketing list names
     *
     * @param orgId Represents the Organization id
     * @param ids   Represents the {@link Collection} of tag id
     * @return Collection of {@link MarketingList}
     */
    public Collection<String> getNames(final Long orgId, final Collection<Long> ids) {
        final Table table = getMarketingListTable();

        return orgDataStore(orgId).get(table, ids, List.of(table.getColumn(OMKLMARKETINGLIST.NAME))).getRows(table)
                .filter(Objects::nonNull).map(row -> (String) row.get(OMKLMARKETINGLIST.NAME)).toList();
    }

    /**
     * <p>
     * Gets contact counts for the given marketing list ids in a single query.
     * </p>
     *
     * @param orgId            Represents the organization id.
     * @param marketingListIds Represents the marketing list ids.
     * @return A map of marketing list id to contact count.
     */
    public Map<Long, Long> getContactCountMap(final Long orgId, final Collection<Long> marketingListIds) {
        final Table marketingListContactTable = getMarketingListContactTable();
        final SelectQuery selectQuery = SelectQuery.get(marketingListContactTable);
        final Column marketingListIdColumn = marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID);

        selectQuery.addSelectClause(List.of(marketingListIdColumn, marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID).count()));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(marketingListIdColumn, marketingListIds)));
        selectQuery.addGroupByClause(new GroupByClause(marketingListIdColumn));

        return orgDataStore(orgId).getData(selectQuery).stream()
                .collect(Collectors.toMap(row -> (Long) row.get(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID),
                        row -> (Long) row.get(OMKLMARKETINGLISTCONTACT.CONTACT_ID)));
    }

    /**
     * <p>
     * Builds the where clause for fetching contacts not assigned to a marketing list.
     * Uses IS NULL check on the derived table's contact_id column to identify unassigned contacts.
     * </p>
     *
     * @param derivedContactIdColumn The contact_id column from the derived table
     * @param contactTable           The contact table
     * @param dropdownCriteria       The dropdown criteria for filtering
     * @return The where clause
     */
    private WhereClause getUnassignedContactsCriteria(final Column derivedContactIdColumn, final Table contactTable,
                                                      final DropdownCriteria dropdownCriteria) {
        final WhereClause whereClause = new WhereClause(Criteria.eq(derivedContactIdColumn, null));

        if (Objects.nonNull(dropdownCriteria)) {
            final Object criteriaValue = dropdownCriteria.getValue();

            if (Objects.nonNull(criteriaValue) && !criteriaValue.toString().isBlank()) {
                final Column firstNameColumn = contactTable.getColumn(MCOCONTACT.FIRST_NAME);
                final Column lastNameColumn = contactTable.getColumn(MCOCONTACT.LAST_NAME);

                return new WhereClause(dropdownCriteria.build(firstNameColumn)).or(dropdownCriteria.build(lastNameColumn))
                        .or(dropdownCriteria.build(firstNameColumn.concatWithTrim(lastNameColumn.trim()))).and(whereClause);
            }
        }

        return whereClause;
    }

    /**
     * <p>
     * Builds a derived table containing contact IDs assigned to the specified marketing list.
     * This derived table is used with LEFT JOIN to efficiently identify unassigned contacts.
     * </p>
     *
     * @param marketingListId The marketing list id
     * @return The derived table containing assigned contact IDs
     */
    private Table getDerivedTable(final Long marketingListId) {
        final Table marketingListContactTable = getMarketingListContactTable();
        final SelectQuery selectQuery = SelectQuery.get(marketingListContactTable);

        selectQuery.addSelectClause(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListId)));

        return DerivedTable.get(marketingListContactTable.getName(), marketingListContactTable.getAlias(), selectQuery);
    }

    /**
     * <p>
     * Gets a map of marketing list IDs to their names for the specified contact.
     * </p>
     *
     * @param orgId     The organization id
     * @param contactId The contact id
     * @return Map of marketing list ID to name
     */
    private Map<Long, String> getNames(final Long orgId, final Long contactId) {
        final Table marketingListTable = getMarketingListTable();
        final Table marketingListContactTable = getMarketingListContactTable();
        final SelectQuery selectQuery = SelectQuery.get(marketingListContactTable);

        selectQuery.addSelectClause(List.of(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListTable.getColumn(OMKLMARKETINGLIST.NAME)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.CONTACT_ID), contactId)));
        selectQuery.addJoinClause(new JoinClause(marketingListContactTable.getColumn(OMKLMARKETINGLISTCONTACT.MARKETING_LIST_ID), marketingListTable.getPKColumn()));
        selectQuery.addOrderByClause(new OrderByClause(marketingListTable.getPKColumn()));

        return orgDataStore(orgId).get(selectQuery).getRows(marketingListTable).filter(Objects::nonNull)
                .collect(Collectors.toMap(Row::getPKValue, row -> row.get(OMKLMARKETINGLIST.NAME), (existing, replacement) -> existing, TreeMap::new));
    }
}