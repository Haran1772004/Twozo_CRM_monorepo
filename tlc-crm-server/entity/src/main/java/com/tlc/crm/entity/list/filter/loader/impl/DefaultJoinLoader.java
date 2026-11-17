package com.tlc.crm.entity.list.filter.loader.impl;

import com.tlc.crm.entity.list.filter.loader.JoinsLoader;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.SelectQuery;

/**
 * <p>
 * Implements of the {@link JoinsLoader} interface to provides functionality for appends the joins into {@link SelectQuery}
 * </p>
 *
 * @author Sathish
 */
public class DefaultJoinLoader extends AbstractJoinsLoader {

    private final MultiQuery multiQuery;
    private final JoinClause.JoinType joinType;

    public DefaultJoinLoader(final MultiQuery multiQuery, final JoinClause.JoinType joinType) {
        this.multiQuery = multiQuery;
        this.joinType = joinType;
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
        addJoinClauses(multiQuery, new JoinClause(localColumn, remoteColumn, joinType));
    }
}
