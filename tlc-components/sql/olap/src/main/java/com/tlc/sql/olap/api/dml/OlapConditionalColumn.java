package com.tlc.sql.olap.api.dml;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ConditionalColumn;

import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapTableDefinition;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Objects;

/**
 * Represents a SQL CASE expression as a Column.
 * <p>
 * Example:
 *   CASE
 *      WHEN condition1 THEN result1
 *      WHEN condition2 THEN result2
 *      ELSE defaultResult
 *   END
 * <p>
 * This allows CASE expressions to be treated like other {@link Column} objects.
 */
@Getter
public class OlapConditionalColumn extends OlapColumn implements ConditionalColumn<OlapColumnDefinition, OlapTableDefinition> {

    /**
     * List of WHEN...THEN clauses for this CASE expression.
     */
    private final List<WhenClause> whenClauses;

    /**
     * Optional ELSE clause result for the CASE expression.
     */
    private final Object elseResult;

    private OlapConditionalColumn(final List<WhenClause> whenClauses, final Object elseResult, final Column column, final String alias) {
        super(column.getTable(), column.getColumnDefinition(), column.getColumnName(), alias);
        this.whenClauses = whenClauses;
        this.elseResult = elseResult;
    }

    private OlapConditionalColumn(final OlapConditionalColumn column, final Column.ColumnType type) {
        super(column, type);
        this.whenClauses = column.whenClauses;
        this.elseResult = column.elseResult;
    }

    /**
     * Begins building a new CASE expression starting with a WHEN clause.
     *
     * @param whenClauses It holds the {@link WhenClause} details
     * @return a {@link CaseBuilder} for chaining additional clauses
     */
    public static AbstractCaseBuilder<OlapColumnDefinition, OlapTableDefinition> when(final List<WhenClause> whenClauses) {
        return CaseBuilder.create().when(whenClauses);
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing SUM(column)
     */
    @Override
    public OlapConditionalColumn sum() {
        return new OlapConditionalColumn(this, Column.ColumnType.SUM);
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing AVG(column)
     */
    @Override
    public OlapConditionalColumn average() {
        return new OlapConditionalColumn(this, Column.ColumnType.AVG);
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing COUNT(column)
     */
    @Override
    public OlapConditionalColumn count() {
        return new OlapConditionalColumn(this, Column.ColumnType.COUNT);
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing MAX(column)
     */
    @Override
    public OlapConditionalColumn max() {
        return new OlapConditionalColumn(this, Column.ColumnType.MAX);
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing MIN(column)
     */
    @Override
    public OlapConditionalColumn min() {
        return new OlapConditionalColumn(this, Column.ColumnType.MIN);
    }

    /**
     * Builder class for constructing {@link ConditionalColumn} instances fluently.
     */
    @RequiredArgsConstructor(staticName = "create", access = AccessLevel.PRIVATE)
    public static class CaseBuilder extends AbstractCaseBuilder<OlapColumnDefinition, OlapTableDefinition> {

        /**
         * {@inheritDoc}
         *
         * @param clauses the list of WHEN...THEN clauses
         * @param elseResult the ELSE clause result, or null if not specified
         * @param column the column associated with the CASE expression
         * @param alias the alias for the resulting column
         * @return the newly built conditional column
         */
        @Override
        protected OlapConditionalColumn buildConditionalColumn(final List<ConditionalColumn.WhenClause> clauses, final Object elseResult, final Column column, final String alias) {
            return new OlapConditionalColumn(clauses, elseResult, column, Objects.isNull(alias) ? column.getColumnName() : alias);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param handler the DML handler used to build the column SQL string
     * @return the SQL string representation of this column
     */
    @Override
    public String toString(final DMLHandler handler, final boolean needColumnAlias) {
        return handler.getConditionalColumnString(this, needColumnAlias);
    }
}