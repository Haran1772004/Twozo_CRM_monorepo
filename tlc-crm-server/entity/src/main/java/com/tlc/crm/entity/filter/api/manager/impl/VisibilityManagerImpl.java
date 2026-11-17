package com.tlc.crm.entity.filter.api.manager.impl;

import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.manager.TeamManager;
import com.tlc.crm.common.iam.api.manager.TerritoryManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.model.Team;
import com.tlc.crm.common.iam.api.model.Territory;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.crm.common.sql.resource.OMCRMTEAM;
import com.tlc.crm.common.sql.resource.OMCRMTERRITORY;

import com.tlc.crm.entity.filter.api.manager.VisibilityManager;
import com.tlc.crm.entity.filter.api.models.FilterVisibility;
import com.tlc.crm.entity.filter.api.models.Visibility;
import com.tlc.crm.entity.filter.api.models.VisibilityPermission;
import com.tlc.crm.entity.sql.resource.METFTEAMVISIBILITY;
import com.tlc.crm.entity.sql.resource.METFTERRITORYVISIBILITY;
import com.tlc.crm.entity.sql.resource.METFUSERVISIBILITY;
import com.tlc.crm.entity.sql.resource.OMETFILTERVISIBILITY;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 * Provides the {@link Visibility} related DB fetch action.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class VisibilityManagerImpl implements VisibilityManager {

    private final UserService userService;
    private final TeamManager teamManager;
    private final TerritoryManager territoryManager;

    private static class Instance {
        private static final VisibilityManagerImpl INSTANCE = new VisibilityManagerImpl();
    }

    private VisibilityManagerImpl() {
        userService = UserService.getInstance();
        teamManager = TeamManager.getInstance();
        territoryManager = TerritoryManager.getInstance();
    }

    public static VisibilityManagerImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Fetches the table of the filter visibility.
     * </p>
     *
     * @return OMETFilterVisibility table.
     */
    Table getFilterVisibilityTable() {
        return Table.get(OMETFILTERVISIBILITY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the user visibility.
     * </p>
     *
     * @return OMETFUserVisibility table.
     */
    Table getUserVisibilityTable() {
        return Table.get(METFUSERVISIBILITY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the team visibility.
     * </p>
     *
     * @return OMETFTeamVisibility table.
     */
    Table getTeamVisibilityTable() {
        return Table.get(METFTEAMVISIBILITY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the territory visibility.
     * </p>
     *
     * @return OMETFTerritoryVisibility table.
     */
    Table getTerritoryVisibilityTable() {
        return Table.get(METFTERRITORYVISIBILITY.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the team.
     * </p>
     *
     * @return OMCRMTeam table.
     */
    Table getTeamTable() {
        return Table.get(OMCRMTEAM.TABLE);
    }

    /**
     * <p>
     * Fetches the table of the territory.
     * </p>
     *
     * @return OMCRMTerritory table.
     */
    Table getTerritoryTable() {
        return Table.get(OMCRMTERRITORY.TABLE);
    }

    /**
     * {@inheritDoc}
     *
     * @return Default {@link FilterVisibility}.
     */
    @Override
    public FilterVisibility getDefaultVisibility() {
        final FilterVisibility filterVisibility = new FilterVisibility();

        filterVisibility.setVisibility(Visibility.EVERY_ONE);
        filterVisibility.setVisibilityPermission(VisibilityPermission.CAN_VIEW);
        filterVisibility.setPreventShare(true);

        return filterVisibility;
    }

    /**
     * {@inheritDoc}
     *
     * @param filterVisibility Holds the details of visibility of filter.
     */
    @Override
    public void create(final FilterVisibility filterVisibility) {
        final DataContainer targetContainer = DataContainer.create();

        loadVisibilityIntoContainer(filterVisibility.getOrgId(), true, filterVisibility, null, targetContainer, null);
        DataStoreProvider.getSecureOrgDataStore(filterVisibility.getOrgId()).commitChanges(targetContainer);
    }

    /**
     * <p>
     * Loads the {@link FilterVisibility} into {@link DataContainer}.
     * </p>
     *
     * @param orgId            Represents the org id
     * @param isCreateEvent    a flag which indicates whether the action is related to create event or update event.
     * @param filterVisibility an entity that holds the {@link FilterVisibility} attributes.
     * @param sourceContainer  holds the user defined values of existing visibilities.
     * @param targetContainer  holds the updated information of user defined values.
     * @param unmappedUserIds  Represents the collection of unmapped user ids
     */
    void loadVisibilityIntoContainer(final Long orgId, final boolean isCreateEvent, final FilterVisibility filterVisibility,
                                     final DataContainer sourceContainer, final DataContainer targetContainer, final Set<Long> unmappedUserIds) {
        final Table filterVisibilityTable = getFilterVisibilityTable();
        final Visibility visibility = filterVisibility.getVisibility();
        final Collection<CrmUser> users = filterVisibility.getUsers();
        final Collection<Team> teams = filterVisibility.getTeams();
        final Collection<Territory> territories = filterVisibility.getTerritories();
        final VisibilityPermission visibilityPermission = filterVisibility.getVisibilityPermission();
        final Collection<Long> mappedTeamIds = new HashSet<>();
        final Collection<Long> mappedTerritoryIds = new HashSet<>();
        final Collection<Long> mappedUserIds = new HashSet<>();
        final Row row = isCreateEvent ? new Row(filterVisibilityTable) : sourceContainer.getRow(filterVisibilityTable, filterVisibility.getId());

        if (row.isNewRow()) {
            row.setOrgId(filterVisibility.getOrgId());
            filterVisibility.setId(row.getPKValue());
        }

        if (Objects.nonNull(visibilityPermission)) {
            row.set(OMETFILTERVISIBILITY.PERMISSION_ID, visibilityPermission.getId());
        }

        if (Objects.nonNull(visibility)) {
            row.set(OMETFILTERVISIBILITY.VISIBILITY_ID, visibility.getId());
        }
        row.set(OMETFILTERVISIBILITY.PREVENT_SHARE, filterVisibility.isPreventShare());

        targetContainer.updateRow(row);
        final Long filterVisibilityId = filterVisibility.getId();

        if (Objects.nonNull(users)) {
            for (final CrmUser user : users) {
                loadUserVisibilityIntoContainer(isCreateEvent, false, filterVisibilityId, user, sourceContainer, targetContainer);
                mappedUserIds.add(user.id());
            }
        }

        if (Objects.nonNull(teams)) {
            for (final Team team : teams) {
                loadTeamVisibilityIntoContainer(isCreateEvent, filterVisibilityId, team, sourceContainer, targetContainer);
                mappedTeamIds.add(team.id());
            }
        }

        if (Objects.nonNull(territories)) {
            for (final Territory territory : territories) {
                loadTerritoryVisibilityIntoContainer(isCreateEvent, filterVisibilityId, territory, sourceContainer, targetContainer);
                mappedTerritoryIds.add(territory.id());
            }
        }

        if (!isCreateEvent) {
            final Collection<Long> emptyList = Collections.emptyList();
            final Set<Long> allMappedUserIds = new HashSet<>(mappedUserIds);

            if (!mappedTeamIds.isEmpty()) {
                for (final Collection<Long> batchedTeamIds : JavaUtil.splitCollection(mappedTeamIds, NumericalConstant.FIFTY)) {
                    allMappedUserIds.addAll(teamManager.getTeamUserIds(orgId, batchedTeamIds));
                }
            }

            if (!mappedTerritoryIds.isEmpty()) {
                for (final Collection<Long> batchedTerritoryIds : JavaUtil.splitCollection(mappedTerritoryIds, NumericalConstant.TWENTY_FIVE)) {
                    allMappedUserIds.addAll(territoryManager.getTerritoryUserIds(orgId, batchedTerritoryIds));
                }
            }
            deleteUnmappedUserVisibilityEntities(filterVisibilityId, Objects.isNull(users) ?
                    emptyList : mappedUserIds, sourceContainer, targetContainer, unmappedUserIds, allMappedUserIds);
            deleteUnmappedTeamVisibilityEntities(orgId, filterVisibilityId, Objects.isNull(teams)
                    ? emptyList : mappedTeamIds, sourceContainer, targetContainer, unmappedUserIds, allMappedUserIds);
            deleteUnmappedTerritoryVisibilityEntities(orgId, filterVisibilityId, Objects.isNull(territories) ?
                    emptyList : mappedTerritoryIds, sourceContainer, targetContainer, unmappedUserIds, allMappedUserIds);
        }
    }

    /**
     * <p>
     * Removes the unmapped user visibility entities.
     * </p>
     *
     * @param filterVisibilityId For delete the unmapped entries.
     * @param userIds            For delete the unmapped entries.
     * @param sourceContainer    holds the user defined values of existing visibilities.
     * @param targetContainer    holds the updated information of user defined values.
     * @param unmappedUserIds    Represents the collection of unmapped user ids
     * @param mappedUserIds      Represents the set of mapped user ids
     */
    private void deleteUnmappedUserVisibilityEntities(final Long filterVisibilityId, final Collection<Long> userIds,
                                                      final DataContainer sourceContainer, final DataContainer targetContainer,
                                                      final Set<Long> unmappedUserIds, final Set<Long> mappedUserIds) {
        final Table userVisibilityTable = getUserVisibilityTable();
        final WhereClause whereClause = new WhereClause(Criteria
                .eq(userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID), filterVisibilityId))
                .and(Criteria.eq(userVisibilityTable.getColumn(METFUSERVISIBILITY.IS_CREATOR), false));
        final Stream<Row> oldRows;

        if (userIds.isEmpty()) {
            oldRows = sourceContainer.getRows(userVisibilityTable, whereClause);
        } else {
            oldRows = sourceContainer.getRows(userVisibilityTable, whereClause
                    .and(Criteria.notIn(userVisibilityTable.getColumn(METFUSERVISIBILITY.USER_ID), userIds)));
        }

        oldRows.forEach(row -> {
            final Long userId = row.get(METFUSERVISIBILITY.USER_ID);

            if (!mappedUserIds.contains(userId)) {
                unmappedUserIds.add(userId);
            }
            targetContainer.deleteRow(row);
        });
    }

    /**
     * <p>
     * Removes the unmapped team visibility entities.
     * </p>
     *
     * @param orgId              Represents the org id
     * @param filterVisibilityId For delete the unmapped entries.
     * @param teamIds            For delete the unmapped entries.
     * @param sourceContainer    holds the user defined values of existing visibilities.
     * @param targetContainer    holds the updated information of user defined values.
     * @param unmappedUserIds    Represents the collection of unmapped user ids
     * @param mappedUserIds      Represents the set of mapped user ids
     */
    private void deleteUnmappedTeamVisibilityEntities(final Long orgId, final Long filterVisibilityId, final Collection<Long> teamIds,
                                                      final DataContainer sourceContainer, final DataContainer targetContainer,
                                                      final Set<Long> unmappedUserIds, final Set<Long> mappedUserIds) {
        final Table teamVisibilityTable = getTeamVisibilityTable();
        final WhereClause whereClause = new WhereClause(Criteria
                .eq(teamVisibilityTable.getColumn(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID), filterVisibilityId));
        final Stream<Row> oldRows;
        final Collection<Long> deletableTeamIds = new HashSet<>();

        if (teamIds.isEmpty()) {
            oldRows = sourceContainer.getRows(teamVisibilityTable, whereClause);
        } else {
            oldRows = sourceContainer.getRows(teamVisibilityTable, whereClause
                    .and(Criteria.notIn(teamVisibilityTable.getColumn(METFTEAMVISIBILITY.TEAM_ID), teamIds)));
        }

        oldRows.forEach(row -> {
            final Long teamId = row.get(METFTEAMVISIBILITY.TEAM_ID);

            deletableTeamIds.add(teamId);
            targetContainer.deleteRow(row);
        });

        if (!deletableTeamIds.isEmpty()) {
            for (final Collection<Long> batchedTeamIds : JavaUtil.splitCollection(deletableTeamIds, NumericalConstant.FIFTY)) {
                teamManager.getTeamUserIds(orgId, batchedTeamIds).stream().filter(userId -> !mappedUserIds.contains(userId)).forEach(unmappedUserIds::add);
            }
        }
    }

    /**
     * <p>
     * Removes the unmapped territory visibility entities.
     * </p>
     *
     * @param orgId              Represents the org id
     * @param filterVisibilityId For delete the unmapped entries.
     * @param territoryIds       For delete the unmapped entries.
     * @param sourceContainer    holds the user defined values of existing visibilities.
     * @param targetContainer    holds the updated information of user defined values.
     * @param unmappedUserIds    Represents the collection of unmapped user ids
     * @param mappedUserIds      Represents the set of mapped user ids
     */
    private void deleteUnmappedTerritoryVisibilityEntities(final Long orgId, final Long filterVisibilityId, final Collection<Long> territoryIds,
                                                           final DataContainer sourceContainer, final DataContainer targetContainer,
                                                           final Set<Long> unmappedUserIds, final Set<Long> mappedUserIds) {
        final Table territoryVisibilityTable = getTerritoryVisibilityTable();
        final WhereClause whereClause = new WhereClause(Criteria
                .eq(territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.FILTER_VISIBILITY_ID), filterVisibilityId));
        final Collection<Long> deletableTerritoryIds = new HashSet<>();
        final Stream<Row> oldRows;

        if (territoryIds.isEmpty()) {
            oldRows = sourceContainer.getRows(territoryVisibilityTable, whereClause);
        } else {
            oldRows = sourceContainer.getRows(territoryVisibilityTable, whereClause
                    .and(Criteria.notIn(territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.TERRITORY_ID), territoryIds)));
        }

        oldRows.forEach(row -> {
            final Long territoryId = row.get(METFTERRITORYVISIBILITY.TERRITORY_ID);

            deletableTerritoryIds.add(territoryId);
            targetContainer.deleteRow(row);
        });

        if (!deletableTerritoryIds.isEmpty()) {
            for (final Collection<Long> batchedTerritoryIds : JavaUtil.splitCollection(deletableTerritoryIds, NumericalConstant.TWENTY_FIVE)) {
                territoryManager.getTerritoryUserIds(orgId, batchedTerritoryIds).stream().filter(userId -> !mappedUserIds.contains(userId)).forEach(unmappedUserIds::add);
            }
        }
    }

    /**
     * <p>
     * Loads the user visibility into {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent      A flag which indicates whether the action is related to create event or update event.
     * @param filterVisibilityId For maps the user and visibility.
     * @param sourceContainer    Holds the user defined values of existing visibilities.
     * @param targetContainer    Holds the updated information of user defined values.
     */
    void loadUserVisibilityIntoContainer(final boolean isCreateEvent, final boolean isCreator, final Long filterVisibilityId,
                                         final CrmUser user, final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table userVisibilityTable = getUserVisibilityTable();
        final Long userId = user.id();
        final Row row = isCreateEvent ? new Row(userVisibilityTable) : fetchOldUserRow(userId, filterVisibilityId, sourceContainer);

        row.set(METFUSERVISIBILITY.USER_ID, userId);
        row.set(METFUSERVISIBILITY.FILTER_VISIBILITY_ID, filterVisibilityId);
        row.set(METFUSERVISIBILITY.IS_CREATOR, isCreator);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Fetches the old user visibility row in the given source container.
     * </p>
     *
     * @param userId             For fetch the corresponding user visibility row.
     * @param filterVisibilityId For fetch the corresponding user visibility row.
     * @param sourceContainer    Holds the existing visibility entries.
     * @return If old row is null, then new {@link Row} was created and return here or else returns old user row.
     */
    private Row fetchOldUserRow(final Long userId, final Long filterVisibilityId, final DataContainer sourceContainer) {
        final Table userVisibilityTable = getUserVisibilityTable();
        final Row oldRow = sourceContainer.getRow(userVisibilityTable, new WhereClause(Criteria.eq(
                userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID), filterVisibilityId))
                .and(Criteria.eq(userVisibilityTable.getColumn(METFUSERVISIBILITY.USER_ID), userId)));

        return Objects.isNull(oldRow) ? new Row(userVisibilityTable) : oldRow;
    }

    /**
     * <p>
     * Loads the team visibility into {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent      A flag which indicates whether the action is related to create event or update event.
     * @param filterVisibilityId For maps the team and visibility.
     * @param sourceContainer    Holds the user defined values of existing visibilities.
     * @param targetContainer    Holds the updated information of user defined values.
     */
    private void loadTeamVisibilityIntoContainer(final boolean isCreateEvent, final Long filterVisibilityId, final Team team,
                                                 final DataContainer sourceContainer, final DataContainer targetContainer) {
        final Table teamVisibilityTable = getTeamVisibilityTable();
        final Long teamId = team.id();
        final Row row = isCreateEvent ? new Row(teamVisibilityTable) : fetchOldTeamRow(teamId, filterVisibilityId, sourceContainer);

        row.set(METFTEAMVISIBILITY.TEAM_ID, teamId);
        row.set(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID, filterVisibilityId);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Fetches the old team visibility in the given source container.
     * </p>
     *
     * @param teamId             For fetch the corresponding team visibility row.
     * @param filterVisibilityId For fetch the corresponding team visibility row.
     * @param sourceContainer    Holds the existing visibility entries.
     * @return If old row is null, then new {@link Row} was created and return here or else returns old user row.
     */
    private Row fetchOldTeamRow(final Long teamId, final Long filterVisibilityId, final DataContainer sourceContainer) {
        final Table teamVisibilityTable = getTeamVisibilityTable();
        final Row oldRow = sourceContainer.getRow(teamVisibilityTable, new WhereClause(Criteria.eq(
                teamVisibilityTable.getColumn(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID), filterVisibilityId))
                .and(Criteria.eq(teamVisibilityTable.getColumn(METFTEAMVISIBILITY.TEAM_ID), teamId)));

        return Objects.isNull(oldRow) ? new Row(teamVisibilityTable) : oldRow;
    }

    /**
     * <p>
     * Loads the territory visibility into {@link DataContainer}.
     * </p>
     *
     * @param isCreateEvent      A flag which indicates whether the action is related to create event or update event.
     * @param filterVisibilityId For maps the territory and visibility.
     * @param sourceContainer    Holds the user defined values of existing visibilities.
     * @param targetContainer    Holds the updated information of user defined values.
     */
    private void loadTerritoryVisibilityIntoContainer(final boolean isCreateEvent, final Long filterVisibilityId,
                                                      final Territory territory, final DataContainer sourceContainer,
                                                      final DataContainer targetContainer) {
        final Table territoryVisibilityTable = getTerritoryVisibilityTable();
        final Long territoryId = territory.id();
        final Row row = isCreateEvent ? new Row(territoryVisibilityTable)
                : fetchOldTerritoryRow(territoryId, filterVisibilityId, sourceContainer);

        row.set(METFTERRITORYVISIBILITY.TERRITORY_ID, territoryId);
        row.set(METFTERRITORYVISIBILITY.FILTER_VISIBILITY_ID, filterVisibilityId);

        targetContainer.updateRow(row);
    }

    /**
     * <p>
     * Fetches the old territory visibility row in the given source container.
     * </p>
     *
     * @param territoryId        For fetch the corresponding territory visibility row.
     * @param filterVisibilityId For fetch the corresponding territory visibility row.
     * @param sourceContainer    Holds the existing visibility entries.
     * @return If old row is null, then new {@link Row} was created and return here or else returns old user row.
     */
    private Row fetchOldTerritoryRow(final Long territoryId, final Long filterVisibilityId, final DataContainer sourceContainer) {
        final Table territoryVisibilityTable = getTerritoryVisibilityTable();
        final Row oldRow = sourceContainer.getRow(territoryVisibilityTable, new WhereClause(Criteria.eq(
                territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.FILTER_VISIBILITY_ID), filterVisibilityId))
                .and(Criteria.eq(territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.TERRITORY_ID), territoryId)));

        return Objects.isNull(oldRow) ? new Row(territoryVisibilityTable) : oldRow;
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link FilterVisibility}.
     * </p>
     *
     * @param row           Holds the values of {@link FilterVisibility}.
     * @param dataContainer Holds all the retrieved entities.
     * @return Converted {@link FilterVisibility}.
     */
    FilterVisibility convertRowToVisibility(final Row row, final DataContainer dataContainer) {
        final FilterVisibility filterVisibility = new FilterVisibility();
        final Long filterVisibilityId = row.getPKValue();
        final Table userVisibilityTable = getUserVisibilityTable();
        final Table teamVisibilityTable = getTeamVisibilityTable();
        final Table territoryVisibilityTable = getTerritoryVisibilityTable();

        final Short visibilityId = row.get(OMETFILTERVISIBILITY.VISIBILITY_ID);
        final Short permissionId = row.get(OMETFILTERVISIBILITY.PERMISSION_ID);
        final Stream<Row> userVisibilityRows = dataContainer.getRows(userVisibilityTable, new WhereClause(
                Criteria.eq(userVisibilityTable.getColumn(METFUSERVISIBILITY.FILTER_VISIBILITY_ID), filterVisibilityId)));
        final Stream<Row> teamVisibilityRows = dataContainer.getRows(teamVisibilityTable, new WhereClause(
                Criteria.eq(teamVisibilityTable.getColumn(METFTEAMVISIBILITY.FILTER_VISIBILITY_ID), filterVisibilityId)));
        final Stream<Row> territoryVisibilityRows = dataContainer.getRows(territoryVisibilityTable, new WhereClause(Criteria
                .eq(territoryVisibilityTable.getColumn(METFTERRITORYVISIBILITY.FILTER_VISIBILITY_ID), filterVisibilityId)));

        if (Objects.nonNull(permissionId)) {
            filterVisibility.setVisibilityPermission(VisibilityPermission.get(permissionId));
        }

        if (Objects.nonNull(visibilityId)) {
            filterVisibility.setVisibility(Visibility.get(visibilityId));
        }
        filterVisibility.setId(filterVisibilityId);
        filterVisibility.setOrgId(row.getOrgId());
        filterVisibility.setPreventShare(row.get(OMETFILTERVISIBILITY.PREVENT_SHARE));
        filterVisibility.setUsers(userVisibilityRows.filter(userVisibilityRow -> !(boolean) userVisibilityRow.get(METFUSERVISIBILITY.IS_CREATOR))
                .map(userVisibilityRow -> userService.getUser(row.getOrgId(), userVisibilityRow.get(METFUSERVISIBILITY.USER_ID))).toList());
        teamVisibilityRows.forEach(teamVisibilityRow -> {
            final Team team = convertRowToTeam(teamVisibilityRow, dataContainer);

            filterVisibility.addTeam(team);
        });
        territoryVisibilityRows.forEach(territoryRow -> {
            final Territory territory = convertRowToTerritory(territoryRow, dataContainer);

            filterVisibility.addTerritory(territory);
        });

        return filterVisibility;
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link Team}.
     * </p>
     *
     * @param teamVisibilityRow Holds the values of team visibility.
     * @return Converted {@link Team}.
     */
    private Team convertRowToTeam(final Row teamVisibilityRow, final DataContainer dataContainer) {
        final Long teamId = teamVisibilityRow.get(METFTEAMVISIBILITY.TEAM_ID);
        final Team team = new Team();
        final Row teamRow = dataContainer.getRow(getTeamTable(), teamId);

        team.setId(teamId);

        if (Objects.nonNull(teamRow)) {
            team.setName(teamRow.get(OMCRMTEAM.NAME));
        }

        return team;
    }

    /**
     * <p>
     * Converts the {@link Row} to {@link VisibilityPermission}.
     * </p>
     *
     * @param territoryVisibilityRow Holds the values of territory visibility.
     * @return Converted {@link VisibilityPermission}.
     */
    private Territory convertRowToTerritory(final Row territoryVisibilityRow, final DataContainer dataContainer) {
        final Long territoryId = territoryVisibilityRow.get(METFTERRITORYVISIBILITY.TERRITORY_ID);
        final Territory territory = new Territory();
        final Row territoryRow = dataContainer.getRow(getTerritoryTable(), territoryId);

        territory.setId(territoryId);

        if (Objects.nonNull(territoryRow)) {
            territory.setName(territoryRow.get(OMCRMTERRITORY.NAME));
        }

        return territory;
    }
}
