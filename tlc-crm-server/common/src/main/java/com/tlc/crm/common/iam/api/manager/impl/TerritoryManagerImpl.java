package com.tlc.crm.common.iam.api.manager.impl;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.internal.manager.AbstractIamConfigManager;
import com.tlc.crm.common.iam.internal.IamErrorCodes;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.common.sql.resource.MCRMUSERTERRITORY;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.iam.client.dropdown.SearchFilter;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * A {@link TerritoryManagerImpl} that provides the various methods for the {@link TerritoryManager}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TerritoryManagerImpl extends AbstractIamConfigManager<Territory> implements TerritoryManager {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TerritoryManagerImpl.class);

    private final CrmUserManagerImpl crmUserManager;
    private final TeamManagerImpl teamManager;
    private final UserService userService;

    private TerritoryManagerImpl() {
        crmUserManager = CrmUserManagerImpl.getInstance();
        teamManager = TeamManagerImpl.getInstance();
        userService = UserService.getInstance();
    }

    private static class Instance {
        private static final TerritoryManagerImpl INSTANCE = new TerritoryManagerImpl();
    }

    /**
     * <p>
     * Gets the {@link TerritoryManagerImpl} instance
     * </p>
     *
     * @return The instance of {@link TerritoryManagerImpl}
     */
    public static TerritoryManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @return The territory {@link Table}
     */
    @Override
    public Table getTable() {
        return Table.get(OMCRMTERRITORY.TABLE);
    }

    /**
     * <p>
     * Gets the user territory table
     * </p>
     *
     * @return The user territory {@link Table}
     */
    public Table getUserTerritoryTable() {
        return Table.get(MCRMUSERTERRITORY.TABLE);
    }

    /**
     * <p>
     * Gets the user table
     * </p>
     *
     * @return The user {@link Table}
     */
    private Table getUserTable() {
        return crmUserManager.getTable();
    }

    /**
     * {@inheritDoc}
     *
     * @param territory Represents the territory details
     * @return The {@link Territory} details
     */
    @Override
    public Territory create(final Territory territory) {
        final DataContainer targetContainer = DataContainer.create();

        loadTerritoryIntoContainer(true, territory, null, targetContainer);
        secureOrgDataStore(territory.orgId()).commitChanges(targetContainer);

        return territory;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param territories Contains the collection of {@link Territory}
     * @return The {@link ProcessOutcome}
     */
    @Override
    public ProcessOutcome partialUpdate(final Long orgId, final Collection<Territory> territories) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();
        final Table territoryTable = getTable();

        for (final Territory territory : territories) {

            try {
                final UpdateQuery updateQuery = UpdateQuery.get(territoryTable);
                final Long territoryId = territory.id();

                updateQuery.addUpdateColumn(territoryTable.getColumn(OMCRMTERRITORY.NAME), territory.getName());
                updateQuery.addUpdateColumn(territoryTable.getColumn(OMCRMTERRITORY.DESCRIPTION), territory.getDescription());
                updateQuery.addUpdateColumn(territoryTable.getColumn(OMCRMTERRITORY.UPDATED_BY), territory.getUpdatedBy().id());
                updateQuery.addUpdateColumn(territoryTable.getColumn(OMCRMTERRITORY.UPDATED_TIME), Instant.now().toEpochMilli());
                updateQuery.setWhereClause(new WhereClause(Criteria.eq(territoryTable.getPKColumn(), territoryId)));

                secureOrgDataStore(orgId).update(updateQuery);
                processOutcome.addProcessedRecord(territoryId);
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Territory update failed Org ID : {} ID : {} reason : {}", orgId, territory.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.TERRITORY_UPDATE_FAILED,
                        "i18n.territory.update.action.failed"), ErrorKeys.UPDATE_ACTION_FAILED, territory.id().toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.TERRITORY_UPDATE_FAILED,
                    "i18n.territory.update.action.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param id    Represents the territory id
     * @return The {@link Territory} details
     */
    @Override
    public Territory get(final Long orgId, final Long id) {
        final Table territoryTable = getTable();
        final Row row = orgDataStore(orgId).get(territoryTable, id);

        if (Objects.isNull(row)) {
            throw ErrorCode.get(IamErrorCodes.TERRITORY_NOT_FOUND, "i18n.territory.not.found");
        }

        return convertRowToModel(row, null, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents the organization id
     * @param territories Contains the collection of models
     * @return The updated response sent
     */
    @Override
    public ProcessOutcome update(final Long orgId, final Collection<Territory> territories) {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntries = new ArrayList<>();
        final Table territoryTable = getTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final SelectQuery selectQuery = SelectQuery.get(territoryTable);
        final Column territoryPkColumn = territoryTable.getPKColumn();
        final Collection<Long> territoryIds = territories.stream().map(Territory::id).toList();

        selectQuery.addSelectClause(territoryTable, userTerritoryTable);
        selectQuery.addJoinClause(new JoinClause(territoryPkColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(territoryPkColumn, territoryIds)));
        final DataContainer sourceContainer = secureOrgDataStore(orgId).get(selectQuery);

        for (final Territory territory : territories) {
            try {
                final DataContainer targetContainer = DataContainer.create();

                loadTerritoryIntoContainer(false, territory, sourceContainer, targetContainer);
                secureOrgDataStore(orgId).commitChanges(targetContainer);
                processOutcome.addProcessedRecord(territory.id());
            } catch (ErrorCode errorCode) {
                LOGGER.error("Update action: Territory update failed Org ID : {} ID : {} reason : {}", orgId, territory.id(), errorCode.getMessage(), errorCode);
                failedEntries.add(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.TERRITORY_UPDATE_FAILED,
                        "i18n.territory.update.action.failed"), ErrorKeys.UPDATE_ACTION_FAILED, territory.id().toString()));
            }
        }

        if (!failedEntries.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(IamErrorCodes.TERRITORY_UPDATE_FAILED,
                    "i18n.territory.update.action.failed")), failedEntries));
        }

        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param territoryId    Represents the territory id
     * @param userStartValue Represents the starting index of user to be fetched
     * @param userLimit      Represents the maximum number of user to be fetched
     * @return The {@link Territory} details
     */
    @Override
    public Territory get(final Long orgId, final Long territoryId, final int userStartValue, final int userLimit) {
        final Table territoryTable = getTable();
        final SelectQuery selectQuery = getSelectQuery(false);

        selectQuery.setLimitClause(new LimitClause(userStartValue, userLimit));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(territoryTable.getPKColumn(), territoryId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return convertRowToModel(dataContainer.getRow(territoryTable), dataContainer, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param start Represents the starting index of territory to be fetched
     * @param limit Represents the maximum number of territory to be fetched
     * @return The collection of {@link Territory} details
     */
    @Override
    public Collection<Territory> get(final Long orgId, final int start, final int limit) {
        final Collection<Territory> territories = new ArrayList<>();
        final Table territoryTable = getTable();
        final SelectQuery innerSelectQuery = SelectQuery.get(territoryTable);
        final Column territoryPkColumn = territoryTable.getPKColumn();
        final SelectQuery selectQuery = getSelectQuery(false);
        final Table userTerritoryTable = getUserTerritoryTable();

        selectQuery.addSelectClause(territoryTable);
        innerSelectQuery.setLimitClause(new LimitClause(start, limit));
        innerSelectQuery.addSelectClause(territoryPkColumn);
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(territoryTable.getColumn(OMCRMTERRITORY.ORG_ID), orgId)));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(territoryPkColumn, innerSelectQuery)));
        selectQuery.addOrderByClause(new OrderByClause(territoryTable.getColumn(OMCRMTERRITORY.CREATED_TIME), OrderByClause.OrderType.ASCENDING));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        dataContainer.getRows(territoryTable).forEach(row -> {
            final Long territoryPkValue = row.getPKValue();
            final Territory territory = new Territory();
            final Collection<Long> userIds = dataContainer.getRows(userTerritoryTable, new WhereClause(Criteria
                    .eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), territoryPkValue))).map(userTerritoryRow ->
                    (Long) userTerritoryRow.get(MCRMUSERTERRITORY.USER_ID)).collect(Collectors.toSet());

            territory.setId(territoryPkValue);
            territory.setName(row.get(OMCRMTERRITORY.NAME));
            territory.setUpdatedTime(row.get(OMCRMTERRITORY.UPDATED_TIME));
            territory.setCreatedTime(row.get(OMCRMTERRITORY.CREATED_TIME));
            territory.setUsersCount((long) userIds.size());
            loadCreatedByAndUpdatedByIntoTerritory(orgId, row.get(OMCRMTERRITORY.CREATED_BY), row.get(OMCRMTERRITORY.UPDATED_BY),
                    territory);
            territories.add(territory);
        });

        return territories;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param id    Represents the territory id
     * @return The total count of users
     */
    @Override
    public long getUsersCount(final Long orgId, final Long id) {
        final Table userTerritoryTable = getUserTerritoryTable();
        final CountQuery countQuery = CountQuery.get(userTerritoryTable);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), id)));

        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param names Contains the collection of territory names
     * @return If any territory is exists it will return true, otherwise it will return false
     */
    @Override
    public boolean checkTerritoriesAreExists(final Long orgId, final Collection<String> names) {
        final Table territoryTable = getTable();
        final CountQuery countQuery = CountQuery.get(territoryTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(territoryTable.getColumn(OMCRMTERRITORY.NAME), names)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId          Represents an organization id
     * @param territoryId    Represents the territory id
     * @param userStartValue Represents the starting index of users to be fetched
     * @param userLimit      Represents the maximum number of users to be fetched
     * @return The collection of {@link CrmUser} details
     */
    @Override
    public Collection<CrmUser> getUsers(final Long orgId, final Long territoryId, final int userStartValue, final int userLimit) {
        final Table userTable = getUserTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final SelectQuery selectQuery = SelectQuery.get(userTable);

        selectQuery.addJoinClause(new JoinClause(userTable.getPKColumn(), userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), territoryId)));
        selectQuery.addSelectClause(userTable);
        selectQuery.setLimitClause(new LimitClause(userStartValue, userLimit));

        return orgDataStore(orgId).get(selectQuery).getRows(userTable).map(crmUserManager::convertRowToModel).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param limitClause Represents the start and limit the territory to be fetched
     * @param criteria    Represents the criteria for territory filtering
     * @return The collection of {@link Territory} details
     */
    @Override
    public Collection<Territory> getTerritoryDropDownList(final Long orgId, final LimitClause limitClause, final Criteria criteria) {
        final Collection<Territory> territories = new ArrayList<>();
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(table.getColumn(OMCRMTERRITORY.NAME));
        selectQuery.setWhereClause(new WhereClause(criteria));
        selectQuery.addOrderByClause(new OrderByClause(table.getPKColumn()));
        selectQuery.setLimitClause(limitClause);
        final Stream<Row> rows = orgDataStore(orgId).get(selectQuery).getRows(table);

        rows.forEach(row -> {
            final Territory territory = new Territory();

            territory.setId(row.getPKValue());
            territory.setName(row.get(OMCRMTERRITORY.NAME));
            territories.add(territory);
        });

        return territories;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param territoryIds Represents the collection of territory ids
     * @return Collection of {@link Long} user ids related to the territories
     */
    @Override
    public Collection<Long> getTerritoryUserIds(final Long orgId, final Collection<Long> territoryIds) {
        final Table territoryTable = getTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final SelectQuery selectQuery = SelectQuery.get(territoryTable);

        selectQuery.addJoinClause(new JoinClause(territoryTable.getPKColumn(), userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID)));
        selectQuery.addSelectClause(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID));
        selectQuery.setWhereClause(new WhereClause(Criteria.in(territoryTable.getPKColumn(), territoryIds)));

        return orgDataStore(orgId).get(selectQuery).getRows(userTerritoryTable).map(row ->
                (Long) row.get(MCRMUSERTERRITORY.USER_ID)).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @return {@link Territory} of the given user.
     */
    @Override
    public Collection<Long> getUserTerritoryIds(final Long orgId, final Long userId) {
        final Table table = getTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final Column pkColumn = table.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(table);

        selectQuery.addSelectClause(pkColumn);
        selectQuery.addJoinClause(new JoinClause(pkColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).map(Row::getPKValue).toList();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param territoryIds Represents the {@link Collection} of {@link Territory} ids
     * @param isPartial    Represents whether partial data required or not
     * @return the {@link Collection} of {@link Territory}
     */
    @Override
    public Collection<Territory> getTerritories(final long orgId, final Collection<Long> territoryIds, final boolean isPartial) {
        final Table territoryTable = getTable();
        final SelectQuery selectQuery = getSelectQuery(isPartial);

        selectQuery.setWhereClause(new WhereClause(Criteria.in(territoryTable.getPKColumn(), territoryIds)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(territoryTable).map(row -> convertRowToModel(row, dataContainer, isPartial))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId  Represents the organisation id.
     * @param userId Represents the user id.
     * @return Collection of user ids.
     */
    @Override
    public Collection<Long> getTerritoryMateIds(final Long orgId, final Long userId) {
        final Table userTerritoryTable = getUserTerritoryTable();
        final SelectQuery innerSelectQuery = SelectQuery.get(userTerritoryTable);
        final SelectQuery selectQuery = SelectQuery.get(userTerritoryTable);
        final Column territoryIdColumn = userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID);
        final Column userIdColumn = userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID);

        innerSelectQuery.addSelectClause(territoryIdColumn);
        innerSelectQuery.setWhereClause(new WhereClause(Criteria.eq(userIdColumn, userId)));
        selectQuery.addSelectClause(userIdColumn);
        selectQuery.setWhereClause(new WhereClause(Criteria.in(territoryIdColumn, innerSelectQuery)).and(Criteria.notEq(userIdColumn, userId)));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(userTerritoryTable).map(row -> (Long) row.get(userIdColumn)).collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId         Represents the organization ID
     * @param territoryName Name of the territory
     * @return the {@link Territory}
     */
    @Override
    public Optional<Territory> getTerritoryByName(final long orgId, final String territoryName) {
        return getTerritoryDropDownList(orgId, new LimitClause(1), Criteria.eq(getTable().getColumn(OMCRMTERRITORY.NAME), territoryName)).stream().findFirst();
    }

    /**
     * <p>
     * Loads the created by and updated by users into territory
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param createdByUserId Represents the created by user id
     * @param updatedByUserId Represents the updated by used id
     * @param territory       Represents the territory details
     */
    private void loadCreatedByAndUpdatedByIntoTerritory(final Long orgId, final Long createdByUserId, final Long updatedByUserId, final Territory territory) {
        final Collection<CrmUser> crmUsers = userService.getUsers(orgId, List.of(createdByUserId, updatedByUserId));

        for (final CrmUser crmUser : crmUsers) {
            final Long userId = crmUser.id();

            if (Objects.equals(userId, createdByUserId)) {
                territory.setCreatedBy(crmUser);
            }

            if (Objects.equals(userId, updatedByUserId)) {
                territory.setUpdatedBy(crmUser);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId Represents an organization id
     * @param ids   Contains the collection of territory ids
     * @param names Contains the collection of territory names
     * @return If any territory is exists it will return true, otherwise it will return false
     */
    public boolean checkTerritoriesAreExists(final Long orgId, final Collection<Long> ids, final Collection<String> names) {
        final Table territoryTable = getTable();
        final CountQuery countQuery = CountQuery.get(territoryTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(territoryTable.getColumn(OMCRMTERRITORY.NAME), names))
                .and(Criteria.notIn(territoryTable.getPKColumn(), ids)));

        return orgDataStore(orgId).get(countQuery) > 0;
    }

    /**
     * <p>
     * Gets the select query for territory
     * </p>
     *
     * @param isPartial  Represents whether partial data required or not
     * @return The {@link SelectQuery}
     */
    private SelectQuery getSelectQuery(final boolean isPartial) {
        final Table territoryTable = getTable();
        final SelectQuery selectQuery = SelectQuery.get(territoryTable);
        final Column territoryNameColumn = territoryTable.getColumn(OMCRMTERRITORY.NAME);

        if (isPartial) {
            selectQuery.addSelectClause(territoryNameColumn);
        } else {
            final Table userTerritoryTable = getUserTerritoryTable();
            final Table userTable = getUserTable();

            selectQuery.addJoinClause(List.of(
                    new JoinClause(territoryTable.getPKColumn(), userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID)),
                    new JoinClause(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userTable.getPKColumn()))
            );
            selectQuery.addSelectClause(userTable, userTerritoryTable);
            selectQuery.addSelectClause(List.of(territoryNameColumn,
                    territoryTable.getColumn(OMCRMTERRITORY.DESCRIPTION),
                    territoryTable.getColumn(OMCRMTERRITORY.ORG_ID))
            );
        }

        return selectQuery;
    }

    /**
     * <p>
     * Converts row into {@link Territory}
     * </p>
     *
     * @param row           Represents the territory row
     * @param dataContainer A container that holds the user defined field values and system values
     * @param isPartial     Represents whether partial data required or not
     * @return The {@link Territory} details
     */
    private Territory convertRowToModel(final Row row, final DataContainer dataContainer, final boolean isPartial) {
        final Territory territory = new Territory();

        territory.setId(row.getPKValue());
        territory.setName(row.get(OMCRMTERRITORY.NAME));

        if (isPartial) {
            return territory;
        }
        final Long orgId = row.getOrgId();
        final Long createdById = row.get(OMCRMTERRITORY.CREATED_BY);
        final Long updatedById = row.get(OMCRMTERRITORY.UPDATED_BY);

        territory.setOrgId(orgId);
        territory.setDescription(row.get(OMCRMTERRITORY.DESCRIPTION));
        territory.setCreatedTime(row.get(OMCRMTERRITORY.CREATED_TIME));
        territory.setUpdatedTime(row.get(OMCRMTERRITORY.UPDATED_TIME));

        if (Objects.nonNull(createdById) && Objects.nonNull(updatedById)) {
            loadCreatedByAndUpdatedByIntoTerritory(orgId, createdById, updatedById, territory);
        }

        if (Objects.nonNull(dataContainer)) {
            final Table userTable = crmUserManager.getTable();
            final Table userTerritoryTable = getUserTerritoryTable();
            final Collection<Long> userIds = dataContainer.getRows(userTerritoryTable, new WhereClause(Criteria
                    .eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), territory.id()))).map(userTerritoryRow ->
                    (Long) userTerritoryRow.get(MCRMUSERTERRITORY.USER_ID)).collect(Collectors.toSet());
            final Stream<Row> userRows = dataContainer.getRows(userTable, new WhereClause(Criteria.in(userTable.getPKColumn(),
                    userIds)));

            userRows.forEach(userRow -> territory.addUser(crmUserManager.convertRowToModel(userRow)));
        }

        return territory;
    }

    /**
     * <p>
     * Loads the territory details into the target container
     * </p>
     *
     * @param isCreateEvent   A flag which indicates whether the action is related to create or update event
     * @param territory       Represents the territory details which have to be saved into database
     * @param targetContainer Holds the field values of existing territory
     * @param sourceContainer Holds the user defined and system field values of existing territory
     */
    private void loadTerritoryIntoContainer(final boolean isCreateEvent, final Territory territory, final DataContainer sourceContainer,
                                            final DataContainer targetContainer) {
        final Collection<CrmUser> users = territory.getCrmUsers();
        final Collection<Team> teams = territory.getTeams();
        final Long currentTime = Instant.now().toEpochMilli();
        final Table territoryTable = getTable();
        final Row territoryRow = isCreateEvent ? new Row(territoryTable) : sourceContainer.getRow(territoryTable,
                new WhereClause(Criteria.eq(territoryTable.getPKColumn(), territory.id())));
        final Long territoryPkValue = territoryRow.getPKValue();
        final Long orgId = territory.orgId();
        final Collection<Long> userIds = new HashSet<>();

        if (territoryRow.isNewRow()) {
            territoryRow.setOrgId(orgId);
            territory.setId(territoryPkValue);
            territoryRow.set(OMCRMTERRITORY.CREATED_TIME, currentTime);
            territoryRow.set(OMCRMTERRITORY.CREATED_BY, territory.getCreatedBy().id());
        }
        territoryRow.set(OMCRMTERRITORY.NAME, territory.getName());
        territoryRow.set(OMCRMTERRITORY.DESCRIPTION, territory.getDescription());
        territoryRow.set(OMCRMTERRITORY.UPDATED_TIME, currentTime);
        territoryRow.set(OMCRMTERRITORY.UPDATED_BY, territory.getUpdatedBy().id());

        if (Objects.nonNull(teams) && !teams.isEmpty()) {
            final Collection<Long> teamIds = teams.stream().map(Team::id).toList();

            userIds.addAll(teamManager.getTeamUserIds(orgId, teamIds));
        }

        if (Objects.nonNull(users) && !users.isEmpty()) {
            userIds.addAll(users.stream().map(CrmUser::id).toList());
        }

        if (!userIds.isEmpty()) {
            loadTerritoryUserIntoContainer(isCreateEvent, territoryPkValue, userIds, sourceContainer, targetContainer);
        }

        if (!isCreateEvent) {
            deleteUnmappedUser(territory, sourceContainer, targetContainer);
        }
        targetContainer.updateRow(territoryRow);
    }

    /**
     * <p>
     * Deletes unmapped territory users of the team in the container
     * </p>
     *
     * @param territory       Represents the {@link Territory} details
     * @param sourceContainer Holds the user defined and system field values of existing territory
     * @param targetContainer Holds the updated information of user defined and system field values
     */
    private void deleteUnmappedUser(final Territory territory, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Collection<CrmUser> crmUsers = territory.getCrmUsers();

        if (Objects.nonNull(crmUsers) && !crmUsers.isEmpty()) {
            final Table userTerritoryTable = getUserTerritoryTable();
            final Collection<Long> userIds = crmUsers.stream().map(CrmUser::id).toList();
            final WhereClause whereClause = new WhereClause(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), territory.id()))
                    .and(Criteria.notIn(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userIds));
            final Stream<Row> rows = sourceContainer.getRows(userTerritoryTable, whereClause);

            rows.forEach(targetContainer::deleteRow);
        }
    }

    /**
     * <p>
     * Loads the user territory details into the target container
     * </p>
     *
     * @param territoryId     Represents the territory id
     * @param userIds         Represents the collection of user ids
     * @param targetContainer Holds the field values of existing user
     */
    private void loadTerritoryUserIntoContainer(final boolean isCreateEvent, final Long territoryId, final Collection<Long> userIds,
                                                final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table userTerritoryTable = getUserTerritoryTable();

        for (final Long userId : userIds) {
            final Row existingRow = isCreateEvent ? null : sourceContainer.getRow(userTerritoryTable, new WhereClause(Criteria
                    .eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), territoryId)).and(Criteria
                    .eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userId)));

            if (Objects.isNull(existingRow)) {
                final Row row = new Row(userTerritoryTable);

                row.set(MCRMUSERTERRITORY.USER_ID, userId);
                row.set(MCRMUSERTERRITORY.TERRITORY_ID, territoryId);
                targetContainer.addNewRow(row);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param territory Holds the details of the territory.
     */
    @Override
    public void linkUsers(final Territory territory) {
        final Long territoryId = territory.id();
        final Collection<Long> userIds = territory.getCrmUsers().stream().map(CrmUser::id).collect(Collectors.toSet());
        final Table territoryTable = getTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final SelectQuery selectQuery = SelectQuery.get(territoryTable);
        final DataContainer targetContainer = DataContainer.create();
        final OrgDataStore orgDataStore = secureOrgDataStore(territory.orgId());
        final Column territoryPkColumn = territoryTable.getPKColumn();

        selectQuery.addSelectClause(territoryTable);
        selectQuery.addSelectClause(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID));
        selectQuery.addJoinClause(new JoinClause(territoryPkColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), JoinClause.JoinType.LEFT));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(territoryPkColumn, territoryId)));
        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final Collection<Long> existingIds = dataContainer.getRows(userTerritoryTable).map(row -> (Long) row.get(MCRMUSERTERRITORY.USER_ID))
                .collect(Collectors.toSet());
        Row territoryRow = null;

        for (final Long userId : userIds) {

            if (!existingIds.contains(userId)) {
                final Row row = new Row(userTerritoryTable);

                row.set(MCRMUSERTERRITORY.TERRITORY_ID, territoryId);
                row.set(MCRMUSERTERRITORY.USER_ID, userId);

                if (Objects.isNull(territoryRow)) {
                    territoryRow = dataContainer.getRow(territoryTable);
                }
                targetContainer.updateRow(row);
            }
        }

        if (Objects.nonNull(territoryRow)) {
            territoryRow.set(OMCRMTERRITORY.UPDATED_TIME, Instant.now().toEpochMilli());
            territoryRow.set(OMCRMTERRITORY.UPDATED_BY, territory.getUpdatedBy().id());
            targetContainer.updateRow(territoryRow);
        }
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param territory Holds the details of the territory.
     */
    @Override
    public void unlinkUsers(final Territory territory) {
        final Table territoryTable = getTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final OrgDataStore orgDataStore = secureOrgDataStore(territory.orgId());
        final Column territoryPkColumn = territoryTable.getPKColumn();
        final SelectQuery selectQuery = SelectQuery.get(territoryTable);

        selectQuery.addJoinClause(new JoinClause(territoryPkColumn, userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(territoryPkColumn, territory.id()))
                .and(Criteria.in(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), territory.getCrmUsers().stream().map(CrmUser::id).toList())));
        final DataContainer dataContainer = orgDataStore.get(selectQuery);
        final DataContainer targetContainer = DataContainer.create();
        final Row territoryRow = dataContainer.getRow(territoryTable);

        if (Objects.nonNull(territoryRow)) {
            territoryRow.set(OMCRMTERRITORY.UPDATED_TIME, Instant.now().toEpochMilli());
            territoryRow.set(OMCRMTERRITORY.UPDATED_BY, territory.getUpdatedBy().id());
            targetContainer.updateRow(territoryRow);
        }
        dataContainer.getRows(userTerritoryTable).forEach(targetContainer::deleteRow);
        orgDataStore.commitChanges(targetContainer);
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represents an organization id
     * @param requiredUserCount A flag indicating whether the territory should include the user count
     * @param limitClause       Represents the start and limit the territory to be fetched
     * @param criteria          Represents the criteria for territory filtering
     * @return The collection of {@link Territory} details
     */
    @Override
    public Collection<Territory> getTerritoryDropDownList( final Long orgId,
                                                           final boolean requiredUserCount,
                                                           final LimitClause limitClause,
                                                           final Criteria criteria )
    {
        final Collection<Territory> territories = new ArrayList<>();
        final Table territoryTable = getTable();
        final Table userTerritoryTable = getUserTerritoryTable();
        final Column territoryPkColumn = territoryTable.getPKColumn();
        final Column territoryNameColumn = territoryTable.getColumn( OMCRMTERRITORY.NAME );
        final Column territoryOrgColumn = territoryTable.getColumn( OMCRMTERRITORY.ORG_ID );
        final Column territoryUserCountColumn = userTerritoryTable.getColumn( MCRMUSERTERRITORY.USER_ID )
                .count();
        final SelectQuery selectQuery = SelectQuery.get( territoryTable );

        selectQuery.addJoinClause( new JoinClause( territoryPkColumn, userTerritoryTable.getColumn( MCRMUSERTERRITORY.TERRITORY_ID ) ) );
        selectQuery.addSelectClause( List.of( territoryUserCountColumn, territoryPkColumn, territoryNameColumn, territoryOrgColumn ) );
        selectQuery.setWhereClause( new WhereClause( criteria ) );
        selectQuery.addOrderByClause( new OrderByClause( territoryPkColumn ) );
        selectQuery.setLimitClause( limitClause );
        selectQuery.addGroupByClause( List.of( new GroupByClause( territoryPkColumn ), new GroupByClause( territoryNameColumn ), new GroupByClause( territoryOrgColumn ) ) );
        final List<Map<String, Object>> territoryDetails = orgDataStore( orgId ).getData( selectQuery );

        if( Objects.nonNull( territoryDetails ) && !territoryDetails.isEmpty() )
        {
            territoryDetails.forEach( territoryDetail ->
            {
                final Territory territory = new Territory();

                if( requiredUserCount )
                {
                    territory.setUsersCount( ( long ) territoryDetail.get( MCRMUSERTERRITORY.USER_ID ) );
                }

                territory.setId( ( long ) territoryDetail.get( OMCRMTERRITORY.ID ) );
                territory.setName( ( String ) territoryDetail.get( OMCRMTERRITORY.NAME ) );
                territories.add( territory );
            } );
        }

        return territories;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organization id
     * @param searchFilter Represents the criteria for territory filtering
     * @return The total territory count
     */
    public long getTerritoriesCount(final Long orgId, final SearchFilter searchFilter) {
        final Table territoryTable = getTable();
        final CountQuery countQuery = CountQuery.get(territoryTable);
        final Column territoryNameColumn = territoryTable.getColumn(OMCRMTERRITORY.NAME);
        final Object value = searchFilter.getValue();

        if (Objects.nonNull(value)) {
            countQuery.setWhereClause(new WhereClause(new Criteria(territoryNameColumn, value,
                    Operator.getOperator(searchFilter.getType()))));
        }
        return orgDataStore(orgId).get(countQuery);
    }

    /**
     * Checks whether the users are part of a territory or not.
     *
     * @param orgId       Represents an organization id.
     * @param territoryId Represents the territory id.
     * @param userIds     Refers the user ids.
     * @return true, if users are all part of territory, otherwise false.
     */
    @Override
    public boolean areUsersPartOfTerritory(final long orgId, final long territoryId, final Collection<Long> userIds) {
        final Table userTerritoryTable = getUserTerritoryTable();
        final CountQuery countQuery = CountQuery.get(userTerritoryTable);

        countQuery.setWhereClause(new WhereClause(Criteria.in(userTerritoryTable.getColumn(MCRMUSERTERRITORY.USER_ID), userIds))
                .and(Criteria.eq(userTerritoryTable.getColumn(MCRMUSERTERRITORY.TERRITORY_ID), territoryId)));

        return secureOrgDataStore(orgId).get(countQuery) == userIds.size();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        The unique identifier of the organization whose territories are to be retrieved.
     * @param searchFilter An optional {@link SearchFilter} containing filtering and pagination criteria;
     *                     may be {@code null} to fetch all teams.
     * @return A collection of {@link Territory} objects matching the specified organization and filter criteria.
     */
    @Override
    public Collection<Territory> getTerritoryList(final long orgId, final SearchFilter searchFilter) {
        final Table table = getTable();
        final Column nameColumn = table.getColumn(OMCRMTERRITORY.NAME);
        final Object value = searchFilter.getValue();
        final SelectQuery selectQuery = SelectQuery.get(table);

        if (Objects.nonNull(value)) {
            selectQuery.setWhereClause(new WhereClause(new Criteria(nameColumn, value, Operator.getOperator(searchFilter.getType()))));
        }

        selectQuery.addSelectClause(List.of(nameColumn));
        selectQuery.addOrderByClause(new OrderByClause(table.getPKColumn()));
        selectQuery.setLimitClause(new LimitClause(searchFilter.getStart(), searchFilter.getLimit()));
        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return dataContainer.getRows(table).map(row -> {
            final Territory territory = new Territory();

            territory.setId(row.getPKValue());
            territory.setName(row.get(OMCRMTERRITORY.NAME));
            return territory;
        }).toList();
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId       Represents an organization id
     * @param territoryId Represents the territory id
     * @return the territory name
     */
    @Override
    public String getName(final long orgId, final long territoryId) {
        final Table table = getTable();
        final Row row = orgDataStore(orgId).get(table, territoryId, List.of(table.getColumn(OMCRMTERRITORY.NAME)));

        return Objects.nonNull(row) ? row.get(OMCRMTERRITORY.NAME) : null;
    }
}
