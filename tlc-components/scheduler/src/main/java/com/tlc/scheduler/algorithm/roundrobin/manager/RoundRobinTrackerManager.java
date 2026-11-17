package com.tlc.scheduler.algorithm.roundrobin.manager;

import com.tlc.scheduler.algorithm.roundrobin.model.ResetInterval;
import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinGroup;
import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinRule;
import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinTracker;
import com.tlc.scheduler.algorithm.roundrobin.sql.resource.OMSCHROUNDROBINRULE;
import com.tlc.scheduler.algorithm.roundrobin.sql.resource.OMSCHROUNDROBINTRACKER;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import java.time.Instant;
import java.util.Set;

/**
 * Manager class responsible for handling CRUD and data access
 * operations for {@link RoundRobinTracker} entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class RoundRobinTrackerManager extends AbstractRoundRobinManager<RoundRobinTracker> {

    private final RoundRobinGroupManager roundRobinGroupManager;

    private static class Instance {
        private static final RoundRobinTrackerManager ROUND_ROBIN_MANAGER = new RoundRobinTrackerManager();
    }

    private RoundRobinTrackerManager() {
        this.roundRobinGroupManager = RoundRobinGroupManager.getInstance();
    }

    /**
     * Gets the {@link RoundRobinTrackerManager} instance.
     *
     * @return {@link RoundRobinTrackerManager}.
     */
    public static RoundRobinTrackerManager getInstance() {
        return Instance.ROUND_ROBIN_MANAGER;
    }

    /**
     * Gets the {@link Table}.
     *
     * @return The round-robin tracker {@link Table}.
     */
    @Override
    public Table getTable() {
        return Table.get(OMSCHROUNDROBINTRACKER.TABLE);
    }

    /**
     * Gets the {@link Table}.
     *
     * @return The round-robin rule {@link Table}.
     */
    public Table getRuleTable() {
        return Table.get(OMSCHROUNDROBINRULE.TABLE);
    }

    /**
     * Creates a new {@link RoundRobinTracker} entry in the database, along with its
     * associated {@link RoundRobinRule} entry if available.
     *
     * @param roundRobinTracker The tracker entity to be persisted.
     */
    public void create(final RoundRobinTracker roundRobinTracker) {
        final DataContainer targetContainer = DataContainer.create();
        final RoundRobinRule roundRobinRule = roundRobinTracker.getRoundRobinRule();
        final Long orgId = roundRobinTracker.orgId();
        final Table table = getTable();
        final Row row = new Row(table);

        if (Objects.nonNull(roundRobinRule)) {
            final Table ruleTable = getRuleTable();
            final Row ruleRow = new Row(ruleTable);

            ruleRow.setOrgId(orgId);
            ruleRow.set(OMSCHROUNDROBINRULE.RULE_ENTITY_ID, roundRobinRule.getRuleEntityId());
            ruleRow.set(OMSCHROUNDROBINRULE.LAST_ASSIGNED_INDEX, roundRobinRule.getLastAssignedIndex());
            row.set(OMSCHROUNDROBINTRACKER.RULE_ID, ruleRow.getPKValue());

            targetContainer.addNewRow(ruleRow);
        }
        final long currentTime = Instant.now().toEpochMilli();
        final ResetInterval resetInterval = roundRobinTracker.getResetInterval();

        roundRobinTracker.setId(row.getPKValue());
        row.setOrgId(orgId);
        row.set(OMSCHROUNDROBINTRACKER.GROUP_ID, roundRobinTracker.getRoundRobinGroup().id());
        row.set(OMSCHROUNDROBINTRACKER.UPDATED_AT, currentTime);
        row.set(OMSCHROUNDROBINTRACKER.LAST_RESET_AT, currentTime);
        row.set(OMSCHROUNDROBINTRACKER.CURRENT_INDEX, roundRobinTracker.getCurrentIndex());
        row.set(OMSCHROUNDROBINTRACKER.TOTAL_COUNT, roundRobinTracker.getTotalCount());
        row.set(OMSCHROUNDROBINTRACKER.RESET_INTERVAL, Objects.isNull(resetInterval) ? null : resetInterval.getId());

        targetContainer.addNewRow(row);
        orgDataStore(orgId).commitChanges(targetContainer);
    }

    /**
     * Fetches a {@link RoundRobinTracker} instance for the given organization and rule entity ID.
     *
     * @param orgId        The organization ID.
     * @param ruleEntityId The rule entity ID to search for.
     * @return An {@link Optional} containing the tracker if found, or empty otherwise.
     */
    public Optional<RoundRobinTracker> getByRuleEntityId(final long orgId, final Long ruleEntityId) {
        final Table table = getTable();
        final Table ruleTable = getRuleTable();
        final Table groupTable = roundRobinGroupManager.getTable();
        final SelectQuery selectQuery = SelectQuery.get(table);
        final JoinClause.JoinType joinType = JoinClause.JoinType.LEFT;

        selectQuery.addSelectClause(table, ruleTable, groupTable);
        selectQuery.addJoinClause(List.of(new JoinClause(table.getColumn(OMSCHROUNDROBINTRACKER.RULE_ID), ruleTable.getPKColumn(), joinType),
                new JoinClause(table.getColumn(OMSCHROUNDROBINTRACKER.GROUP_ID), groupTable.getPKColumn(), joinType)));
        selectQuery.setWhereClause(new WhereClause(Criteria.eq(ruleTable.getColumn(OMSCHROUNDROBINRULE.RULE_ENTITY_ID), ruleEntityId)));

        final DataContainer dataContainer = orgDataStore(orgId).get(selectQuery);

        return Optional.ofNullable(dataContainer.getRow(table)).map(row -> {
            final RoundRobinTracker roundRobinTracker = convertRowToModel(row);
            final RoundRobinRule roundRobinRule = new RoundRobinRule();
            final Row roundRobinRuleRow = dataContainer.getRow(ruleTable);

            roundRobinRule.setId(roundRobinRuleRow.getPKValue());
            roundRobinRule.setOrgId(orgId);
            roundRobinRule.setLastAssignedIndex(roundRobinRuleRow.get(OMSCHROUNDROBINRULE.LAST_ASSIGNED_INDEX));
            roundRobinRule.setRuleEntityId(roundRobinRuleRow.get(OMSCHROUNDROBINRULE.RULE_ENTITY_ID));

            roundRobinTracker.setRoundRobinGroup(roundRobinGroupManager.convertRowToModel(dataContainer.getRow(groupTable)));
            roundRobinTracker.setRoundRobinRule(roundRobinRule);

            return roundRobinTracker;
        });
    }

    /**
     * Updates the last assigned index for a given organization and rule entity.
     *
     * @param orgId        The organization ID for which the index should be updated.
     * @param id           Refers the {@code RoundRobinTracker} id.
     * @param ruleEntityId The ID of the rule entity associated with this assignment.
     * @param index        The new last-assigned index to persist.
     */
    public void updateLastAssignedIndex(final long orgId, final Long id, final Long ruleEntityId, final int index) {
        final List<UpdateQuery> updateQueries = new ArrayList<>();
        final Table table = getTable();
        final Table ruleTable = getRuleTable();
        final UpdateQuery updateQuery = UpdateQuery.get(table);
        final UpdateQuery ruleUpdateQuery = UpdateQuery.get(ruleTable);

        ruleUpdateQuery.addUpdateColumn(ruleTable.getColumn(OMSCHROUNDROBINRULE.LAST_ASSIGNED_INDEX), index);
        ruleUpdateQuery.setWhereClause(new WhereClause(Criteria.eq(ruleTable.getColumn(OMSCHROUNDROBINRULE.RULE_ENTITY_ID), ruleEntityId)));

        updateQuery.addUpdateColumn(table.getColumn(OMSCHROUNDROBINTRACKER.CURRENT_INDEX), index);
        updateQuery.addUpdateColumn(table.getColumn(OMSCHROUNDROBINTRACKER.UPDATED_AT), Instant.now().toEpochMilli());
        updateQuery.setWhereClause(new WhereClause(Criteria.eq(table.getPKColumn(), id)));

        updateQueries.add(updateQuery);
        updateQueries.add(ruleUpdateQuery);

        orgDataStore(orgId).update(updateQueries);
    }

    /**
     * Deletes the round-robin rule rows for the given ids.
     *
     * @param ids Refers the rule entity ids.
     */
    public void deleteRoundRobinRule(final Set<Long> ids) {
        final Table table = getRuleTable();

        adminDataStore.delete(table, new WhereClause(Criteria.in(table.getColumn(OMSCHROUNDROBINRULE.RULE_ENTITY_ID), ids)));
    }

    /**
     * {@inheritDoc}
     *
     * @param row The database row retrieved from a query.
     * @return The {@code RoundRobinTracker}.
     */
    @Override
    public RoundRobinTracker convertRowToModel(final Row row) {
        final RoundRobinTracker roundRobinTracker = new RoundRobinTracker();
        final Short resetInterval = row.get(OMSCHROUNDROBINTRACKER.RESET_INTERVAL);

        roundRobinTracker.setId(row.getPKValue());
        roundRobinTracker.setOrgId(row.getOrgId());
        roundRobinTracker.setCurrentIndex(row.get(OMSCHROUNDROBINTRACKER.CURRENT_INDEX));
        roundRobinTracker.setLastResetAt(row.get(OMSCHROUNDROBINTRACKER.LAST_RESET_AT));
        roundRobinTracker.setResetInterval(Objects.isNull(resetInterval) ? null : ResetInterval.get(resetInterval));
        roundRobinTracker.setUpdatedAt(row.get(OMSCHROUNDROBINTRACKER.UPDATED_AT));
        roundRobinTracker.setTotalCount(row.get(OMSCHROUNDROBINTRACKER.TOTAL_COUNT));

        return roundRobinTracker;
    }

    /**
     * Initializes a Round-Robin tracker and its corresponding rule entry.
     *
     * @param orgId             The Organization ID
     * @param groupName         Name of the Round Robin group
     * @param ruleEntityId      The ID of the rule entity
     * @param totalCount        Total number of users participating in Round Robin
     * @param targetContainer   Holds the updated information of Round Robin tracker
     */
    public void loadTrackerRowIntoContainer(final long orgId, final String groupName, final Long ruleEntityId,
                                            final int totalCount, final DataContainer targetContainer) {
        final Table table = getTable();
        final Table ruleTable = getRuleTable();
        final Row row = new Row(table);
        final Row ruleRow = new Row(ruleTable);

        ruleRow.setOrgId(orgId);
        ruleRow.set(OMSCHROUNDROBINRULE.RULE_ENTITY_ID, ruleEntityId);
        ruleRow.set(OMSCHROUNDROBINRULE.LAST_ASSIGNED_INDEX, 0);
        row.set(OMSCHROUNDROBINTRACKER.RULE_ID, ruleRow.getPKValue());

        targetContainer.addNewRow(ruleRow);

        final long currentTime = Instant.now().toEpochMilli();
        final RoundRobinGroup roundRobinGroup = roundRobinGroupManager.getByName(groupName);

        row.setOrgId(orgId);
        row.set(OMSCHROUNDROBINTRACKER.GROUP_ID, roundRobinGroup.id());
        row.set(OMSCHROUNDROBINTRACKER.UPDATED_AT, currentTime);
        row.set(OMSCHROUNDROBINTRACKER.LAST_RESET_AT, currentTime);
        row.set(OMSCHROUNDROBINTRACKER.CURRENT_INDEX, 0);
        row.set(OMSCHROUNDROBINTRACKER.TOTAL_COUNT, totalCount);
        row.set(OMSCHROUNDROBINTRACKER.RESET_INTERVAL, null);

        targetContainer.addNewRow(row);
    }
}