package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
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
public interface ConditionalColumn<C extends ColumnDefinition, T extends TableDefinition<C>> extends Column {

    List<WhenClause> getWhenClauses();

    Object getElseResult();

    ConditionalColumn<C, T> sum();

    ConditionalColumn<C, T> average();

    ConditionalColumn<C, T> count();

    ConditionalColumn<C, T> max();

    ConditionalColumn<C, T> min();

    /**
     * Represents a single WHEN...THEN clause in a CASE expression.
     */
    @Getter
    @RequiredArgsConstructor(staticName = "create")
    class WhenClause {
        /**
         * The condition for the WHEN clause.
         */
        private final WhereClause whereClause;

        /**
         * The result expression if the condition is true.
         */
        private final Object result;
    }

    /**
     * Builder class for constructing {@link ConditionalColumn} instances fluently.
     */
    abstract class AbstractCaseBuilder<C extends ColumnDefinition, T extends TableDefinition<C>> {
        private final List<WhenClause> clauses = new ArrayList<>();
        private Object elseResult;
        private Column column;
        private String alias;

        /**
         * Adds a new WHEN...THEN clause to the CASE expression.
         *
         * @param whenClauses It holds the collection of {@link WhenClause} details
         * @return the current builder instance
         */
        public AbstractCaseBuilder<C, T> when(final @NonNull List<WhenClause> whenClauses) {
            if (whenClauses.isEmpty()) {
                throw ErrorCode.get(SQLErrorCodes.CASE_EXPRESSION_VALUE_REQUIRED);
            }

            clauses.addAll(whenClauses);
            return this;
        }

        /**
         * Sets the ELSE clause.
         */
        public AbstractCaseBuilder<C, T> otherwise(final @NonNull Object elseResult) {
            this.elseResult = elseResult;
            return this;
        }

        /**
         * Sets the table reference for this conditional column.
         */
        public AbstractCaseBuilder<C, T> column(final @NonNull Column column) {
            this.column = column;
            return this;
        }

        /**
         * Sets the alias name for this conditional column.
         */
        public AbstractCaseBuilder<C, T> alias(final String alias) {
            this.alias = alias;
            return this;
        }

        /**
         * Builds and returns the complete {@link ConditionalColumn} object.
         */
        public ConditionalColumn<C, T> build() {
            if (clauses.isEmpty()) {
                throw ErrorCode.get(SQLErrorCodes.CASE_EXPRESSION_VALUE_REQUIRED);
            }

            return buildConditionalColumn(clauses, elseResult, column, Objects.isNull(alias) ? column.getColumnName() : alias);
        }

        /**
         * Builds and returns a {@link ConditionalColumn} instance from the accumulated CASE expression components.
         *
         * @param clauses the list of WHEN...THEN clauses
         * @param elseResult the ELSE clause result, or null if not specified
         * @param column the column associated with the CASE expression
         * @param alias the alias for the resulting column
         * @return the newly built conditional column
         */
        protected abstract ConditionalColumn<C, T> buildConditionalColumn(final List<WhenClause> clauses, final Object elseResult, final Column column, final String alias);
    }

    String toString(final DMLHandler handler, final boolean needAliasName);
}
