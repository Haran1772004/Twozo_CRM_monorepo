package com.tlc.crm.entity.marketinglist.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.marketinglist.model.MarketingList;
import com.tlc.crm.entity.sql.resource.OMKLMARKETINGLIST;
import com.tlc.crm.entity.sql.resource.OMKLUSERPREFERENCE;
import com.tlc.crm.entity.dropdown.DropdownCriteria;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the {@link MarketingList} related actions
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class MarketingListManager extends AbstractConfigManager<MarketingList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketingListManager.class);

    private MarketingListManager() {
    }

    /**
     * Holds the instance of {@link MarketingListManager}
     */
    private static class InstanceHolder {
        private static final MarketingListManager INSTANCE = new MarketingListManager();
    }

    /**
     * Gets the {@link MarketingListManager} instance
     *
     * @return the {@link MarketingListManager} instance
     */
    public static MarketingListManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Table} of marketing list
     */
    @Override
    public Table getTable() {
        return Table.get(OMKLMARKETINGLIST.TABLE);
    }

    /**
     * Gets the user preference table for marketing list
     *
     * @return The user preference {@link Table}
     */
    private Table getUserPreferenceTable() {
        return Table.get(OMKLUSERPREFERENCE.TABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<MarketingList> get(final Long orgId, final Collection<Long> ids) {
        final Table table = getTable();

        return orgDataStore(orgId).get(table, ids).getRows(table).map(row -> convertRowToModel(orgId, row)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organization ID
     * @param ids   which contains the collection of ids
     * @return the collection of {@link MarketingList}
     */
    @Override
    public Collection<MarketingList> partialGet(final Long orgId, final Collection<Long> ids) {
        final Table marketingListTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(marketingListTable);

        selectQuery.addSelectClause(marketingListTable.getColumn(OMKLMARKETINGLIST.NAME));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(marketingListTable.getPKColumn(), ids)));

        return orgDataStore(orgId).get(selectQuery).getRows(marketingListTable).map(marketingListRow -> convertRowToModel(orgId, marketingListRow)).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MarketingList create(final MarketingList marketingList) {
        final Long orgId = marketingList.orgId();
        final DataContainer targetContainer = DataContainer.create();

        loadMarketingListIntoContainer(true, marketingList, null, targetContainer);
        orgDataStore(orgId).commitChanges(targetContainer);

        return marketingList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<MarketingList> marketingLists) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Table table = getTable();
        final Collection<Long> ids = marketingLists.stream().map(MarketingList::id).filter(Objects::nonNull).toList();
        final DataContainer entityContainer = orgDataStore(orgId).get(table, ids);
        final Collection<ErrorDescriptor> failedEntities = new ArrayList<>();

        marketingLists.forEach(marketingList -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadMarketingListIntoContainer(false, marketingList, entityContainer, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(marketingList.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Marketing list update failed Org ID : {} ID : {} reason : {}",
                        orgId, marketingList.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, marketingList.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * Loads the marketing list details into the target container
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param marketingList   An entity that holds the marketing list attributes
     * @param sourceContainer Holds the source data for update scenarios
     * @param targetContainer Holds the updated information of marketing list
     */
    public void loadMarketingListIntoContainer(final boolean isCreateEvent, final MarketingList marketingList,
                                                final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table table = getTable();
        final Long marketingListId = marketingList.id();
        final Long currentTime = Instant.now().toEpochMilli();
        final Row row = isCreateEvent ? new Row(table) : sourceContainer.getRow(table, marketingListId);
        final Long orgId = marketingList.orgId();

        if (row.isNewRow()) {
            marketingList.setId(row.getPKValue());
            row.setOrgId(marketingList.orgId());
            row.set(OMKLMARKETINGLIST.CREATED_AT, currentTime);
            row.set(OMKLMARKETINGLIST.CREATED_BY, marketingList.getCreatedBy().id());
        }

        row.set(OMKLMARKETINGLIST.NAME, marketingList.getName());
        row.set(OMKLMARKETINGLIST.UPDATED_BY, marketingList.getUpdatedBy().id());
        row.set(OMKLMARKETINGLIST.UPDATED_AT, currentTime);

        targetContainer.updateRow(row);

        if (isCreateEvent && marketingList.isDefault()) {
            loadPreferenceIntoContainer(orgId, marketingList.getCreatedBy().id(), marketingList.id(), targetContainer);
        }
    }

    /**
     * Converts row to {@link MarketingList} model
     *
     * @param orgId The organization id
     * @param marketingListRow   The row to convert
     * @return {@link MarketingList}
     */
    public MarketingList convertRowToModel(final Long orgId, final Row marketingListRow) {
        final MarketingList marketingList = new MarketingList();

        marketingList.setId(marketingListRow.getPKValue());
        marketingList.setOrgId(orgId);
        marketingList.setName(marketingListRow.get(OMKLMARKETINGLIST.NAME));
        marketingList.setCreatedTime(marketingListRow.get(OMKLMARKETINGLIST.CREATED_AT));
        marketingList.setModifiedTime(marketingListRow.get(OMKLMARKETINGLIST.UPDATED_AT));

        return marketingList;
    }

    /**
     * Gets the count of marketing lists for an organization
     *
     * @param orgId      The organization id
     * @param searchText The search text to filter marketing lists by name
     * @return The count
     */
    public long getTotalCount(final Long orgId, final String searchText) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        if (Objects.nonNull(searchText) && !searchText.isBlank()) {
            countQuery.setWhereClause(new WhereClause(Criteria.contains(table.getColumn(OMKLMARKETINGLIST.NAME), searchText)));
        }

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Gets the count of marketing lists for an organization
     *
     * @param orgId The organization id
     * @return The count
     */
    public long getTotalCount(final Long orgId) {
        return getTotalCount(orgId, (String) null);
    }

    /**
     * Gets the total count of marketing lists for dropdown
     *
     * @param orgId            The organization id
     * @param dropdownCriteria Represents the dropdown criteria
     * @return The total count
     */
    public long getTotalCount(final Long orgId, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(dropdownCriteria.build(table.getColumn(OMKLMARKETINGLIST.NAME))));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Gets marketing lists for dropdown with pagination
     *
     * @param orgId            The organization id
     * @param limitClause      The limit clause for pagination
     * @param dropdownCriteria Represents the dropdown criteria
     * @return Collection of {@link MarketingList}
     */
    public Collection<MarketingList> getDropdownList(final Long orgId, final LimitClause limitClause, final DropdownCriteria dropdownCriteria) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final Column nameColumn = table.getColumn(OMKLMARKETINGLIST.NAME);

        selectQuery.addSelectClause(List.of(table.getPKColumn(), nameColumn));
        selectQuery.setWhereClause(new WhereClause(dropdownCriteria.build(nameColumn)));
        selectQuery.setLimitClause(limitClause);

        return orgDataStore(orgId).get(selectQuery).getRows(table).map(row -> convertRowToModel(orgId, row)).toList();
    }

    /**
     * Checks whether marketing lists with the given names already exist
     *
     * @param orgId      The organization id
     * @param name       The marketing list name to check
     * @param excludedId The marketing list id to be excluded
     * @return {@code true} if any marketing list with the given names exists, {@code false} otherwise
     */
    public boolean existsByName(final Long orgId, final String name, final Long excludedId) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.eq(table.getColumn(OMKLMARKETINGLIST.NAME), name));

        countQuery.setWhereClause(Objects.nonNull(excludedId) ? whereClause.and(Criteria.notEq(table.getPKColumn(), excludedId)) : whereClause);

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * Checks whether marketing lists already exist
     *
     * @param orgId The organization id
     * @param marketingListIds The collection of marketing list ids
     * @return true if any marketing list exists, false otherwise
     */
    public boolean exists(final Long orgId, final Set<Long> marketingListIds) {
        final Table marketingListTable = getTable();
        final CountQuery countQuery = CountQuery.get(marketingListTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(marketingListTable.getPKColumn(), marketingListIds)));

        return orgDataStore(orgId).get(countQuery) == marketingListIds.size();
    }

    /**
     * Checks whether marketing lists already exist
     *
     * @param orgId                   The organization id
     * @param excludedMarketingListId The marketing list id to be excluded in the check
     * @return true if any other marketing list exists, false otherwise
     */
    public boolean existsExcluding(final Long orgId, final Long excludedMarketingListId) {
        final Table marketingListTable = getTable();
        final CountQuery countQuery = CountQuery.get(marketingListTable);

        countQuery.setWhereClause(new WhereClause(Criteria.notEq(marketingListTable.getPKColumn(), excludedMarketingListId)));

        return orgDataStore(orgId).get(countQuery) > NumericalConstant.ZERO;
    }

    /**
     * Gets marketing list ids by their names
     *
     * @param orgId              The organization id
     * @param marketingListNames The collection of marketing list names to fetch
     * @return Collection of marketing list ids
     */
    public Collection<Long> getMarketingListIds(final Long orgId, final Collection<String> marketingListNames) {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getPKColumn());
        selectQuery.setWhereClause(new WhereClause(Criteria.in(table.getColumn(OMKLMARKETINGLIST.NAME), marketingListNames)));

        return orgDataStore(orgId).get(selectQuery).getRows(table).map(Row::getPKValue).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * <p>
     * Gets the preferred marketing list id for the given user
     * </p>
     *
     * @param orgId  The organization id
     * @param userId The user id
     * @return The preferred marketing list id wrapped in Optional, or empty if not set
     */
    public Optional<Long> getPreferredMarketingListId(final Long orgId, final Long userId) {
        final Table userPreferenceTable = getUserPreferenceTable();
        final SelectQuery selectQuery = SelectQuery.get(userPreferenceTable);

        selectQuery.addSelectClause(userPreferenceTable.getColumn(OMKLUSERPREFERENCE.MARKETING_LIST_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userPreferenceTable.getColumn(OMKLUSERPREFERENCE.USER_ID), userId)));

        return orgDataStore(orgId).get(selectQuery).getRows(userPreferenceTable).findFirst()
                .map(row -> row.get(OMKLUSERPREFERENCE.MARKETING_LIST_ID));
    }

    /**
     * <p>
     * Sets the preferred marketing list for the given user.
     * If a preference already exists, it updates the marketing list id.
     * </p>
     *
     * @param orgId           The organization id
     * @param userId          The user id
     * @param marketingListId The marketing list id to set as preferred
     */
    public void setMarketingListPreference(final Long orgId, final Long userId, final Long marketingListId) {
        final DataContainer targetContainer = DataContainer.create();

        loadPreferenceIntoContainer(orgId, userId, marketingListId, targetContainer);

        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the preferred marketing list information into the target container
     * </p>
     *
     * @param orgId           Represents the org id
     * @param userId          Represents the user id
     * @param marketingListId Represents the marketing list id to be set as preferred
     * @param targetContainer Represents the target container
     */
    private void loadPreferenceIntoContainer(final Long orgId, final Long userId, final Long marketingListId, final DataContainer targetContainer) {
        final Optional<Row> optionalPreferenceRow = getExistingPreferenceRow(orgId, userId);
        final Row preferenceRow = optionalPreferenceRow.orElseGet(() -> new Row(getUserPreferenceTable()));

        if (preferenceRow.isNewRow()) {
            preferenceRow.setOrgId(orgId);
            preferenceRow.set(OMKLUSERPREFERENCE.USER_ID, userId);
        }
        preferenceRow.set(OMKLUSERPREFERENCE.MARKETING_LIST_ID, marketingListId);

        targetContainer.updateRow(preferenceRow);
    }

    /**
     * <p>
     * Gets the existing preference row for the given user
     * </p>
     *
     * @param orgId  The organization id
     * @param userId The user id
     * @return The existing row wrapped in Optional, or empty if not found
     */
    private Optional<Row> getExistingPreferenceRow(final Long orgId, final Long userId) {
        final Table table = getUserPreferenceTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(OMKLUSERPREFERENCE.USER_ID), userId)));

        return orgDataStore(orgId).get(selectQuery).getRows(table).findFirst();
    }
}