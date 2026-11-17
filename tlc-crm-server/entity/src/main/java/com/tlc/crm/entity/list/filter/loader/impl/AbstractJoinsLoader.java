package com.tlc.crm.entity.list.filter.loader.impl;

import com.tlc.crm.entity.list.filter.loader.JoinsLoader;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Implements of the {@link JoinsLoader} interface to provides functionality for managing join clause
 * </p>
 *
 * @author Mohamed Yasar k
 */
public abstract class AbstractJoinsLoader implements JoinsLoader {

    /**
     * <p>
     * Adds the List of join clauses into {@link SelectQuery}
     * </p>
     *
     * @param multiQuery Represents the {@link MultiQuery}
     * @param joinClause  holds the details about {@link JoinClause}
     */
    protected final void addJoinClauses(final MultiQuery multiQuery, final JoinClause joinClause) {
        final Collection<Table> localTableJoins = new HashSet<>();
        final Map<Table, JoinClause> remoteTableJoins = new HashMap<>();

        for (final JoinClause existingJoinClause : multiQuery.getJoinClause()) {
            localTableJoins.add(existingJoinClause.getLocalTable());
            remoteTableJoins.put(existingJoinClause.getRemoteTable(), existingJoinClause);
        }
        final Table remoteTable = joinClause.getRemoteTable();

        if (remoteTableJoins.containsKey(remoteTable)) {
            final JoinClause remoteTableJoinClause = remoteTableJoins.get(remoteTable);
            final JoinClause.JoinType joinType = joinClause.getJoinType();

            if (Objects.equals(joinClause.getLocalTable(), remoteTableJoinClause.getLocalTable())
                    && !(JoinClause.JoinType.INNER == joinType || remoteTableJoinClause.getJoinType() == joinType)) {
                multiQuery.replaceJoinClause(remoteTableJoinClause, joinClause);
            }
        } else if (!localTableJoins.contains(remoteTable)) {
            multiQuery.addJoinClause(joinClause);
        }
    }
}