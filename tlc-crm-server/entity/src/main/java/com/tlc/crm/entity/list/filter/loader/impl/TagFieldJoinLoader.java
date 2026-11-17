package com.tlc.crm.entity.list.filter.loader.impl;

import com.tlc.sql.common.util.TableUtils;
import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.crm.entity.sql.resource.OMTTAG;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.oltp.resolver.OltpTableResolver;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Implements of the {@link JoinsLoader} interface to provides functionality for tag field
 * </p>
 *
 * @author Sathish
 */
public class TagFieldJoinLoader extends AbstractJoinsLoader {

    private final MultiQuery multiQuery;
    private final JoinClause.JoinType joinType;
    private final Collection<Long> valueEntityIds;

    public TagFieldJoinLoader(final MultiQuery multiQuery, final JoinClause.JoinType joinType,
                              final Collection<Long> valueEntityIds) {
        this.multiQuery = multiQuery;
        this.joinType = joinType;
        this.valueEntityIds = valueEntityIds;
    }

    /**
     * <p>
     * Builds and adds the {@link JoinClause} in the select query.
     * </p>
     *
     * @param localColumn  Represents the local column of the {@link JoinClause}.
     * @param remoteColumn Represents the remote column of the {@link JoinClause}.
     */
    @Override
    public void load(final Column localColumn, final Column remoteColumn) {
        final JoinClause joinClause;

        if (valueEntityIds.isEmpty()) {
            joinClause = new JoinClause(localColumn, remoteColumn, joinType);
        } else {
            final Table derivedTable = buildDerivedTableForMultiSelect(valueEntityIds, remoteColumn);
            joinClause = new JoinClause(localColumn, derivedTable.getColumn(remoteColumn.getColumnName()), joinType);
        }

        addJoinClauses(multiQuery, joinClause);
    }

    /**
     * <p>
     * Builds the {@link DerivedTable} to build inner {@link SelectQuery}.
     * </p>
     *
     * @param valueEntityIds Represents the value entity ids.
     * @param remoteColumn   Represents the remote column.
     * @return {@link DerivedTable}.
     */
    private Table buildDerivedTableForMultiSelect(final Collection<Long> valueEntityIds, final Column remoteColumn) {
        final Table remoteTable = remoteColumn.getTable();
        final Table tagTable = getTagTable();
        final Map<Table, Column> fkColumns = TableUtils.groupFkColumnsByTable(remoteTable, OltpTableResolver.get());

        if (fkColumns.containsKey(tagTable)) {
            final SelectQuery selectQuery = SelectQuery.get(remoteTable);

            selectQuery.addSelectClause(remoteColumn);
            selectQuery.addGroupByClause(new GroupByClause(remoteColumn));

            return DerivedTable.get(remoteTable.getName(), remoteTable.getAlias(), selectQuery);
        }

        return remoteTable;
    }

    /**
     * <p>
     * Fetches the table of the tag.
     * </p>
     *
     * @return the tag table.
     */
    private Table getTagTable() {
        return Table.get(OMTTAG.TABLE);
    }
}