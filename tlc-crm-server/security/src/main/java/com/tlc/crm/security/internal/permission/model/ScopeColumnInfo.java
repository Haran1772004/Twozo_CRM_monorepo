package com.tlc.crm.security.internal.permission.model;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.JoinClause;

import java.util.Collections;
import java.util.List;

/**
 * Holds a scope column along with the join clauses required to access it in a query.
 *
 * @param column The scope column
 * @param joins  The join clauses needed to include the column's table in a query; empty if no joins are required
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public record ScopeColumnInfo(Column column, List<JoinClause> joins) {

    /**
     * Creates a {@link ScopeColumnInfo} with no required joins.
     *
     * @param column The scope column
     * @return A new {@link ScopeColumnInfo} instance
     */
    public static ScopeColumnInfo of(final Column column) {
        return new ScopeColumnInfo(column, Collections.emptyList());
    }

    /**
     * Creates a {@link ScopeColumnInfo} with the specified joins.
     *
     * @param column The scope column
     * @param joins  The required join clauses
     * @return A new {@link ScopeColumnInfo} instance
     */
    public static ScopeColumnInfo of(final Column column, final List<JoinClause> joins) {
        return new ScopeColumnInfo(column, joins);
    }

    /**
     * @return true if join clauses are required to access this scope column
     */
    public boolean hasJoins() {
        return !joins.isEmpty();
    }
}