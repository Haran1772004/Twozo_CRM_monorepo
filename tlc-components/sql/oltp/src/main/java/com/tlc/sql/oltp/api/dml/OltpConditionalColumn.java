package com.tlc.sql.oltp.api.dml;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ConditionalColumn;
import com.tlc.sql.common.api.handler.AbstractDMLHandler;

import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
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
public class OltpConditionalColumn extends OltpColumn implements ConditionalColumn<OltpColumnDefinition, OltpTableDefinition> {

    /**
     * List of WHEN...THEN clauses for this CASE expression.
     */
    private final List<WhenClause> whenClauses;

    /**
     * Optional ELSE clause result for the CASE expression.
     */
    private final Object elseResult;

    private OltpConditionalColumn(final List<WhenClause> whenClauses, final Object elseResult, final Column column, final String alias) {
        super(column.getTable(), column.getColumnName(), alias, false);
        this.whenClauses = whenClauses;
        this.elseResult = elseResult;
    }

    private OltpConditionalColumn(final OltpConditionalColumn column, final Column.ColumnType type) {
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
    public static AbstractCaseBuilder<OltpColumnDefinition, OltpTableDefinition> when(final List<WhenClause> whenClauses) {
        return CaseBuilder.create().when(whenClauses);
    }

    @Override
    public OltpConditionalColumn sum() {
        return new OltpConditionalColumn(this, Column.ColumnType.SUM);
    }

    @Override
    public OltpConditionalColumn average() {
        return new OltpConditionalColumn(this, Column.ColumnType.AVG);
    }

    @Override
    public OltpConditionalColumn count() {
        return new OltpConditionalColumn(this, Column.ColumnType.COUNT);
    }

    @Override
    public OltpConditionalColumn max() {
        return new OltpConditionalColumn(this, Column.ColumnType.MAX);
    }

    @Override
    public OltpConditionalColumn min() {
        return new OltpConditionalColumn(this, Column.ColumnType.MIN);
    }

    /**
     * Builder class for constructing {@link ConditionalColumn} instances fluently.
     */
    @RequiredArgsConstructor(staticName = "create", access = AccessLevel.PRIVATE)
    public static class CaseBuilder extends AbstractCaseBuilder<OltpColumnDefinition, OltpTableDefinition> {

        @Override
        protected OltpConditionalColumn buildConditionalColumn(final List<ConditionalColumn.WhenClause> clauses, final Object elseResult, final Column column, final String alias) {
            return new OltpConditionalColumn(clauses, elseResult, column, Objects.isNull(alias) ? column.getColumnName() : alias);
        }
    }

    /**
     * Returns the string representation of this {@link Column} using the formatting rules provided by the given {@link AbstractDMLHandler}.
     *
     * @param handler the {@link AbstractDMLHandler} used to build the column SQL string
     * @return the SQL string representation of this column
     */
    @Override
    public String toString(final DMLHandler handler, final boolean needColumnAlias) {
        return handler.getConditionalColumnString(this, needColumnAlias);
    }
}