package com.tlc.sql.common.api.dml.secure;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.util.TableUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Secure CTE-based select query builder.
 *
 * <p>This implementation constructs a {@link With} (CTE) wrapper that represents permission-filtered derived tables for any non-base tables
 * present in {@code permissionCriteriaMap}. When the base table has permission criteria it is transformed into a derived table as well.
 * The resulting {@link With} embeds the transformed {@link SelectQuery} and any derived tables required to enforce row-level permission filters.</p>
 *
 * @author Sathish
 * @version 1.0
 */
public class SecureCTESelectQueryBuilder extends AbstractSecureSelectQueryBuilder<With> {

    private static final String CTE_ALIAS_PREFIX = "permitted_%s";

    private SecureCTESelectQueryBuilder() {
    }

    private static final class InstanceHolder {
        private static final SecureCTESelectQueryBuilder SECURE_SELECT_QUERY_BUILDER = new SecureCTESelectQueryBuilder();
    }

    /**
     * Return the singleton instance of this builder.
     *
     * @return singleton {@link SecureCTESelectQueryBuilder}
     */
    public static SecureCTESelectQueryBuilder getInstance() {
        return InstanceHolder.SECURE_SELECT_QUERY_BUILDER;
    }

    /**
     * Build a {@link With} that secures the supplied select query using the
     * provided per-table permission criteria. The builder will:
     * <ol>
     *   <li>Promote base table to a derived table when base table criteria exist.</li>
     *   <li>Create derived CTE tables for other tables that require permission filters.</li>
     *   <li>Adjust join conditions to reference existence of CTE entries when necessary.</li>
     * </ol>
     *
     * @param selectQuery           the original select query to secure
     * @param permissionCriteriaMap mapping of table -> PermissionCriteria to apply
     * @return a With (CTE) encapsulating the secured query
     */
    @Override
    public With build(final SelectQuery selectQuery, final Map<Table, PermissionCriteria> permissionCriteriaMap) {
        final Map<Table, JoinClause> existingJoinClauseMap = selectQuery.getJoinClause().stream()
                .collect(Collectors.toMap(JoinClause::getRemoteTable, Function.identity()));
        final Table baseTable = selectQuery.getBaseTable();
        final With.Builder withBuilder = With.Builder.create();
        final Collection<String> cteTableNames = new HashSet<>();

        final PermissionCriteria baseTablePermissionCriteria = permissionCriteriaMap.remove(baseTable);

        if (Objects.nonNull(baseTablePermissionCriteria)) {
            selectQuery.updateBaseTableAsDerivedTable(buildDerivedTable(TableUtils.getAllColumns(baseTable), baseTable.getAlias(), baseTable, baseTablePermissionCriteria));
        }

        for (final Map.Entry<Table, PermissionCriteria> permissionCriteriaEntry : permissionCriteriaMap.entrySet()) {
            final Table table = permissionCriteriaEntry.getKey();
            final Column tablePkColumn = table.getPKColumn();
            final String cteAliasName = getCTEAliasName(table);

            if (cteTableNames.add(cteAliasName)) {
                final JoinClause joinClause = existingJoinClauseMap.get(table);
                final DerivedTable derivedTable = buildDerivedTable(Set.of(tablePkColumn), cteAliasName, table, permissionCriteriaEntry.getValue());

                withBuilder.addDerivedTable(derivedTable);

                if (Objects.nonNull(joinClause)) {
                    final CTESelectQuery cteSelectQuery = CTESelectQuery.get(derivedTable);
                    final JoinClause joinClauseWithCondition = new JoinClause(joinClause.getLocalTable(), joinClause.getRemoteTable(),
                            joinClause.getRelations(), joinClause.getJoinType(), new WhereClause(Criteria.exists(cteSelectQuery)), Connector.AND);

                    cteSelectQuery.addStaticSelectClause(1);
                    cteSelectQuery.setWhereClause(new WhereClause(Criteria.eq(derivedTable.getPKColumn(), tablePkColumn)));
                    selectQuery.replaceJoinClause(joinClause, joinClauseWithCondition);

                    if (table instanceof final DerivedTable derivedMainTable) {
                        final SelectQuery derivedMainTableQuery = derivedMainTable.getSelectQuery();

                        derivedMainTableQuery.addSelectClause(derivedMainTableQuery.getBaseTable().getPKColumn());
                    }
                }
            }
        }

        withBuilder.selectQuery(selectQuery);
        return withBuilder.build();
    }

    /**
     * Produce a deterministic alias name for a CTE derived table based on the base table name.
     *
     * @param table the table for which to generate a CTE alias
     * @return generated CTE alias
     */
    private String getCTEAliasName(Table table) {
        return String.format(CTE_ALIAS_PREFIX, table.getName().toLowerCase());
    }
}
