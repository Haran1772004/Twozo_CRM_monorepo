package com.tlc.sql.common.api.dml.secure;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base implementation helpers for secure select query builders.
 *
 * <p>This abstract helper provides common routines used by concrete implementations that need to transform a {@link SelectQuery}
 * and per-table {@link PermissionCriteria} into secured query forms. It helps build derived tables
 * (either CTE-shaded or regular derived tables), append necessary join clauses referenced by permission
 * predicates, and produce consistent CTE alias names.</p>
 *
 * @param <T> secured query representation produced by subclasses
 * @author Sathish
 * @version 1.0
 */
public abstract class AbstractSecureSelectQueryBuilder<T> implements SecureSelectQueryBuilder<T> {

    /**
     * Build a derived table representation for the given table and permission criteria. When {@code isCTETable} is true the derived
     * table will include only the primary key column (suitable for CTE usage); otherwise the full table reference/columns are selected.
     *
     * @param columns            the columns to include in the derived table
     * @param aliasName          the alias name for the derived table
     * @param table              the base table for which the derived table is built
     * @param permissionCriteria the permission criteria supplying where-clause and join-configs
     * @return a DerivedTable encapsulating the secured selection logic
     */
    protected DerivedTable buildDerivedTable(final Collection<Column> columns, final String aliasName, final Table table,
                                             final PermissionCriteria permissionCriteria) {
        final String tableName = table.getName();
        final Table dataTable = table instanceof DerivedTable ? Table.get(tableName, table.getAlias()) : table;
        final SelectQuery selectQuery = SelectQuery.get(dataTable);
        final WhereClause whereClause = permissionCriteria.whereClause();

        selectQuery.addSelectClause(columns);
        selectQuery.setWhereClause(whereClause);

        appendJoins(dataTable, null, selectQuery, permissionCriteria.joinConfigs(), whereClause);
        return DerivedTable.get(tableName, aliasName, selectQuery);
    }

    /**
     * Recursively inspect the where-clause and append any join clauses to the provided SelectQuery that are required by permission-driven predicates.
     * This method will only add a join when the predicate's criteria references a table not already present in the select.
     *
     * @param baseTable                the originating base table for join resolution
     * @param initialCriteriaConnector the connector to use for the initial criteria (AND/OR)
     * @param selectQuery              the select query to which join clauses are added
     * @param joinConfigsMap           map of table name -> JoinConfigs used to produce join clauses
     * @param whereClause              the permission-derived where-clause to inspect
     */
    private void appendJoins(final Table baseTable, final Connector initialCriteriaConnector, final SelectQuery selectQuery,
                             final Map<String, JoinConfigs> joinConfigsMap, final WhereClause whereClause) {

        if (whereClause.isConnector()) {
            final Connector connector = whereClause.getConnector();

            appendJoins(baseTable, connector, selectQuery, joinConfigsMap, whereClause.getLeft());
            appendJoins(baseTable, connector, selectQuery, joinConfigsMap, whereClause.getRight());
        } else {
            final Table criteriaTable = whereClause.getCriteria().getColumn().getTable();

            if (selectQuery.getTables().contains(criteriaTable)) {
                if (!Objects.equals(selectQuery.getBaseTable(), criteriaTable)) {
                    final JoinConfigs joinConfigs = joinConfigsMap.get(criteriaTable.getName());

                    if (Objects.nonNull(joinConfigs)) {
                        final Map<Table, JoinClause> existingJoinClauseMap = selectQuery.getJoinClause().stream()
                                .collect(Collectors.toMap(JoinClause::getRemoteTable, Function.identity()));
                        final List<JoinClause> joins = joinConfigs.getJoins(initialCriteriaConnector, baseTable);

                        for (final JoinClause join : joins) {
                            final JoinClause.JoinType joinType = join.getJoinType();
                            final JoinClause existingJoinClause = existingJoinClauseMap.get(join.getRemoteTable());

                            if ((joinType == JoinClause.JoinType.LEFT || joinType == JoinClause.JoinType.RIGHT)
                                    && (Objects.nonNull(existingJoinClause) && existingJoinClause.getJoinType() != joinType)) {
                                selectQuery.replaceJoinClause(existingJoinClause, join);
                            }
                        }
                    }
                }
            } else {
                final JoinConfigs joinConfigs = joinConfigsMap.get(criteriaTable.getName());

                if (Objects.nonNull(joinConfigs)) {
                    selectQuery.addJoinClause(joinConfigs.getJoins(initialCriteriaConnector, baseTable));
                }
            }
        }
    }
}
