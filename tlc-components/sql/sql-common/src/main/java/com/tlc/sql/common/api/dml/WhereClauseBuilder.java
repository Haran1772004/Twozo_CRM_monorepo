package com.tlc.sql.common.api.dml;

import com.tlc.sql.common.api.dml.util.WhereClauseUtil;

import java.util.Objects;

/**
 * <p>
 * A builder class for constructing {@link WhereClause} instances.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class WhereClauseBuilder {

    private WhereClause whereClause;

    public WhereClauseBuilder() {
        whereClause = null;
    }

    public WhereClauseBuilder(final WhereClause baseCriteria) {
        whereClause = baseCriteria;
    }

    /**
     * <p>
     * Appends the provided {@link WhereClause} to the existing one using the specified {@link Connector}.
     * </p>
     *
     * @param whereClause The {@link WhereClause} to append.
     * @param connector   The {@link Connector} to use for combining the clauses.
     * @return The current instance of {@link WhereClauseBuilder}.
     */
    public WhereClauseBuilder append(final WhereClause whereClause, final Connector connector) {
        this.whereClause = Objects.isNull(this.whereClause)
                ? whereClause
                : WhereClauseUtil.connect(this.whereClause, whereClause, connector);
        return this;
    }

    /**
     * <p>
     * Retrieves the constructed {@link WhereClause}.
     * </p>
     *
     * @return The constructed {@link WhereClause}.
     */
    public WhereClause getWhereClause() {
        return whereClause;
    }
}
