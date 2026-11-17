package com.tlc.scheduler.algorithm.roundrobin.manager;

import com.tlc.scheduler.algorithm.roundrobin.model.LimitType;
import com.tlc.scheduler.algorithm.roundrobin.model.RoundRobinGroup;
import com.tlc.scheduler.algorithm.roundrobin.sql.resource.MSCHROUNDROBINGROUP;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

/**
 * Manager class responsible for performing database operations
 * related to {@link RoundRobinGroup} entities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class RoundRobinGroupManager extends AbstractRoundRobinManager<RoundRobinGroup> {

    private static class Instance {
        private static final RoundRobinGroupManager ROUND_ROBIN_GROUP_MANAGER = new RoundRobinGroupManager();
    }

    private RoundRobinGroupManager() {
    }

    /**
     * Gets the {@link RoundRobinGroupManager} instance.
     *
     * @return {@link RoundRobinGroupManager}.
     */
    public static RoundRobinGroupManager getInstance() {
        return Instance.ROUND_ROBIN_GROUP_MANAGER;
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Table}.
     */
    @Override
    public Table getTable() {
        return Table.get(MSCHROUNDROBINGROUP.TABLE);
    }

    /**
     * Checks whether a round-robin group with the given name already exists.
     *
     * @param group The {@link RoundRobinGroup} object containing the name to check.
     * @return {@code true} if a group with the same name exists, otherwise {@code false}.
     */
    public boolean exists(final RoundRobinGroup group) {
        final Table table = getTable();
        final CountQuery countQuery = CountQuery.get(table);

        countQuery.setWhereClause(new WhereClause(Criteria.eq(table.getColumn(MSCHROUNDROBINGROUP.NAME), group.name())));
        return adminDataStore.get(countQuery) > 0;
    }

    /**
     * Retrieves a {@link RoundRobinGroup} by its name.
     *
     * @param group The name of the group.
     * @return The {@link RoundRobinGroup} if found, or {@code null} if no record matches.
     */
    public RoundRobinGroup getByName(final String group) {
        final Table table = getTable();
        final Row row = adminDataStore.get(table, new WhereClause(Criteria.eq(table.getColumn(MSCHROUNDROBINGROUP.NAME), group))).getRow(table);

        return convertRowToModel(row);
    }

    /**
     * {@inheritDoc}
     *
     * @param row The database row retrieved from a query.
     */
    @Override
    public RoundRobinGroup convertRowToModel(final Row row) {
        return new RoundRobinGroup(row.getPKValue(), row.get(MSCHROUNDROBINGROUP.NAME),
                LimitType.get(row.get(MSCHROUNDROBINGROUP.LIMIT_TYPE)));
    }
}
