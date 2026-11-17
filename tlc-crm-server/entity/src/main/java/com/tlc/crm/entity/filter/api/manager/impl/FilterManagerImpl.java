package com.tlc.crm.entity.filter.api.manager.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.common.sql.resource.MCRMUSERTEAM;
import com.tlc.crm.common.sql.resource.MCRMUSERTERRITORY;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.AdditionalOption;
import com.tlc.crm.entity.filter.api.models.CriteriaField;
import com.tlc.crm.entity.filter.api.models.CriteriaOption;
import com.tlc.crm.entity.filter.api.models.CriteriaValue;
import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.filter.api.models.FilterConnector;
import com.tlc.crm.entity.filter.api.models.FilterCriteria;
import com.tlc.crm.entity.filter.api.models.FilterProvider;
import com.tlc.crm.entity.filter.api.models.FilterVisibility;
import com.tlc.crm.entity.filter.api.models.Visibility;
import com.tlc.crm.entity.filter.api.models.VisibilityPermission;
import com.tlc.crm.entity.filter.fields.FilterFields;
import com.tlc.crm.entity.internal.status.FilterErrorCodes;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.service.filter.ComparatorService;
import com.tlc.crm.entity.service.filter.FilterFieldService;
import com.tlc.crm.entity.service.filter.FilterSourceService;
import com.tlc.crm.entity.sql.resource.METDATAPROVIDER;
import com.tlc.crm.entity.sql.resource.METFCRITERIA;
import com.tlc.crm.entity.sql.resource.METFCRITERIAADDITIONALFIELD;
import com.tlc.crm.entity.sql.resource.METFCRITERIAADDITIONALOPTION;
import com.tlc.crm.entity.sql.resource.METFCRITERIAFIELD;
import com.tlc.crm.entity.sql.resource.METFCRITERIAOPTION;
import com.tlc.crm.entity.sql.resource.METFCRITERIAVALUE;
import com.tlc.crm.entity.sql.resource.METFFILTERCRITERIA;
import com.tlc.crm.entity.sql.resource.METFTEAMVISIBILITY;
import com.tlc.crm.entity.sql.resource.METFTERRITORYVISIBILITY;
import com.tlc.crm.entity.sql.resource.METFUSERVISIBILITY;
import com.tlc.crm.entity.sql.resource.OMETFFAVOURITEFILTER;
import com.tlc.crm.entity.sql.resource.OMETFILTER;
import com.tlc.crm.entity.sql.resource.OMETFILTERVISIBILITY;
import com.tlc.crm.entity.sql.resource.OMETFRECENTLYAPPLIEDFILTER;

import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.DmlUtil;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the {@link Filter} related DB actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see AbstractConfigManager
 */
public class FilterManagerImpl extends AbstractConfigManager<Filter> implements FilterManager {

    private final FilterSourceService filterSourceService;
    private final UserService userService;
    private final ComparatorService comparatorService;
    private final FilterFieldService filterFieldService;
    private final CriteriaFieldManagerImpl criteriaFieldManager;
    private final VisibilityManagerImpl visibilityManager;

    private static class Instance {
        private static final FilterManagerImpl INSTANCE = new FilterManagerImpl();
    }

    private FilterManagerImpl() {
        filterSourceService = FilterSourceService.getInstance();
        userService = UserService.getInstance();
        comparatorService = ComparatorService.getInstance();
        filterFieldService = FilterFieldService.getSecureInstance();
        criteriaFieldManager = CriteriaFieldManagerImpl.getInstance();
        visibilityManager = VisibilityManagerImpl.getInstance();
    }

    public static FilterManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return OMETFilter table.
     */
    @Override
    public Table getTable() {
        return Table.get(OMETFILTER.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the criteria.
     * </p>
     *
     * @return OMETFCriteria table.
     */
    public Table getCriteriaTable() {
        return Table.get(METFCRITERIA.TABLE);
    }

    /**
     * <p>
     * Fetches the table that maps filters to criteria.
     * </p>
     *
     * @return METFFilterCriteria table.
     */
    private Table getFilterCriteriaTable() {
        return Table.get(METFFILTERCRITERIA.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the criteria value.
     * </p>
     *
     * @return METFCriteriaValue table.
     */
    private Table getCriteriaValueTable() {
        return Table.get(METFCRITERIAVALUE.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the data provider.
     * </p>
     *
     * @return METFDataProvider table.
     */
    private Table getProviderTable() {
        return Table.get(METDATAPROVIDER.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the active filter.
     * </p>
     *
     * @return OMETFActiveFilter table.
     */
    private Table getActiveFilterTable() {
        return Table.get(OMETFRECENTLYAPPLIEDFILTER.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the favourite filter.
     * </p>
     *
     * @return OMETFFavouriteFilter table.
     */
    private Table getFavouriteFilterTable() {
        return Table.get(OMETFFAVOURITEFILTER.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the user team.
     * </p>
     *
     * @return MCRMUserTeam table.
     */
    private Table getUserTeamTable() {
        return Table.get(MCRMUSERTEAM.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the user territory.
     * </p>
     *
     * @return MCRMUserTerritory table.
     */
    private Table getUserTerritoryTable() {
        return Table.get(MCRMUSERTERRITORY.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organization id.
     * @param ids   Which contains the collection of filter ids.
     * @return Collection of {@link Filter} that hold the retrieved data.
     */
    @Override
    public Collection<Filter> get(final Long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = DataContainer.create();

        loadFilterData(orgId, ids, dataContainer);
        loadVisibilityData(orgId, dataContainer);
        loadCriteriaData(orgId, ids, dataContainer);

        return dataContainer.getRows(getTable())
                .map(filterRow -> convertRowToFilter(filterRow, dataContainer))
                .toList();
    }

    /**
     * <p>
     * Loads filter, provider and visibility data into the target container.
     * </p>
     *
     * @param orgId         Represents the organization id.
     * @param ids           Holds the collection of filter ids.
     * @param dataContainer Holds the retrieved data.
     */
    private void loadFilterData(final Long orgId, final Collection<Long> ids, final DataContainer dataContainer) {
        final Table filterTable = getTable();
        final Table providerTable = getProviderTable();
        final Table filterVisibilityTable = visibilityManager.getFilterVisibilityTable();
        final Table userVisibilityTable = visibilityManager.getUserVisibilityTable();
        final Table teamVisibilityTable = visibilityManager.getTeamVisibilityTable();

        final Column visibilityPkColumn = filterVisibilityTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(filterTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(filterTable.getPKColumn(), ids)));
        selectQuery.addSelectClause(filterTable, providerTable, filterVisibilityTable, userVisibilityTable, teamVisibilityTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(filterTable.getColumn(OMETFILTER.PROVIDER_ID), providerTable.getPKColumn()),
                new JoinClause(filterTable.getColumn(OMETFILTER.FILTER_VISIBILITY_ID), visibilityPkColumn),
                new JoinClause(visibilityPkColumn, userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(visibilityPkColumn, teamVisibilityTable.getColumn(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID), leftJoin))
        );

        dataContainer.append(orgDataStore(orgId).get(selectQuery));
    }

    /**
     * <p>
     * Loads team and territory visibility details into the target container.
     * </p>
     *
     * @param orgId         Represents the organization id.
     * @param dataContainer Holds the retrieved data.
     */
    private void loadVisibilityData(final Long orgId, final DataContainer dataContainer) {
        final Table filterTable = getTable();
        final Table filterVisibilityTable = visibilityManager.getFilterVisibilityTable();
        final Table teamVisibilityTable = visibilityManager.getTeamVisibilityTable();
        final Table territoryVisibilityTable = visibilityManager.getTerritoryVisibilityTable();
        final Table teamTable = visibilityManager.getTeamTable();
        final Table territoryTable = visibilityManager.getTerritoryTable();

        final Collection<Long> visibilityIds = dataContainer.getRows(filterTable)
                .map(row -> (Long) row.get(OMETFILTER.FILTER_VISIBILITY_ID))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (visibilityIds.isEmpty()) {
            return;
        }
        final Column visibilityPkColumn = filterVisibilityTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(filterVisibilityTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(visibilityPkColumn, visibilityIds)));
        selectQuery.addSelectClause(territoryVisibilityTable);
        selectQuery.addSelectClause(List.of(teamTable.getColumn(OMCRMTEAM.NAME), territoryTable.getColumn(OMCRMTERRITORY.NAME)));
        selectQuery.addJoinClause(List.of(
                new JoinClause(visibilityPkColumn, teamVisibilityTable.getColumn(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(teamVisibilityTable.getColumn(METFTEAMVISIBILITY.TEAM_ID), teamTable.getPKColumn(), leftJoin),
                new JoinClause(visibilityPkColumn, territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.TERRITORY_ID), territoryTable.getPKColumn(), leftJoin))
        );

        dataContainer.append(orgDataStore(orgId).get(selectQuery));
    }

    /**
     * <p>
     * Loads filter criteria data into the target container.
     * </p>
     *
     * @param orgId         Represents the organization id.
     * @param ids           Holds the collection of filter ids.
     * @param dataContainer Holds the retrieved data.
     */
    private void loadCriteriaData(final Long orgId, final Collection<Long> ids, final DataContainer dataContainer) {
        final Table filterCriteriaTable = getFilterCriteriaTable();
        final SelectQuery selectQuery = SelectQuery.get(filterCriteriaTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(filterCriteriaTable.getColumn(METFFILTERCRITERIA.FILTER_ID), ids)));
        selectQuery.addSelectClause(filterCriteriaTable);
        final DataContainer container = orgDataStore(orgId).get(selectQuery);

        dataContainer.append(container);
        final Collection<Long> criteriaIds = container.getRows(filterCriteriaTable)
                .map(row -> (Long) row.get(METFFILTERCRITERIA.CRITERIA_ID))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!criteriaIds.isEmpty()) {
            dataContainer.append(buildCriteriaContainer(orgId, true, criteriaIds));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param filter Holds the {@link Filter} attributes.
     * @return {@link Filter} that holds the filter attributes.
     */
    @Override
    public Filter create(final Filter filter) {
        final DataContainer targetContainer = DataContainer.create();

        loadFilterIntoContainer(true, filter, null, targetContainer);
        final Long orgId = filter.orgId();
        final OrgDataStore orgDataStore = filter.isDefault() ? DataStoreProvider.getOrgDataStore(orgId) : orgDataStore(orgId);

        orgDataStore.commitChanges(targetContainer);
        final CrmUser creator = filter.getCreator();

        if (Objects.nonNull(creator) && Filter.Category.COMMON == filter.getCategory()) {
            updateRecentlyAppliedFilter(orgId, creator.id(), filter.getFilterProvider().getId(), filter.id());
        }
        return filter;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId   Represents the organisation id.
     * @param filters Hold the collection of {@link Filter} attributes.
     * @return The response as {@link Map}.
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Filter> filters) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final Collection<Long> ids = filters.stream().map(Filter::id).filter(Objects::nonNull).toList();
        final DataContainer entityContainer = buildDataContainer(orgId, ids);
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();

        filters.forEach(filter -> {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadFilterIntoContainer(false, filter, entityContainer, targetContainer);
                orgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(filter.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Filter update failed Org ID : {} ID : {} reason : {}", orgId, filter.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), CommonErrorCodes.UPDATE_ACTION_FAILED.name(), filter.id().toString()));
            }
        });

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param filters Hold the collection of {@link Filter} attributes.
     */
    @Override
    public void delete(final Long orgId, final Collection<Filter> filters) {
        final Table filterTable = getTable();
        final DataContainer targetContainer = DataContainer.create();
        final Collection<Long> filterIds = filters.stream().map(TlcModel::id).filter(Objects::nonNull).collect(Collectors.toSet());
        final DataContainer sourceContainer = buildDataContainerForDelete(orgId, filterIds);

        sourceContainer.getRows(filterTable).forEach(targetContainer::deleteRow);
        sourceContainer.getRows(visibilityManager.getFilterVisibilityTable()).forEach(targetContainer::deleteRow);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization ID
     * @param criteriaIds Represents the collection of criteria IDs to delete
     */
    @Override
    public void deleteCriteria(final Long orgId, final Collection<Long> criteriaIds) {
        if (Objects.isNull(criteriaIds) || criteriaIds.isEmpty()) {
            return;
        }
        orgDataStore(orgId).delete(getCriteriaTable(), criteriaIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organisation id.
     * @param userId   The ID of the user requesting the filters.
     * @param sourceId Represents the source id.
     * @param limit    Holds the limitation details.
     * @param category The category of the filters to be retrieved.
     * @return collection of {@link Filter}.
     */
    @Override
    public Collection<Filter> getOrgFilters(final Long orgId, final Long userId, final Long sourceId,
                                            final DropdownLimit limit, final Filter.Category category) {
        return getFilters(orgId, userId, null, sourceId, category, limit);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the organisation id.
     * @param currentUserId Represents the current user id
     * @param sourceId      Represents the source id.
     * @param userIds       The collection of user IDs to filter by.
     * @return collection of {@link Filter}.
     */
    @Override
    public Collection<Filter> getSalesOwnerFilters(final Long orgId, final Long currentUserId,
                                                   final Long sourceId, final Collection<Long> userIds) {
        return getFilters(orgId, currentUserId, userIds, sourceId, Filter.Category.SALES_OWNER, null);
    }

    /**
     * <p>
     * Gets the collection of {@link Filter} based on organisation id.
     * </p>
     *
     * @param orgId    Represents the organisation id.
     * @param userId   The ID of the user requesting the filters.
     * @param sourceId Represents the source id.
     * @param limit    Holds the limitation details.
     * @param category The category of the filters to be retrieved.
     * @return collection of {@link Filter}.
     */
    private Collection<Filter> getFilters(final Long orgId, final Long userId, final Collection<Long> userIds,
                                          final Long sourceId, final Filter.Category category, final DropdownLimit limit) {
        final Table filterTable = getTable();
        final Table providerTable = getProviderTable();
        final Table favouriteFilterTable = getFavouriteFilterTable();
        final Table userVisibilityTable = visibilityManager.getUserVisibilityTable();
        final Table filterVisibilityTable = visibilityManager.getFilterVisibilityTable();
        final Table teamVisibilityTable = visibilityManager.getTeamVisibilityTable();
        final Table territoryVisibilityTable = visibilityManager.getTerritoryVisibilityTable();
        final Table userTeamTable = getUserTeamTable();
        final Table teamTable = visibilityManager.getTeamTable();
        final Table territoryTable = visibilityManager.getTerritoryTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final Table activeFilterTable = getActiveFilterTable();
        final Column filterPkColumn = filterTable.getPKColumn();
        final Column teamPKColumn = teamTable.getPKColumn();
        final Column territoryPKColumn = territoryTable.getPKColumn();
        final Column filterVisibilityPkColumn = filterVisibilityTable.getPKColumn();
        final Column userIdColumn = userVisibilityTable.getColumn(METFUSERVISIBILITY.USER_ID);
        final Column visibilityIdColumn = userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID);
        final Column providerIdColumn = filterTable.getColumn(OMETFILTER.PROVIDER_ID);
        final SelectQuery selectQuery = SelectQuery.get(filterTable);
        final SelectQuery derivedQuery = SelectQuery.get(favouriteFilterTable);

        derivedQuery.addSelectClause(List.of(
                favouriteFilterTable.getColumn(OMETFFAVOURITEFILTER.FILTER_ID),
                favouriteFilterTable.getColumn(OMETFFAVOURITEFILTER.IS_FAVORITE),
                favouriteFilterTable.getPKColumn()));
        derivedQuery.setWhereClause(new WhereClause(Criteria.eq(favouriteFilterTable.getColumn(OMETFFAVOURITEFILTER.USER_ID), userId)));
        final SelectQuery visibilitySelectQuery = SelectQuery.get(filterVisibilityTable);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final WhereClause visibilityCriteria = new WhereClause(Criteria.eq(filterVisibilityTable.getColumn(OMETFILTERVISIBILITY.VISIBILITY_ID), Visibility.EVERY_ONE.getId()));
        final Table favouriteFilterDerivedTable = DerivedTable.get(favouriteFilterTable.getName(), favouriteFilterTable.getAlias(), derivedQuery);
        final Column filterIdColumn = favouriteFilterDerivedTable.getColumn(OMETFFAVOURITEFILTER.FILTER_ID);

        WhereClause userTeamTerritoryCriteria = new WhereClause(Criteria.eq(userIdColumn, userId)).or(Criteria.eq(userIdColumn, null))
                .or(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userId)).or(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userId));

        if (Objects.nonNull(userIds)) {
            userTeamTerritoryCriteria = WhereClauseUtil.connect(new WhereClause(Criteria.in(filterTable.getColumn(OMETFILTER.NAME), userIds)), userTeamTerritoryCriteria, Connector.OR);
        }

        visibilitySelectQuery.addSelectClause(filterVisibilityPkColumn);
        visibilitySelectQuery.addJoinClause(List.of(
                new JoinClause(filterVisibilityPkColumn, visibilityIdColumn, leftJoin),
                new JoinClause(filterVisibilityPkColumn, teamVisibilityTable.getColumn(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(teamVisibilityTable.getColumn(METFTEAMVISIBILITY.TEAM_ID), teamPKColumn, leftJoin),
               new JoinClause(teamPKColumn, userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), leftJoin),
                new JoinClause(filterVisibilityPkColumn, territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.TERRITORY_ID), territoryPKColumn, leftJoin),
                new JoinClause(territoryPKColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), leftJoin)));
        visibilitySelectQuery.setWhereClause(visibilityCriteria.or(userTeamTerritoryCriteria));

        selectQuery.addSelectClause(filterTable.getColumn(OMETFILTER.FILTER_VISIBILITY_ID));
        selectQuery.addSelectClause(filterTable.getColumn(OMETFILTER.NAME));
        selectQuery.addSelectClause(filterTable.getColumn(OMETFILTER.IS_SYSTEM_FILTER));
        selectQuery.addSelectClause(favouriteFilterDerivedTable.getColumn(OMETFFAVOURITEFILTER.IS_FAVORITE));
        selectQuery.addSelectClause(filterTable.getColumn(OMETFILTER.ORG_ID));
        selectQuery.addSelectClause(filterTable.getColumn(OMETFILTER.CATEGORY));
        selectQuery.addSelectClause(filterTable.getColumn(OMETFILTER.IS_RECENTLY_APPLIED));
        selectQuery.addSelectClause(filterIdColumn);
        selectQuery.addSelectClause(providerIdColumn);
        selectQuery.addSelectClause(providerTable.getColumn(METDATAPROVIDER.NAME));
        selectQuery.addSelectClause(providerTable.getColumn(METDATAPROVIDER.SOURCE_ID));
        selectQuery.addSelectClause(userVisibilityTable, filterVisibilityTable);

        selectQuery.addJoinClause(List.of(new JoinClause(providerIdColumn, providerTable.getPKColumn()),
                new JoinClause(filterTable.getColumn(OMETFILTER.FILTER_VISIBILITY_ID), filterVisibilityPkColumn),
                new JoinClause(filterVisibilityPkColumn, visibilityIdColumn, leftJoin),
                new JoinClause(filterPkColumn, filterIdColumn, leftJoin)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(providerTable.getColumn(METDATAPROVIDER.SOURCE_ID), sourceId))
                .and(Criteria.eq(filterTable.getColumn(OMETFILTER.CATEGORY), category.getId())).and(Criteria.in(filterVisibilityPkColumn, visibilitySelectQuery)));
        selectQuery.addOrderByClause(new OrderByClause(filterPkColumn));

        if (Objects.nonNull(limit)) {
            selectQuery.setLimitClause(limit.getLimitClause());
        }
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final SelectQuery activeFilterQuery = SelectQuery.get(activeFilterTable);
        final Row providerRow = dataContainer.getRow(providerTable,
                new WhereClause(Criteria.eq(providerTable.getColumn(METDATAPROVIDER.SOURCE_ID), sourceId)));
        final boolean hasActiveFilter;

        if (Objects.isNull(providerRow)) {
            hasActiveFilter = false;
        } else {
            activeFilterQuery.addSelectClause(activeFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.FILTER_ID));
            activeFilterQuery.setWhereClause(new WhereClause(Criteria.eq(activeFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.USER_ID), userId))
                    .and(Criteria.eq(activeFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.PROVIDER_ID), providerRow.getPKValue())));
            final DataContainer activeFilterContainer = orgDataStore(orgId).get(activeFilterQuery);

            hasActiveFilter = 0 != activeFilterContainer.size(activeFilterTable);
            dataContainer.append(activeFilterContainer);
        }

        return dataContainer.getRows(filterTable).map(row -> convertRowToPartialFilter(row, dataContainer, hasActiveFilter)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    Represents the organization id.
     * @param filterId Represents the filter id to modify favourite list.
     * @param isAdd    A flag that represent the filter is favourite or not.
     */
    @Override
    public void updateFavouriteFilter(final Long orgId, final Long filterId, final Long userId, final boolean isAdd) {
        final Table favouriteFilterTable = getFavouriteFilterTable();
        final OrgDataStore orgDataStore = orgDataStore(orgId);
        final DataContainer dataContainer = DataContainer.create();
        final Row oldFavouriteFilterRow = orgDataStore.get(favouriteFilterTable, new WhereClause(Criteria.eq(
                favouriteFilterTable.getColumn(OMETFFAVOURITEFILTER.FILTER_ID), filterId))
                .and(Criteria.eq(favouriteFilterTable.getColumn(OMETFFAVOURITEFILTER.USER_ID), userId))).getRow(favouriteFilterTable);
        final Row favouriteFilterRow = Objects.isNull(oldFavouriteFilterRow) ? new Row(favouriteFilterTable) : oldFavouriteFilterRow;

        if (!exists(orgId, filterId)) {
            throw ErrorCode.get(FilterErrorCodes.FILTER_NOT_FOUND, String.format("ORG ID : %d ID : %d", orgId, filterId));
        }

        if (favouriteFilterRow.isNewRow()) {
            favouriteFilterRow.setOrgId(orgId);
            favouriteFilterRow.set(OMETFFAVOURITEFILTER.USER_ID, userId);
            favouriteFilterRow.set(OMETFFAVOURITEFILTER.FILTER_ID, filterId);
        }
        favouriteFilterRow.set(OMETFFAVOURITEFILTER.IS_FAVORITE, isAdd);

        dataContainer.updateRow(favouriteFilterRow);
        orgDataStore.commitChanges(dataContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @return Map of provider id and their collection of {@link Filter}.
     */
    @Override
    public Map<Long, Collection<Filter>> getDefaultFilters() {
        final Table filterTable = getTable();
        final Table criteriaTable = getCriteriaTable();
        final Table filterCriteriaTable = getFilterCriteriaTable();
        final Table criteriaFieldTable = criteriaFieldManager.getCriteriaFieldTable();
        final Table criteriaOptionTable = criteriaFieldManager.getCriteriaOptionTable();
        final Table criteriaValueTable = getCriteriaValueTable();

        final Column criteriaPkColumn = criteriaTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(filterTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterTable.getColumn(OMETFILTER.ORG_ID), null)));
        selectQuery.addOrderByClause(new OrderByClause(criteriaTable.getPKColumn()));
        selectQuery.addJoinClause(List.of(
                new JoinClause(filterTable.getPKColumn(), filterCriteriaTable.getColumn(METFFILTERCRITERIA.FILTER_ID), leftJoin),
                new JoinClause(filterCriteriaTable.getColumn(METFFILTERCRITERIA.CRITERIA_ID), criteriaPkColumn),
                new JoinClause(criteriaPkColumn, criteriaValueTable.getColumn(METFCRITERIAVALUE.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaPkColumn, criteriaFieldTable.getColumn(METFCRITERIAFIELD.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaPkColumn, criteriaOptionTable.getColumn(METFCRITERIAOPTION.CRITERIA_ID), leftJoin)));

        final DataContainer dataContainer = readableDataStore().get(selectQuery);

        return dataContainer.getRows(filterTable).collect(Collectors.groupingBy(filterRow -> filterRow.get(OMETFILTER.PROVIDER_ID),
                Collectors.mapping(filterRow -> convertRowToFilter(filterRow, dataContainer), Collectors.toCollection(ArrayList::new))));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents the organisation id.
     * @param userId    Represents the user id.
     * @param filterIds Holds the filter ids.
     * @return Map of {@link Filter.Permissions}.
     */
    @Override
    public Map<Long, Filter.Permissions> getPermissions(final Long orgId, final Long userId, final Collection<Long> filterIds) {
        final Table table = getTable();
        final Table filterVisibilityTable = visibilityManager.getFilterVisibilityTable();
        final Table userVisibilityTable = visibilityManager.getUserVisibilityTable();
        final Table teamVisibilityTable = visibilityManager.getTeamVisibilityTable();
        final Table territoryVisibilityTable = visibilityManager.getTerritoryVisibilityTable();
        final Table userTeamTable = getUserTeamTable();
        final Table teamTable = visibilityManager.getTeamTable();
        final Table territoryTable = visibilityManager.getTerritoryTable();
        final Table userTerritoryTable = getUserTerritoryTable();

        final Column teamPKColumn = teamTable.getPKColumn();
        final Column territoryPKColumn = territoryTable.getPKColumn();
        final Column filterVisibilityIdColumn = table.getColumn(OMETFILTER.FILTER_VISIBILITY_ID);
        final Column filterVisibilityPkColumn = filterVisibilityTable.getPKColumn();
        final Column visibilityIdColumn = filterVisibilityTable.getColumn(OMETFILTERVISIBILITY.VISIBILITY_ID);
        final Column preventShareColumn = filterVisibilityTable.getColumn(OMETFILTERVISIBILITY.PREVENT_SHARE);
        final Column permissionIdColumn = filterVisibilityTable.getColumn(OMETFILTERVISIBILITY.PERMISSION_ID);
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(table);
        final WhereClause whereClause = new WhereClause(Criteria.in(table.getPKColumn(), filterIds));

        selectQuery.addSelectClause(userVisibilityTable);
        selectQuery.addSelectClause(List.of(filterVisibilityIdColumn, permissionIdColumn, preventShareColumn, visibilityIdColumn));
        selectQuery.setWhereClause(whereClause.and(new WhereClause(Criteria.eq(visibilityIdColumn, Visibility.EVERY_ONE.getId()))
                .or(new WhereClause(Criteria.eq(userVisibilityTable.getColumn(METFUSERVISIBILITY.USER_ID), userId))
                        .or(new WhereClause(Criteria.eq(userTeamTable.getColumn(MCRMUSERTEAM.USER_ID), userId))
                                .or(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userId))))));
        selectQuery.addJoinClause(List.of(
                new JoinClause(filterVisibilityIdColumn, filterVisibilityPkColumn),
                new JoinClause(filterVisibilityPkColumn, userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(filterVisibilityPkColumn, teamVisibilityTable.getColumn(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(teamVisibilityTable.getColumn(METFTEAMVISIBILITY.TEAM_ID), teamPKColumn, leftJoin),
                new JoinClause(teamPKColumn, userTeamTable.getColumn(MCRMUSERTEAM.TEAM_ID), leftJoin),
                new JoinClause(filterVisibilityPkColumn, territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.TERRITORY_ID), territoryPKColumn, leftJoin),
                new JoinClause(territoryPKColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), leftJoin)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).collect(Collectors.toMap(Row::getPKValue, row -> {
            final Long filterVisibilityId = row.get(OMETFILTER.FILTER_VISIBILITY_ID);
            final Row filterVisibilityRow = dataContainer.getRow(filterVisibilityTable, filterVisibilityId);
            final Short visibilityPermissionId = filterVisibilityRow.get(OMETFILTERVISIBILITY.PERMISSION_ID);
            final Visibility visibility = Visibility.get(filterVisibilityRow.get(OMETFILTERVISIBILITY.VISIBILITY_ID));

            if (Visibility.JUST_ME == visibility && Objects.isNull(visibilityPermissionId)) {
                return new Filter.Permissions();
            } else {
                final Column column = userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID);
                final WhereClause userVisibilityCriteria = new WhereClause(Criteria.eq(column, filterVisibilityId))
                        .and(Criteria.eq(userVisibilityTable.getColumn(METFUSERVISIBILITY.IS_CREATOR), true))
                        .and(Criteria.eq(userVisibilityTable.getColumn(METFUSERVISIBILITY.USER_ID), userId));
                final Stream<Row> userVisibilityRows = dataContainer.getRows(userVisibilityTable, userVisibilityCriteria);

                return userVisibilityRows.findAny().isPresent() ? new Filter.Permissions() : getPermissions(visibilityPermissionId, filterVisibilityRow);
            }
        }));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents the organisation id.
     * @param ids   Holds the filter ids.
     * @return Map of {@link FilterVisibility}.
     */
    @Override
    public Map<Long, FilterVisibility> getVisibility(final Long orgId, final Collection<Long> ids) {
        final Table filterTable = getTable();
        final Table filterVisibilityTable = visibilityManager.getFilterVisibilityTable();
        final Table userVisibilityTable = visibilityManager.getUserVisibilityTable();
        final Table teamVisibilityTable = visibilityManager.getTeamVisibilityTable();
        final Table territoryVisibilityTable = visibilityManager.getTerritoryVisibilityTable();

        final Column filterVisibilityIdColumn = filterTable.getColumn(OMETFILTER.FILTER_VISIBILITY_ID);
        final Column visibilityPkColumn = filterVisibilityTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(filterTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(filterTable.getPKColumn(), ids)));
        selectQuery.addSelectClause(filterVisibilityIdColumn);
        selectQuery.addSelectClause(filterVisibilityTable, userVisibilityTable, teamVisibilityTable, territoryVisibilityTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(filterVisibilityIdColumn, visibilityPkColumn),
                new JoinClause(visibilityPkColumn, userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(visibilityPkColumn, teamVisibilityTable.getColumn(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(visibilityPkColumn, territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.FILTER_VISIBILITY_ID), leftJoin)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(filterTable).collect(Collectors.toMap(Row::getPKValue, filterRow -> {
            final Row filterVisibilityRow = dataContainer.getRow(filterVisibilityTable, (Long) filterRow.get(OMETFILTER.FILTER_VISIBILITY_ID));

            return visibilityManager.convertRowToVisibility(filterVisibilityRow, dataContainer);
        }));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organisation id.
     * @param criteriaIds Holds the criteria ids.
     * @return Collection of {@link FilterCriteria}.
     */
    @Override
    public Collection<FilterCriteria> getCriteria(final Long orgId, final Collection<Long> criteriaIds) {
        if (criteriaIds.isEmpty()) {
            return List.of();
        }
        final Table criteriaTable = getCriteriaTable();
        final DataContainer dataContainer = getCriteriaContainer(orgId, criteriaIds);

        return dataContainer.getRows(criteriaTable).map(criteriaRow -> convertRowToCriteria(orgId, criteriaRow, dataContainer,
                Objects.isNull(orgId), false)).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization id.
     * @param criteriaIds Holds the criteria ids.
     * @return {@link DataContainer} that holds criteria rows and child rows.
     */
    @Override
    public DataContainer getCriteriaContainer(final Long orgId, final Collection<Long> criteriaIds) {
        return buildCriteriaContainer(orgId, false, criteriaIds);
    }

    /**
     * {@inheritDoc}
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param filterCriteria  Holds criteria attributes.
     * @param sourceContainer Holds existing criteria rows.
     * @param targetContainer Holds updated criteria rows.
     */
    @Override
    public void loadCriteriaIntoContainer(final boolean isCreateEvent, final FilterCriteria filterCriteria,
                                          final DataContainer sourceContainer, final DataContainer targetContainer) {
        loadCriteriaIntoContainer(isCreateEvent, null, false, filterCriteria, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Builds criteria source rows with bounded joins. Each query keeps join count below four.
     * </p>
     *
     * @param orgId              Represents the organization id.
     * @param requireSecureFetch Represents whether to fetch secure or not.
     * @param criteriaIds        Holds the criteria ids.
     * @return {@link DataContainer} that holds criteria rows.
     */
    private DataContainer buildCriteriaContainer(final Long orgId, final boolean requireSecureFetch, final Collection<Long> criteriaIds) {
        final DataContainer dataContainer = DataContainer.create();

        loadCriteriaSourceData(orgId, requireSecureFetch, criteriaIds, dataContainer);
        loadAdditionalCriteriaFieldData(orgId, requireSecureFetch, criteriaIds, dataContainer);
        loadAdditionalCriteriaOptionData(orgId, requireSecureFetch, criteriaIds, dataContainer);

        return dataContainer;
    }

    /**
     * <p>
     * Loads criteria, value, field and option data into the target container.
     * </p>
     *
     * @param orgId              Represents the organization id.
     * @param requireSecureFetch Represents whether to fetch secure or not.
     * @param criteriaIds        Holds the criteria ids.
     * @param dataContainer      Holds the retrieved data.
     */
    private void loadCriteriaSourceData(final Long orgId, final boolean requireSecureFetch, final Collection<Long> criteriaIds, final DataContainer dataContainer) {
        final Table criteriaTable = getCriteriaTable();
        final Table valueTable = getCriteriaValueTable();
        final Table criteriaFieldTable = criteriaFieldManager.getCriteriaFieldTable();
        final Table criteriaOptionTable = criteriaFieldManager.getCriteriaOptionTable();

        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(criteriaTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(criteriaTable.getPKColumn(), criteriaIds)));
        selectQuery.addSelectClause(criteriaTable, valueTable, criteriaFieldTable, criteriaOptionTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(criteriaTable.getPKColumn(), valueTable.getColumn(METFCRITERIAVALUE.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaTable.getPKColumn(), criteriaFieldTable.getColumn(METFCRITERIAFIELD.CRITERIA_ID), leftJoin),
                new JoinClause(criteriaTable.getPKColumn(), criteriaOptionTable.getColumn(METFCRITERIAOPTION.CRITERIA_ID), leftJoin))
        );

        dataContainer.append(requireSecureFetch ? orgDataStore(orgId).get(selectQuery) : DataStoreProvider.getOrgDataStore(orgId).get(selectQuery));
    }

    /**
     * <p>
     * Loads additional criteria field data into the target container.
     * </p>
     *
     * @param orgId              Represents the organization id.
     * @param requireSecureFetch Represents whether to fetch secure or not.
     * @param criteriaIds        Holds the criteria ids.
     * @param dataContainer      Holds the retrieved data.
     */
    private void loadAdditionalCriteriaFieldData(final Long orgId, final boolean requireSecureFetch, final Collection<Long> criteriaIds,
                                                 final DataContainer dataContainer) {
        final Table criteriaFieldTable = criteriaFieldManager.getCriteriaFieldTable();
        final Table additionalFieldTable = criteriaFieldManager.getCriteriaAdditionalFieldTable();

        final SelectQuery selectQuery = SelectQuery.get(criteriaFieldTable);

        selectQuery.setWhereClause(new WhereClause(
                Criteria.in(criteriaFieldTable.getColumn(METFCRITERIAFIELD.CRITERIA_ID), criteriaIds))
        );
        selectQuery.addSelectClause(criteriaFieldTable, additionalFieldTable);
        selectQuery.addJoinClause(
                new JoinClause(criteriaFieldTable.getPKColumn(),
                additionalFieldTable.getColumn(METFCRITERIAADDITIONALFIELD.CRITERIA_FIELD_ID), JoinClause.JoinType.LEFT)
        );

        dataContainer.append(requireSecureFetch ? orgDataStore(orgId).get(selectQuery) : DataStoreProvider.getOrgDataStore(orgId).get(selectQuery));
    }

    /**
     * <p>
     * Loads additional criteria option data into the target container.
     * </p>
     *
     * @param orgId              Represents the organization id.
     * @param requireSecureFetch Represents whether to fetch secure or not.
     * @param criteriaIds        Holds the criteria ids.
     * @param dataContainer      Holds the retrieved data.
     */
    private void loadAdditionalCriteriaOptionData(final Long orgId, final boolean requireSecureFetch, final Collection<Long> criteriaIds, final DataContainer dataContainer) {
        final Table criteriaOptionTable = criteriaFieldManager.getCriteriaOptionTable();
        final Table additionalOptionTable = criteriaFieldManager.getCriteriaAdditionalOptionTable();

        final SelectQuery selectQuery = SelectQuery.get(criteriaOptionTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(criteriaOptionTable.getColumn(METFCRITERIAOPTION.CRITERIA_ID), criteriaIds)));
        selectQuery.addSelectClause(criteriaOptionTable, additionalOptionTable);
        selectQuery.addJoinClause(new JoinClause(
                criteriaOptionTable.getPKColumn(),
                additionalOptionTable.getColumn(METFCRITERIAADDITIONALOPTION.CRITERIA_OPTION_ID),
                JoinClause.JoinType.LEFT)
        );

        dataContainer.append(requireSecureFetch ? orgDataStore(orgId).get(selectQuery) : DataStoreProvider.getOrgDataStore(orgId).get(selectQuery));
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organisation id.
     * @param userId     Represents the user id.
     * @param providerId Represents the provider id.
     * @param filterId   Represents the filter id.
     */
    @Override
    public void updateRecentlyAppliedFilter(final Long orgId, final Long userId, final Long providerId, final Long filterId) {
        final Table activeFilterTable = getActiveFilterTable();
        final SelectQuery selectQuery = SelectQuery.get(activeFilterTable);
        final OrgDataStore orgDataStore = orgDataStore(orgId);

        selectQuery.setWhereClause(new WhereClause(Criteria.eq(activeFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.USER_ID), userId))
                .and(Criteria.eq(activeFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.PROVIDER_ID), providerId)));
        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final Row oldRow = dataContainer.getRow(activeFilterTable);

        if (Objects.isNull(oldRow)) {
            final Row row = new Row(activeFilterTable);

            row.setOrgId(orgId);
            row.set(OMETFRECENTLYAPPLIEDFILTER.USER_ID, userId);
            row.set(OMETFRECENTLYAPPLIEDFILTER.PROVIDER_ID, providerId);
            row.set(OMETFRECENTLYAPPLIEDFILTER.FILTER_ID, filterId);
            orgDataStore.addRow(row);
        } else if (!Objects.equals(filterId, oldRow.get(OMETFRECENTLYAPPLIEDFILTER.FILTER_ID))) {
            oldRow.set(OMETFRECENTLYAPPLIEDFILTER.FILTER_ID, filterId);
            dataContainer.updateRow(oldRow);
            orgDataStore.commitChanges(dataContainer);
        }
    }

    /**
     * <p>
     * Removes the sales owner filter from the DB.
     * </p>
     *
     * @param orgId   Represents the organisation id.
     * @param userIds Represents the user ids.
     */
    @Override
    public void deleteSalesOwnerFilter(final Long orgId, final Collection<Long> userIds) {
        final Table filterTable = getTable();
        final Table filterCriteriaTable = getFilterCriteriaTable();
        final Table valueTable = getCriteriaValueTable();
        final Table criteriaTable = getCriteriaTable();
        final Table criteriaOptionTable = criteriaFieldManager.getCriteriaOptionTable();
        final Table criteriaFieldTable = criteriaFieldManager.getCriteriaFieldTable();
        final Column criteriaPkColumn = criteriaTable.getPKColumn();

        final Collection<CriteriaField> salesOwnerFields = filterFieldService.getFieldsByName(orgId, FilterFields.SALES_OWNER.getName(), true);
        final Collection<CriteriaField> assigneeFields = filterFieldService.getFieldsByName(orgId, ActivityFields.ASSIGNEE_ID.getName(), false);

        final WhereClause whereClause = new WhereClause(Criteria.eq(filterTable.getColumn(OMETFILTER.CATEGORY), Filter.Category.SALES_OWNER.getId()))
                .and(new WhereClause(Criteria.in(criteriaFieldTable.getColumn(METFCRITERIAFIELD.FIELD_ID), salesOwnerFields.stream().map(CriteriaField::getId).toList()))
                        .or(Criteria.in(criteriaOptionTable.getColumn(METFCRITERIAOPTION.OPTION_ID), assigneeFields.stream().map(CriteriaField::getId).toList())))
                .and(Criteria.in(valueTable.getColumn(METFCRITERIAVALUE.VALUE_ENTITY_ID), userIds));
        final SelectQuery selectQuery = buildSelectQueryForDelete(whereClause);

        selectQuery.addJoinClause(new JoinClause(filterTable.getPKColumn(), filterCriteriaTable.getColumn(METFFILTERCRITERIA.FILTER_ID)));
        selectQuery.addJoinClause(new JoinClause(filterCriteriaTable.getColumn(METFFILTERCRITERIA.CRITERIA_ID), criteriaPkColumn));
        selectQuery.addJoinClause(new JoinClause(criteriaPkColumn, criteriaFieldTable.getColumn(METFCRITERIAFIELD.CRITERIA_ID)));
        selectQuery.addJoinClause(new JoinClause(criteriaPkColumn, criteriaOptionTable.getColumn(METFCRITERIAOPTION.CRITERIA_ID)));
        selectQuery.addJoinClause(new JoinClause(criteriaPkColumn, valueTable.getColumn(METFCRITERIAVALUE.CRITERIA_ID)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();

        dataContainer.getRows(filterTable).forEach(targetContainer::deleteRow);
        dataContainer.getRows(visibilityManager.getFilterVisibilityTable()).forEach(targetContainer::deleteRow);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId     Represents an organization id
     * @param userId    Represents the user id
     * @param filterIds Represents the filter ids
     */
    @Override
    public void storeActiveFilters(final long orgId, final long userId, final Map<Long, Long> filterIds) {
        final Table activeFilterTable = getActiveFilterTable();
        final DataContainer dataContainer = DataContainer.create();

        for (final Map.Entry<Long, Long> entry : filterIds.entrySet()) {
            final Row row = new Row(activeFilterTable);

            row.setOrgId(orgId);
            row.set(OMETFRECENTLYAPPLIEDFILTER.USER_ID, userId);
            row.set(OMETFRECENTLYAPPLIEDFILTER.FILTER_ID, entry.getValue());
            row.set(OMETFRECENTLYAPPLIEDFILTER.PROVIDER_ID, entry.getKey());
            dataContainer.addNewRow(row);
        }

        orgDataStore(orgId).commitChanges(dataContainer);
    }

    /**
     * <p>
     * Builds the {@link DataContainer} which contains the {@link Filter} entries for the given id and organisation id.
     * </p>
     *
     * @param orgId Represents the organisation id.
     * @param ids   Which contains the collection of filter ids.
     * @return {@link DataContainer} that hold the retrieved data.
     */
    private DataContainer buildDataContainerForDelete(final Long orgId, final Collection<Long> ids) {
        final Table filterTable = getTable();
        final WhereClause whereClause = new WhereClause(Criteria.in(filterTable.getPKColumn(), ids));
        final SelectQuery selectQuery = buildSelectQueryForDelete(whereClause);

        return orgDataStore(orgId).get(selectQuery);
    }

    private SelectQuery buildSelectQueryForDelete(final WhereClause whereClause) {
        final Table filterTable = getTable();
        final Table filterVisibilityTable = visibilityManager.getFilterVisibilityTable();
        final SelectQuery selectQuery = SelectQuery.get(filterTable);

        selectQuery.setWhereClause(whereClause);
        selectQuery.addSelectClause(filterTable, filterVisibilityTable);
        selectQuery.addJoinClause(new JoinClause(filterTable.getColumn(OMETFILTER.FILTER_VISIBILITY_ID), filterVisibilityTable.getPKColumn()));

        return selectQuery;
    }

    /**
     * <p>
     * Loads the filter details into container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param filter          An entity that holds the {@link Filter} attributes.
     * @param sourceContainer Holds the user defined values of existing filter.
     * @param targetContainer Holds the updated information of user defined values.
     */
    private void loadFilterIntoContainer(final boolean isCreateEvent, final Filter filter,
                                         final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table filterTable = getTable();
        final Long id = filter.id();
        final Long orgId = filter.orgId();
        final CrmUser creator = filter.getCreator();
        final FilterVisibility filterVisibility = filter.getFilterVisibility();
        final Collection<FilterCriteria> criteriaList = filter.getCriteriaList();
        final Long time = Instant.now().toEpochMilli();
        final Row filterRow = isCreateEvent ? new Row(filterTable) : sourceContainer.getRow(filterTable, id);

        if (!isCreateEvent && Objects.isNull(filterRow)) {
            throw ErrorCode.get(FilterErrorCodes.FILTER_NOT_FOUND, String.format("ORG ID : %d ID : %d", orgId, id));
        }
        final Set<Long> unmappedUserIds = new HashSet<>();
        final Long providerId = filter.getFilterProvider().getId();

        visibilityManager.loadVisibilityIntoContainer(orgId, isCreateEvent, filterVisibility, sourceContainer, targetContainer, unmappedUserIds);

        if (filterRow.isNewRow()) {
            filterRow.setOrgId(orgId);
            filterRow.set(OMETFILTER.CREATED_TIME, time);
            filter.setId(filterRow.getPKValue());
            filterRow.set(OMETFILTER.IS_RECENTLY_APPLIED, filter.isRecentlyApplied());
            filterRow.set(OMETFILTER.CATEGORY, filter.getCategory().getId());

            if (Objects.nonNull(creator)) {
                visibilityManager.loadUserVisibilityIntoContainer(true, true, filterVisibility.getId(), creator, sourceContainer, targetContainer);
            }
        } else {
            filterRow.set(OMETFILTER.MODIFIED_TIME, time);
        }
        filterRow.set(OMETFILTER.NAME, filter.getName());
        filterRow.set(OMETFILTER.ALIAS_NAME, filter.getAliasName());
        filterRow.set(OMETFILTER.IS_SYSTEM_FILTER, filter.isDefault());
        filterRow.set(OMETFILTER.PROVIDER_ID, providerId);
        filterRow.set(OMETFILTER.FILTER_VISIBILITY_ID, filterVisibility.getId());
        targetContainer.updateRow(filterRow);

        if (Objects.nonNull(criteriaList)) {
            for (final FilterCriteria criteria : criteriaList) {
                loadCriteriaIntoContainer(isCreateEvent, filter.id(), criteria, sourceContainer, targetContainer);
            }

            if (!isCreateEvent) {
                deleteUnmappedCriteriaEntities(id, criteriaList, targetContainer);
            }
        }

        if (!isCreateEvent && !unmappedUserIds.isEmpty()) {
            final Table activeFilterTable = getActiveFilterTable();

            unmappedUserIds.forEach(userId -> {
                final Row row = sourceContainer.getRow(activeFilterTable, new WhereClause(Criteria.eq(activeFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.USER_ID), userId))
                        .and(Criteria.eq(activeFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.PROVIDER_ID), providerId))
                        .and(Criteria.eq(activeFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.FILTER_ID), filter.id())));

                if (Objects.nonNull(row)) {
                    targetContainer.deleteRow(row);
                }
            });
        }
    }

    /**
     * <p>
     * Loads the filterCriteria details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param filterId        Represents the id of {@link Filter}.
     * @param filterCriteria  An entity that holds the {@link FilterCriteria} attributes.
     * @param sourceContainer Holds the old information of filter values.
     * @param targetContainer Holds the updated information of filter values.
     */
    private void loadCriteriaIntoContainer(final boolean isCreateEvent, final Long filterId, final FilterCriteria filterCriteria,
                                           final DataContainer sourceContainer, final DataContainer targetContainer) {
        loadCriteriaIntoContainer(isCreateEvent, filterId, true, filterCriteria, sourceContainer, targetContainer);
    }

    /**
     * <p>
     * Loads criteria details into the target container.
     * </p>
     *
     * @param isCreateEvent     A flag which indicates whether the action is related to create event or update event.
     * @param filterId          Represents the id of {@link Filter} when criteria belongs to a filter.
     * @param loadFilterMapping A flag which indicates whether METFilterCriteria mapping should be loaded.
     * @param filterCriteria    An entity that holds the {@link FilterCriteria} attributes.
     * @param sourceContainer   Holds the old information of filter values.
     * @param targetContainer   Holds the updated information of filter values.
     */
    private void loadCriteriaIntoContainer(final boolean isCreateEvent, final Long filterId, final boolean loadFilterMapping,
                                           final FilterCriteria filterCriteria, final DataContainer sourceContainer,
                                           final DataContainer targetContainer) {
        final Long criteriaId = filterCriteria.getId();
        final FilterConnector filterConnector = filterCriteria.getConnector();
        final Row criteriaRow = fetchCriteriaRow(isCreateEvent, criteriaId, sourceContainer);

        if (Objects.nonNull(filterConnector)) {
            criteriaRow.set(METFCRITERIA.CONNECTOR_ID, filterConnector.getId());
        }
        criteriaRow.set(METFCRITERIA.SOURCE_ID, filterCriteria.getFieldSource().id());
        criteriaRow.set(METFCRITERIA.COMPARATOR_ID, filterCriteria.getComparator().getId());

        if (criteriaRow.isNewRow()) {
            filterCriteria.setId(criteriaRow.getPKValue());
        }
        targetContainer.updateRow(criteriaRow);

        if (loadFilterMapping && Objects.nonNull(filterId)) {
            loadFilterCriteriaIntoContainer(isCreateEvent, filterId, filterCriteria, sourceContainer, targetContainer);
        }

        criteriaFieldManager.loadFieldIntoContainer(isCreateEvent, filterCriteria, sourceContainer, targetContainer);
        loadValueIntoContainer(isCreateEvent, filterCriteria, sourceContainer, targetContainer);

        if (!isCreateEvent) {
            deleteUnmappedCriteriaDetails(criteriaId, filterCriteria, targetContainer);
        }
    }

    /**
     * <p>
     * Loads the decoupled filter-to-criteria mapping row.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param filterId        Represents the id of {@link Filter}.
     * @param filterCriteria  An entity that holds the {@link FilterCriteria} attributes.
     * @param sourceContainer Holds the old information of filter values.
     * @param targetContainer Holds the updated information of filter values.
     */
    private void loadFilterCriteriaIntoContainer(final boolean isCreateEvent, final Long filterId, final FilterCriteria filterCriteria,
                                                 final DataContainer sourceContainer,
                                                 final DataContainer targetContainer) {
        final Table filterCriteriaTable = getFilterCriteriaTable();
        final Long criteriaId = filterCriteria.getId();
        final Row filterCriteriaRow;

        if (isCreateEvent || Objects.isNull(sourceContainer)) {
            filterCriteriaRow = new Row(filterCriteriaTable);
        } else {
            final Row oldRow = sourceContainer.getRow(filterCriteriaTable, new WhereClause(Criteria.eq(
                    filterCriteriaTable.getColumn(METFFILTERCRITERIA.FILTER_ID), filterId))
                    .and(Criteria.eq(filterCriteriaTable.getColumn(METFFILTERCRITERIA.CRITERIA_ID), criteriaId)));

            filterCriteriaRow = Objects.nonNull(oldRow) ? oldRow : new Row(filterCriteriaTable);
        }
        filterCriteriaRow.set(METFFILTERCRITERIA.FILTER_ID, filterId);
        filterCriteriaRow.set(METFFILTERCRITERIA.CRITERIA_ID, criteriaId);

        targetContainer.updateRow(filterCriteriaRow);
    }

    /**
     * <p>
     * Loads the value details into the target container.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param filterCriteria  An entity that holds the {@link FilterCriteria} attributes.
     * @param sourceContainer Holds the old information of filter values.
     * @param targetContainer Holds the updated information of filter values.
     */
    private void loadValueIntoContainer(final boolean isCreateEvent, final FilterCriteria filterCriteria,
                                        final DataContainer sourceContainer, final DataContainer targetContainer) {
        final List<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();

        if (Objects.nonNull(criteriaValues)) {
            criteriaValues.forEach(criteriaValue -> {
                final Table valueTable = getCriteriaValueTable();
                final Row criteriaValueRow;

                if (isCreateEvent) {
                    criteriaValueRow = new Row(valueTable);
                } else {
                    final Row oldRow = sourceContainer.getRow(valueTable, new WhereClause(Criteria.eq(
                            valueTable.getColumn(METFCRITERIAVALUE.CRITERIA_ID), filterCriteria.getId()))
                            .and(Criteria.eq(valueTable.getPKColumn(), criteriaValue.getId())));

                    criteriaValueRow = Objects.nonNull(oldRow) ? oldRow : new Row(valueTable);
                }
                criteriaValueRow.set(METFCRITERIAVALUE.CRITERIA_ID, filterCriteria.getId());
                criteriaValueRow.set(METFCRITERIAVALUE.VALUE_ENTITY_ID, criteriaValue.getValueEntityId());
                criteriaValueRow.set(METFCRITERIAVALUE.VALUE, criteriaValue.getValue());
                criteriaValue.setId(criteriaValueRow.getPKValue());

                targetContainer.updateRow(criteriaValueRow);
            });
        }
    }

    /**
     * <p>
     * Fetches the {@link Row} of the criteria.
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create event or update event.
     * @param criteriaId      Represents the id of criteria.
     * @param sourceContainer Holds the old information of filter values.
     * @return {@link Row} that holds the values of criteria.
     */
    private Row fetchCriteriaRow(final boolean isCreateEvent, final Long criteriaId, final DataContainer sourceContainer) {
        final Table criteriaTable = getCriteriaTable();
        final Row criteriaRow;

        if (isCreateEvent || Objects.isNull(criteriaId)) {
            criteriaRow = new Row(criteriaTable);
        } else {
            final Row oldRow = sourceContainer.getRow(criteriaTable, criteriaId);

            if (Objects.isNull(oldRow)) {
                throw ErrorCode.get(FilterErrorCodes.CRITERIA_NOT_FOUND, String.format(" ID : %d", criteriaId));
            } else {
                criteriaRow = oldRow;
            }
        }

        return criteriaRow;
    }

    /**
     * <p>
     * Removes the unmapped criteria details.
     * </p>
     *
     * @param criteriaId      Represents the criteria id to delete the unmapped criteria details.
     * @param targetContainer that holds the updated information of filter.
     */
    private void deleteUnmappedCriteriaDetails(final Long criteriaId, final FilterCriteria filterCriteria,
                                               final DataContainer targetContainer) {
        final List<CriteriaValue> criteriaValues = filterCriteria.getCriteriaValues();
        final CriteriaField criteriaField = filterCriteria.getCriteriaField();
        final AdditionalOption additionalOption = criteriaField.getAdditionalOption();
        final Collection<Long> additionalOptionsIds = new HashSet<>();

        if (Objects.nonNull(additionalOption)) {
            collectAdditionalOptionIds(additionalOptionsIds, additionalOption);
        }
        deleteUnmappedFields(criteriaId, criteriaField, targetContainer);
        deleteUnmappedAdditionalOptions(criteriaField, criteriaId, additionalOptionsIds, targetContainer);
        deleteUnmappedValueEntities(criteriaId, Objects.isNull(criteriaValues) ? Set.of()
                : criteriaValues.stream().map(CriteriaValue::getId).collect(Collectors.toSet()), targetContainer);
    }

    /**
     * <p>
     * Collects the additional option ids in given {@link Collection}.
     * </p>
     *
     * @param additionalOptionIds Holds the additional option ids.
     * @param additionalOption    Holds the details of {@link AdditionalOption}.
     */
    private void collectAdditionalOptionIds(final Collection<Long> additionalOptionIds, final AdditionalOption additionalOption) {
        final AdditionalOption childAdditionalOption = additionalOption.getAdditionalOption();

        additionalOptionIds.add(additionalOption.getId());

        if (Objects.nonNull(childAdditionalOption)) {
            collectAdditionalOptionIds(additionalOptionIds, childAdditionalOption);
        }
    }

    /**
     * <p>
     * Removes the unmapped field entities.
     * </p>
     *
     * @param criteriaId      Represents the id of the criteria.
     * @param targetContainer Holds the updated information of filter.
     */
    private void deleteUnmappedFields(final Long criteriaId, final CriteriaField criteriaField,
                                      final DataContainer targetContainer) {
        final Table criteriaFieldTable = criteriaFieldManager.getCriteriaFieldTable();
        final Table criteriaOptionTable = criteriaFieldManager.getCriteriaOptionTable();
        final Column fieldCriteriaIdColumn = criteriaFieldTable.getColumn(METFCRITERIAFIELD.CRITERIA_ID);
        final Column optionCriteriaIdColumn = criteriaOptionTable.getColumn(METFCRITERIAOPTION.CRITERIA_ID);
        final Column fieldIdColumn = criteriaFieldTable.getColumn(METFCRITERIAFIELD.FIELD_ID);
        final Column optionIdColumn = criteriaOptionTable.getColumn(METFCRITERIAOPTION.OPTION_ID);
        final Long criteriaFieldId = criteriaField.getId();

        final WhereClause fieldsWhereClause;
        final WhereClause optionsWhereClause;

        if (criteriaField.isField()) {
            fieldsWhereClause = new WhereClause(Criteria.eq(fieldCriteriaIdColumn, criteriaId))
                    .and(Criteria.notEq(fieldIdColumn, criteriaFieldId));
            optionsWhereClause = new WhereClause(Criteria.eq(optionCriteriaIdColumn, criteriaId));
        } else {
            fieldsWhereClause = new WhereClause(Criteria.eq(fieldCriteriaIdColumn, criteriaId));
            optionsWhereClause = new WhereClause(Criteria.eq(optionCriteriaIdColumn, criteriaId))
                    .and(Criteria.notEq(optionIdColumn, criteriaFieldId));
        }
        delete(criteriaFieldTable, targetContainer, fieldsWhereClause);
        delete(criteriaOptionTable, targetContainer, optionsWhereClause);
    }

    /**
     * <p>
     * Removes the unmapped additional option entities.
     * </p>
     *
     * @param criteriaField       Represents the criteria field/option.
     * @param additionalOptionIds Represents the additional option id.
     * @param criteriaId          Represents the criteria id.
     * @param targetContainer     Holds the updated information of filter.
     */
    private void deleteUnmappedAdditionalOptions(final CriteriaField criteriaField, final Long criteriaId,
                                                 final Collection<Long> additionalOptionIds, final DataContainer targetContainer) {
        final Table criteriaTable;
        final Table additionalTable;
        final Column criteriaIdColumn;

        if (criteriaField.isField()) {
            criteriaTable = this.criteriaFieldManager.getCriteriaFieldTable();
            additionalTable = this.criteriaFieldManager.getCriteriaAdditionalFieldTable();
            criteriaIdColumn = criteriaTable.getColumn(METFCRITERIAFIELD.CRITERIA_ID);
        } else {
            criteriaTable = this.criteriaFieldManager.getCriteriaOptionTable();
            additionalTable = this.criteriaFieldManager.getCriteriaAdditionalOptionTable();
            criteriaIdColumn = criteriaTable.getColumn(METFCRITERIAOPTION.CRITERIA_ID);
        }
        final Collection<Long> retainedIds = additionalOptionIds.stream()
                .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
        WhereClause whereClause = new WhereClause(Criteria.eq(criteriaIdColumn, criteriaId));

        if (!retainedIds.isEmpty()) {
            whereClause = whereClause.and(Criteria.notIn(additionalTable.getPKColumn(), retainedIds));
        }
        final SelectQuery selectQuery = SelectQuery.get(criteriaTable);

        selectQuery.addSelectClause(additionalTable);
        selectQuery.addJoinClause(DmlUtil.getJoins(criteriaTable, additionalTable));
        selectQuery.setWhereClause(whereClause);

        this.readableDataStore().get(selectQuery).getRows(additionalTable).forEach(targetContainer::deleteRow);
    }

    /**
     * <p>
     * Removes the unmapped criteria value entities.
     * </p>
     *
     * @param criteriaId       Represents the criteria id.
     * @param criteriaValueIds Represents the value ids.
     * @param targetContainer  Holds the updated information of filter.
     */
    private void deleteUnmappedValueEntities(final Long criteriaId, final Collection<Long> criteriaValueIds,
                                             final DataContainer targetContainer) {
        final Table valuesTable = getCriteriaValueTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(valuesTable.getColumn(METFCRITERIAVALUE.CRITERIA_ID),
                criteriaId));

        delete(valuesTable, targetContainer, criteriaValueIds.isEmpty() ? whereClause
                : whereClause.and(Criteria.notIn(valuesTable.getPKColumn(), criteriaValueIds)));
    }

    /**
     * <p>
     * Removes the unmapped criteria entities.
     * </p>
     *
     * @param filterId           Represents the filter id for delete the unmapped criteria entities.
     * @param filterCriteriaList list of {@link FilterCriteria} instance.
     * @param dataContainer      that holds the updated information of filter values.
     */
    private void deleteUnmappedCriteriaEntities(final Long filterId, final Collection<FilterCriteria> filterCriteriaList,
                                                final DataContainer dataContainer) {
        final Table criteriaTable = getCriteriaTable();
        final Table filterCriteriaTable = getFilterCriteriaTable();
        final Collection<Long> criteriaIds = filterCriteriaList.stream().map(FilterCriteria::getId).collect(Collectors.toUnmodifiableSet());
        final WhereClause filterCriteriaWhereClause = new WhereClause(Criteria.eq(filterCriteriaTable.getColumn(METFFILTERCRITERIA.FILTER_ID),
                filterId)).and(Criteria.notIn(filterCriteriaTable.getColumn(METFFILTERCRITERIA.CRITERIA_ID), criteriaIds));
        final Collection<Long> unmappedCriteriaIds = readableDataStore().get(filterCriteriaTable, filterCriteriaWhereClause).getRows(filterCriteriaTable)
                .map(row -> (Long) row.get(METFFILTERCRITERIA.CRITERIA_ID)).toList();

        delete(filterCriteriaTable, dataContainer, filterCriteriaWhereClause);
        if (!unmappedCriteriaIds.isEmpty()) {
            delete(criteriaTable, dataContainer, new WhereClause(Criteria.in(criteriaTable.getPKColumn(), unmappedCriteriaIds)));
        }
    }

    /**
     * <p>
     * Removes the unmapped entities.
     * </p>
     *
     * @param dataContainer holds the updated information of filter.
     * @param whereClause   {@link WhereClause}.
     */
    private void delete(final Table table, final DataContainer dataContainer, final WhereClause whereClause) {
        final DataContainer rowsContainer = readableDataStore().get(table, whereClause);

        if (!rowsContainer.isEmpty()) {
            final Stream<Row> rows = rowsContainer.getRows(table);

            rows.forEach(row -> {
                if (Optional.ofNullable(dataContainer.getRow(table, row.getPKValue())).isEmpty()) {
                    dataContainer.deleteRow(row);
                }
            });
        }
    }

    /**
     * <p>
     * Builds data container for update with all filter-related data, respecting 4-JOIN limit.
     * </p>
     *
     * @param orgId Represents the organization id.
     * @param ids   Represents the filter ids to retrieve.
     * @return {@link DataContainer} that holds all filter-related data.
     */
    private DataContainer buildDataContainer(final Long orgId, final Collection<Long> ids) {
        final DataContainer dataContainer = DataContainer.create();

        loadFilterVisibilityData(orgId, ids, dataContainer);
        loadCriteriaData(orgId, ids, dataContainer);

        return dataContainer;
    }

    /**
     * <p>
     * Loads filter, visibility and recently applied filter data into the target container.
     * </p>
     *
     * @param orgId         Represents the organization id.
     * @param ids           Represents the filter ids to retrieve.
     * @param dataContainer Holds the retrieved data.
     */
    private void loadFilterVisibilityData(final Long orgId, final Collection<Long> ids, final DataContainer dataContainer) {
        final Table filterTable = getTable();
        final Table filterVisibilityTable = visibilityManager.getFilterVisibilityTable();
        final Table userVisibilityTable = visibilityManager.getUserVisibilityTable();
        final Table teamVisibilityTable = visibilityManager.getTeamVisibilityTable();
        final Table territoryVisibilityTable = visibilityManager.getTerritoryVisibilityTable();
        final Table recentlyAppliedFilterTable = getActiveFilterTable();

        final Column visibilityPkColumn = filterVisibilityTable.getPKColumn();
        final JoinClause.JoinType leftJoin = JoinClause.JoinType.LEFT;
        final SelectQuery selectQuery = SelectQuery.get(filterTable);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(filterTable.getPKColumn(), ids)));
        selectQuery.addSelectClause(filterTable, filterVisibilityTable, userVisibilityTable, teamVisibilityTable, territoryVisibilityTable,
                recentlyAppliedFilterTable);
        selectQuery.addJoinClause(List.of(
                new JoinClause(filterTable.getColumn(OMETFILTER.FILTER_VISIBILITY_ID), visibilityPkColumn),
                new JoinClause(visibilityPkColumn, userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(visibilityPkColumn, teamVisibilityTable.getColumn(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(visibilityPkColumn, territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.FILTER_VISIBILITY_ID), leftJoin),
                new JoinClause(filterTable.getPKColumn(), recentlyAppliedFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.FILTER_ID), leftJoin))
        );

        dataContainer.append(orgDataStore(orgId).get(selectQuery));
    }

    /**
     * <p>
     * Converts the partial {@link Row} into {@link Filter}.
     * </p>
     *
     * @param row           Which contains the values of filter.
     * @param dataContainer Holds the retrieved data.
     * @return {@link Filter} that holds the values of filter.
     */
    private Filter convertRowToPartialFilter(final Row row, final DataContainer dataContainer, final boolean hasActiveFilter) {
        final Filter filter = new Filter(Filter.Category.get(row.get(OMETFILTER.CATEGORY)));
        final Long filterId = row.getPKValue();
        final Table filterVisibilityTable = visibilityManager.getFilterVisibilityTable();
        final Table userVisibilityTable = visibilityManager.getUserVisibilityTable();
        final Table favouriteFilterTable = getFavouriteFilterTable();
        final Table activeFilterTable = getActiveFilterTable();
        final Row activeFilterRow = dataContainer.getRow(activeFilterTable,
                new WhereClause(Criteria.eq(activeFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.FILTER_ID), filterId)));
        final Row favouriteFilterRow = dataContainer.getRow(favouriteFilterTable,
                new WhereClause(Criteria.eq(favouriteFilterTable.getColumn(OMETFFAVOURITEFILTER.FILTER_ID), filterId)));
        final Row visibilityRow = dataContainer.getRow(filterVisibilityTable, (Long) row.get(OMETFILTER.FILTER_VISIBILITY_ID));
        final Row creatorRow = dataContainer.getRow(userVisibilityTable, new WhereClause(Criteria.eq(
                userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID), row.get(OMETFILTER.FILTER_VISIBILITY_ID)))
                .and(Criteria.eq(userVisibilityTable.getColumn(METFUSERVISIBILITY.IS_CREATOR), true)));
        final Short permissionId = visibilityRow.get(OMETFILTERVISIBILITY.PERMISSION_ID);
        final Row providerRow = dataContainer.getRow(getProviderTable(), (Long) row.get(OMETFILTER.PROVIDER_ID));

        filter.setId(filterId);
        filter.setName(row.get(OMETFILTER.NAME));
        filter.setAliasName(row.get(OMETFILTER.ALIAS_NAME));
        filter.setDefault(row.get(OMETFILTER.IS_SYSTEM_FILTER));
        filter.setRecentlyApplied(hasActiveFilter ? Objects.nonNull(activeFilterRow) : row.get(OMETFILTER.IS_RECENTLY_APPLIED));
        filter.setFilterProvider(convertRowToProvider(providerRow));
        filter.setModifiedTime(row.get(OMETFILTER.MODIFIED_TIME));

        if (Objects.nonNull(creatorRow)) {
            filter.setCreator(userService.getUser(row.getOrgId(), creatorRow.get(METFUSERVISIBILITY.USER_ID)));
        }

        if (Objects.nonNull(favouriteFilterRow)) {
            filter.setFavorite(favouriteFilterRow.get(OMETFFAVOURITEFILTER.IS_FAVORITE));
        }

        if (Objects.nonNull(permissionId)) {
            filter.setPermissions(getPermissions(permissionId, visibilityRow));
        }

        return filter;
    }

    /**
     * <p>
     * Gets the {@link Filter.Permissions}.
     * </p>
     *
     * @param permissionId  Represents the permission id.
     * @param visibilityRow Represents the visibility row.
     * @return The {@link Filter.Permissions}
     */
    private Filter.Permissions getPermissions(final short permissionId, final Row visibilityRow) {
        final VisibilityPermission visibilityPermission = VisibilityPermission.get(permissionId);

        return new Filter.Permissions(visibilityPermission.isEditable(), visibilityPermission.isDeletable(),
                !(boolean) visibilityRow.get(OMETFILTERVISIBILITY.PREVENT_SHARE));
    }

    /**
     * <p>
     * Converts the filter {@link Row} into {@link Filter} instance.
     * </p>
     *
     * @param row           which contains the values of filter.
     * @param dataContainer that holds the retrieved data.
     * @return {@link Filter} instance that holds the values of filter.
     */
    private Filter convertRowToFilter(final Row row, final DataContainer dataContainer) {
        final Filter filter = new Filter(Filter.Category.get(row.get(OMETFILTER.CATEGORY)));
        final Table userVisibilityTable = visibilityManager.getUserVisibilityTable();
        final Table filterCriteriaTable = getFilterCriteriaTable();
        final Table favouriteFilterTable = getFavouriteFilterTable();
        final Table providerTable = getProviderTable();
        final Long filterId = row.getPKValue();
        final Long orgId = row.getOrgId();
        final Long providerId = row.get(OMETFILTER.PROVIDER_ID);
        final boolean isDefault = row.get(OMETFILTER.IS_SYSTEM_FILTER);
        final Row providerRow = dataContainer.getRow(providerTable, providerId);
        final List<Row> filterCriteriaRows = dataContainer.getRows(filterCriteriaTable,
                new WhereClause(Criteria.eq(filterCriteriaTable.getColumn(METFFILTERCRITERIA.FILTER_ID), filterId))).toList();
        final Row favouriteFilterRow = dataContainer.getRow(favouriteFilterTable,
                new WhereClause(Criteria.eq(favouriteFilterTable.getColumn(OMETFFAVOURITEFILTER.FILTER_ID), filterId)));
        final Row filterVisibilityRow = dataContainer.getRow(visibilityManager.getFilterVisibilityTable(),
                (Long) row.get(OMETFILTER.FILTER_VISIBILITY_ID));
        final Row userVisibilityRow = dataContainer.getRow(userVisibilityTable, new WhereClause(Criteria.eq(userVisibilityTable.getColumn(
                METFUSERVISIBILITY.FILTER_VISIBILITY_ID), row.get(OMETFILTER.FILTER_VISIBILITY_ID))).and(Criteria
                .eq(userVisibilityTable.getColumn(METFUSERVISIBILITY.IS_CREATOR), true)));

        filter.setId(filterId);
        filter.setOrgId(orgId);
        filter.setName(row.get(OMETFILTER.NAME));
        filter.setAliasName(row.get(OMETFILTER.ALIAS_NAME));
        filter.setRecentlyApplied(row.get(OMETFILTER.IS_RECENTLY_APPLIED));
        filter.setDefault(isDefault);
        filter.setCreatedTime(row.get(OMETFILTER.CREATED_TIME));
        filter.setModifiedTime(row.get(OMETFILTER.MODIFIED_TIME));
        filter.setCriteriaList(filterCriteriaRows.stream().map(filterCriteriaRow -> convertMappingRowToCriteria(orgId, filterCriteriaRow, dataContainer,
                Objects.isNull(orgId))).toList());

        if (Objects.nonNull(userVisibilityRow)) {
            filter.setCreator(userService.getUser(orgId, userVisibilityRow.get(METFUSERVISIBILITY.USER_ID)));
        }

        if (Objects.isNull(providerRow) && Objects.nonNull(providerId)) {
            final FilterProvider provider = new FilterProvider();

            provider.setId(providerId);
            filter.setFilterProvider(provider);
        } else {
            filter.setFilterProvider(convertRowToProvider(providerRow));
        }

        if (Objects.nonNull(filterVisibilityRow)) {
            filter.setFilterVisibility(visibilityManager.convertRowToVisibility(filterVisibilityRow, dataContainer));
        }

        if (Objects.nonNull(favouriteFilterRow)) {
            filter.setFavorite(favouriteFilterRow.get(OMETFFAVOURITEFILTER.IS_FAVORITE));
        }

        return filter;
    }

    /**
     * <p>
     * Converts the row into {@link FilterProvider}.
     * </p>
     *
     * @param row Holds the details of the provider.
     * @return {@link FilterProvider}.
     */
    private FilterProvider convertRowToProvider(final Row row) {
        final FilterProvider provider = new FilterProvider();

        provider.setId(row.getPKValue());
        provider.setProvider(Provider.valueOf(row.get(METDATAPROVIDER.NAME)));
        provider.setFieldSource(filterSourceService.get(row.get(METDATAPROVIDER.SOURCE_ID)));

        return provider;
    }

    /**
     * <p>
     * Converts the criteria {@link Row} into {@link FilterCriteria} instance.
     * </p>
     *
     * @param row                 which contains the values of criteria.
     * @param dataContainer       that holds the retrieved data.
     * @param isDefault           A flag that indicates whether the criteria row is default or not.
     * @param requiredSecureFetch Represents whether to fetch secure or not.
     * @return {@link FilterCriteria} instance that holds the values of criteria.
     */
    private FilterCriteria convertRowToCriteria(final Long orgId, final Row row, final DataContainer dataContainer,
                                                final boolean isDefault, final boolean requiredSecureFetch) {
        final FilterCriteria filterCriteria = new FilterCriteria();
        final Long criteriaId = row.getPKValue();
        final Table valueTable = getCriteriaValueTable();
        final Integer connectorId = row.get(METFCRITERIA.CONNECTOR_ID);
        final Long sourceId = row.get(METFCRITERIA.SOURCE_ID);
        final Long comparatorId = row.get(METFCRITERIA.COMPARATOR_ID);
        final Table criteriaFieldTable = criteriaFieldManager.getCriteriaFieldTable();
        final Table criteriaOptionTable = criteriaFieldManager.getCriteriaOptionTable();

        final Row criteriaFieldRow = dataContainer.getRow(criteriaFieldTable, new WhereClause(Criteria.eq(criteriaFieldTable.getColumn(
                METFCRITERIAFIELD.CRITERIA_ID), criteriaId)));
        final Row criteriaOptionRow = dataContainer.getRow(criteriaOptionTable, new WhereClause(Criteria.eq(criteriaOptionTable.getColumn(
                METFCRITERIAOPTION.CRITERIA_ID), criteriaId)));
        final Stream<Row> valueRows = dataContainer.getRows(valueTable, new WhereClause(Criteria.eq(valueTable.getColumn(
                METFCRITERIAVALUE.CRITERIA_ID), criteriaId)));
        final boolean isField = Objects.isNull(criteriaOptionRow);

        filterCriteria.setId(criteriaId);
        filterCriteria.setFieldSource(filterSourceService.get(sourceId));
        filterCriteria.setComparator(comparatorService.get(comparatorId));

        if (isDefault) {
            final CriteriaField criteriaField;

            if (isField) {
                criteriaField = new CriteriaField();

                criteriaField.setId(criteriaFieldRow.get(METFCRITERIAFIELD.FIELD_ID));
            } else {
                criteriaField = new CriteriaOption();

                criteriaField.setId(criteriaOptionRow.get(METFCRITERIAOPTION.OPTION_ID));
            }
            filterCriteria.setCriteriaField(criteriaField);
        } else {
            if (isField && Objects.isNull(criteriaFieldRow)) {
                throw ErrorCode.get(FilterErrorCodes.FILTER_FIELD_NOT_FOUND);
            }
            filterCriteria.setCriteriaField(criteriaFieldManager.getCriteriaField(orgId, criteriaId, isField, requiredSecureFetch));
        }

        if (Objects.nonNull(connectorId)) {
            filterCriteria.setConnector(FilterConnector.get(connectorId));
        }

        if (Objects.nonNull(valueRows)) {
            valueRows.forEach(valueRow -> filterCriteria.addCriteriaValue(convertRowToValue(valueRow)));
        }

        return filterCriteria;
    }

    /**
     * <p>
     * Converts the filter-to-criteria mapping row into {@link FilterCriteria}.
     * </p>
     *
     * @param orgId             Represents the organization id.
     * @param filterCriteriaRow Which contains the values of filter-to-criteria mapping.
     * @param dataContainer     Holds the retrieved data.
     * @param isDefault         A flag that indicates whether the criteria row is default or not.
     * @return {@link FilterCriteria} instance that holds the values of criteria.
     */
    private FilterCriteria convertMappingRowToCriteria(final Long orgId, final Row filterCriteriaRow,
                                                       final DataContainer dataContainer, final boolean isDefault) {
        final Row criteriaRow = dataContainer.getRow(getCriteriaTable(), (Long) filterCriteriaRow.get(METFFILTERCRITERIA.CRITERIA_ID));

        return convertRowToCriteria(orgId, criteriaRow, dataContainer, isDefault, true);
    }

    /**
     * <p>
     * Converts the value {@link Row} into {@link CriteriaValue} instance.
     * </p>
     *
     * @param row which contains the values of {@link CriteriaValue}.
     * @return {@link CriteriaValue} instance that holds the values of value.
     */
    private CriteriaValue convertRowToValue(final Row row) {
        final CriteriaValue criteriaValue = new CriteriaValue();
        final Long valueEntityId = row.get(METFCRITERIAVALUE.VALUE_ENTITY_ID);

        criteriaValue.setId(row.getPKValue());
        criteriaValue.setValue(row.get(METFCRITERIAVALUE.VALUE));

        if (Objects.nonNull(valueEntityId)) {
            criteriaValue.setValueEntityId(valueEntityId);
        }

        return criteriaValue;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId represents the organization ID
     * @param ids   which contains the collection of ids
     * @return collection of {@link Filter}.
     */
    @Override
    public Collection<Filter> partialGet(final Long orgId, final Collection<Long> ids) {
        return null;
    }

    /**
     * <p>
     * Gets the filter id by using provider id and aliasName
     * </p>
     *
     * @param orgId      Represents the organisation Id
     * @param providerId Represents the provider Id
     * @param aliasName  Represents the filter alias name
     * @return The filter id
     */
    @Override
    public Long getFilterId(final Long orgId, final Long providerId, final String aliasName) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, new WhereClause(Criteria.eq(table.getColumn(OMETFILTER.ALIAS_NAME), aliasName)),
                List.of(table.getPKColumn())).getRow(table);

        return Objects.isNull(row) ? null : row.getPKValue();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents the organisation ID
     * @return the optional of user id
     */
    @Override
    public Optional<Long> getRecentlyAppliedFilterUserId(final Long orgId, final Long userId, final Long sourceId) {
        final Table filterTable = getTable();
        final Table recentlyAppliedFilterTable = getActiveFilterTable();
        final Table providerTable = getProviderTable();
        final SelectQuery selectQuery = SelectQuery.get(filterTable);
        final Column filterTablePKColumn = filterTable.getPKColumn();

        selectQuery.addSelectClause(List.of(filterTable.getColumn(OMETFILTER.NAME), filterTablePKColumn));
        selectQuery.addJoinClause(List.of(new JoinClause(filterTablePKColumn, recentlyAppliedFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.FILTER_ID)),
                new JoinClause(filterTable.getColumn(OMETFILTER.PROVIDER_ID), providerTable.getPKColumn())));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(filterTable.getColumn(OMETFILTER.CATEGORY), Filter.Category.SALES_OWNER.getId()))
                .and(Criteria.eq(providerTable.getColumn(METDATAPROVIDER.SOURCE_ID), sourceId))
                .and(Criteria.eq(recentlyAppliedFilterTable.getColumn(OMETFRECENTLYAPPLIEDFILTER.USER_ID), userId)));

        final Row filterRow = orgDataStore(orgId).get(selectQuery).getRow(filterTable);

        return Optional.ofNullable(filterRow).map(row -> NumberUtils.parseNumber(row.get(OMETFILTER.NAME)).longValue());
    }
}