package com.tlc.sql.common.api.ds;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.ExistsQuery;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.Query;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.api.meta.ImmutableColumns;
import com.tlc.sql.common.api.meta.TableType;
import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.internal.ds.DsProvider;
import com.tlc.sql.common.internal.ds.DsProvider.DsProviderType;
import com.tlc.sql.common.internal.ds.WritableDataStoreImpl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Abishek
 * @version 1.0
 */
public class DefaultOrgDataStore extends WritableDataStoreImpl implements OrgDataStore
{
    private final Long orgId;
    private final String orgIdColumn;
    public DefaultOrgDataStore(Long orgId, Map<DsProviderType, DsProvider> dsProviders, DMLHandler dmlHandler)
    {
        super(dsProviders, dmlHandler);
        this.orgId = Objects.requireNonNull(orgId);
        this.orgIdColumn = ImmutableColumns.ORG_ID.getName();
    }

    @Override
    public Long getOrgId()
    {
        return orgId;
    }

    @Override
    protected Row wrapRow(Row row)
    {
        final TableType tableType = row.getTable().getTableDefinition().getType();
        if(tableType.isOrgDependent())
        {
            if(row.isNewRow())
            {
                row.setOrgId(orgId);
            }
            else
            {
                final Long exOrgId = row.getOrgId();
                if(exOrgId == null || !exOrgId.equals(orgId))
                {
                    throw ErrorCode.getLite(ErrorCodes.ACCESS_DENIED);
                }
            }
        }
        return row;
    }

    @Override
    protected SelectQuery wrapSelectQuery(SelectQuery selectQuery)
    {
        wrapMultiQuery(selectQuery);
        return selectQuery;
    }

    @Override
    protected CountQuery wrapCountQuery(CountQuery countQuery)
    {
        wrapMultiQuery(countQuery);
        return countQuery;
    }

    /**
     * Wraps the given {@link ExistsQuery} with organization-scoped constraints.
     *
     * @param existsQuery The {@link ExistsQuery} to be wrapped.
     * @return The wrapped {@link ExistsQuery} with organization filtering applied.
     */
    @Override
    protected ExistsQuery wrapExistsQuery(final ExistsQuery existsQuery) {
        wrapMultiQuery(existsQuery);
        return existsQuery;
    }

    @Override
    protected UpdateQuery wrapUpdateQuery(UpdateQuery updateQuery)
    {
        wrapQuery(updateQuery);
        return updateQuery;
    }

    @Override
    protected DeleteQuery wrapDeleteQuery(DeleteQuery deleteQuery)
    {
        wrapQuery(deleteQuery);
        return deleteQuery;
    }

    @Override
    protected With wrapWithClause(final With with) {

        for (final DerivedTable derivedTable : with.getDerivedTables()) {
            wrapDerivedTableQuery(derivedTable);
        }
        wrapMultiQuery(with.getSelectQuery());

        return with;
    }

    private void wrapMultiQuery(MultiQuery query)
    {
        final Collection<Table> tables = new HashSet<>();
        final WhereClause existing = query.getWhereClause();

        tables.add(query.getBaseTable());
        tables.addAll(query.getJoinClause().stream().filter(joinClause -> joinClause.getJoinType() == JoinClause.JoinType.INNER)
                .map(JoinClause::getRemoteTable).collect(Collectors.toSet()));

        WhereClause builder = null;
        for (Table table : tables)
        {
            final TableType tableType = table.getTableDefinition().getType();
            if(tableType.isOrgDependent())
            {
                final WhereClause existingCriteria = builder;
                Optional<WhereClause> optionalOrgCriteria = buildOrgCriteria(table, existing);

                if(tableType.isCommonNullExists())
                {
                    optionalOrgCriteria = optionalOrgCriteria.map(orgCriteria -> buildNullableOrgCriteria(table, existingCriteria).map(orgCriteria::or).orElse(orgCriteria));
                }
                builder = optionalOrgCriteria.map(orgCriteria -> Objects.isNull(existingCriteria) ? orgCriteria : orgCriteria.and(existingCriteria)).orElse(builder);
            }
        }
        final boolean hasExistingCriteria = Objects.nonNull(existing);
        final Collection<Table> joinedTables = query.getTables();

        wrapInnerSelectQuery(joinedTables);

        if (hasExistingCriteria) {
            wrapInnerSelectQuery(existing);
        }

        if(builder != null)
        {
            query.setWhereClause(hasExistingCriteria ? builder.and(existing) : builder);
        }
    }

    /**
     * <p>
     * Builds the org criteria for the given table.
     * </p>
     *
     * @param table       Represents the {@link Table}.
     * @param whereClause Represents the {@link WhereClause}.
     * @return Org criteria.
     */
    private Optional<WhereClause> buildOrgCriteria(final Table table, final WhereClause whereClause) {
        final Column orgIdColumn = table.getColumn(this.orgIdColumn);

        return Objects.nonNull(whereClause) && hasOrgCriteria(orgIdColumn, whereClause, false) ? Optional.empty()
                : Optional.of(new WhereClause(Criteria.eq(orgIdColumn, orgId)));
    }

    /**
     * <p>
     * Builds the nullable org criteria for the given table.
     * </p>
     *
     * @param table       Represents the {@link Table}.
     * @param whereClause Represents the {@link WhereClause}.
     * @return Nullable org criteria.
     */
    private Optional<WhereClause> buildNullableOrgCriteria(final Table table, final WhereClause whereClause) {
        final Column orgIdColumn = table.getColumn(this.orgIdColumn);

        return Objects.nonNull(whereClause) && hasOrgCriteria(orgIdColumn, whereClause, true) ? Optional.empty()
                : Optional.of(new WhereClause(Criteria.eq(orgIdColumn, null)));
    }

    /**
     * <p>
     * Checks whether the given {@link WhereClause} has org related criteria or not.
     * </p>
     *
     * @param orgIdColumn         Represents the org ID {@link Column}.
     * @param whereClause         Represents the {@link WhereClause}.
     * @param isNullValueCriteria A flag that indicates whether the nullable org criteria or provided org ID criteria.
     * @return A flag that indicates whether the given {@link WhereClause} has org related criteria or not.
     */
    private boolean hasOrgCriteria(final Column orgIdColumn, final WhereClause whereClause, final boolean isNullValueCriteria) {
        if (whereClause.isConnector()) {
            boolean hasOrgCriteria = hasOrgCriteria(orgIdColumn, whereClause.getLeft(), isNullValueCriteria);

            if (hasOrgCriteria(orgIdColumn, whereClause.getRight(), isNullValueCriteria)) {
                hasOrgCriteria = true;
            }
            return hasOrgCriteria;
        }
        final Criteria criteria = whereClause.getCriteria();

        if (Objects.equals(orgIdColumn, criteria.getColumn())) {
            final Object value = criteria.getValue();

            return Objects.equals(Operator.EQUAL, criteria.getOperator()) && (isNullValueCriteria ? Objects.isNull(value)
                    : Objects.equals(orgId, value));
        }
        return false;
    }

    /**
     * <p>
     * Appends the org and common null criteria into the inner select query.
     * </p>
     *
     * @param whereClause Holds the retrieval criteria.
     */
    private void wrapInnerSelectQuery(final WhereClause whereClause) {
        if (whereClause.isConnector()) {
            final WhereClause leftCriteria = whereClause.getLeft();
            final WhereClause rightCriteria = whereClause.getRight();

            wrapInnerSelectQuery(leftCriteria);
            wrapInnerSelectQuery(rightCriteria);
        } else {
            if (whereClause.getCriteria().getValue() instanceof SelectQuery selectQuery) {
                wrapMultiQuery(selectQuery);
            }
        }
    }

    /**
     * <p>
     * Appends the org and common null criteria into the inner select query.
     * </p>
     *
     * @param tables Holds the base table and joined tables.
     */
    private void wrapInnerSelectQuery(final Collection<Table> tables) {
        for (final Table table : tables) {

            if (table instanceof final DerivedTable derivedTable) {
                wrapDerivedTableQuery(derivedTable);
            }
        }
    }

    private void wrapDerivedTableQuery(final DerivedTable derivedTable) {
        final SelectQuery selectQuery = derivedTable.getSelectQuery();

        if (Objects.nonNull(selectQuery)) {
            wrapMultiQuery(selectQuery);
            final Table baseTable = selectQuery.getBaseTable();

            if (baseTable.getTableDefinition().getType().isOrgDependent()) {
                selectQuery.addSelectClause(baseTable.getColumn(this.orgIdColumn));
            }
        }
    }

    private void wrapQuery(Query query)
    {
        final Table table = query.getBaseTable();
        final TableType tableType = table.getTableDefinition().getType();
        if(tableType.isOrgDependent())
        {
            final WhereClause criteriaBuilder = query.getWhereClause();
            final Optional<WhereClause> optionalOrgCriteria = buildOrgCriteria(table, criteriaBuilder);

            optionalOrgCriteria.ifPresent(orgCriteria -> query.setWhereClause(orgCriteria.and(criteriaBuilder)));
        }
    }
}
