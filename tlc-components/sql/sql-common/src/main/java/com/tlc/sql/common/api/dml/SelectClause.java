package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.Objects;

/**
 * <p>
 * Represents a single entry in a SQL SELECT clause, which may be either a
 * table {@link Column} or a static literal value (numeric or string).
 * </p>
 *
 * <p>
 * {@code SelectClause} instances are {@link Comparable}: column-based clauses
 * are ordered by their column type order, while static-value clauses are ordered
 * among themselves by value. Column-based clauses always sort before static-value clauses.
 * </p>
 *
 * @see SelectQuery#addSelectClause(Column)
 * @see SelectQuery#addStaticSelectClause(Number)
 * @see SelectQuery#addStaticSelectClause(String)
 */
public class SelectClause implements Comparable<SelectClause> {

    private final Column column;
    private final Object staticValue;

    /**
     * Creates a column-based {@code SelectClause}.
     *
     * @param column the {@link Column} to include in the SELECT clause; must not be {@code null}
     */
    public SelectClause(final Column column) {
        this.column = Objects.requireNonNull(column);
        this.staticValue = null;
    }

    /**
     * Creates a static numeric literal {@code SelectClause}.
     *
     * @param staticValue the numeric literal to include in the SELECT clause; must not be {@code null}
     */
    public SelectClause(final Number staticValue) {
        this.column = null;
        this.staticValue = Objects.requireNonNull(staticValue);
    }

    /**
     * Creates a static string literal {@code SelectClause}.
     *
     * @param staticValue the string literal to include in the SELECT clause; must not be {@code null}
     */
    public SelectClause(final String staticValue) {
        this.column = null;
        this.staticValue = Objects.requireNonNull(staticValue);
    }

    /**
     * Returns the {@link Column} associated with this clause.
     *
     * @return the column, or {@code null} if this clause holds a static value
     */
    public Column getColumn() {
        return column;
    }

    /**
     * Returns the static literal value associated with this clause.
     *
     * @return the static value ({@link Number} or {@link String}),
     *         or {@code null} if this clause holds a column
     */
    public Object getStaticValue() {
        return staticValue;
    }

    /**
     * Returns {@code true} if this clause holds neither a column nor a static value.
     *
     * @return {@code true} if both column and static value are {@code null}
     */
    public boolean isEmpty() {
        return Objects.isNull(column) && Objects.isNull(staticValue);
    }

    /**
     * Returns {@code true} if this clause is backed by a {@link Column}.
     *
     * @return {@code true} when a column is present; {@code false} for static-value clauses
     */
    public boolean hasColumn() {
        return Objects.nonNull(column);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final SelectClause selectClause = (SelectClause) object;

        return Objects.equals(column, selectClause.column) && Objects.equals(staticValue, selectClause.staticValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(column, staticValue);
    }

    /**
     * Compares this {@code SelectClause} with another for ordering within a {@link java.util.SortedSet}.
     *
     * <p>Ordering rules:</p>
     * <ul>
     *   <li>Column-based clauses sort before static-value clauses.</li>
     *   <li>Among column-based clauses, ordering is determined by the column type's order value;
     *       ties are broken by hash code.</li>
     *   <li>Among static-value clauses, numeric values are compared by their {@code double} value
     *       and string values lexicographically. Comparing a numeric static value against a string
     *       static value throws an {@link com.tlc.commons.code.ErrorCode}.</li>
     * </ul>
     *
     * @param selectClause the other {@code SelectClause} to compare against; must not be {@code null}
     * @return a negative integer, zero, or a positive integer as this clause is less than,
     *         equal to, or greater than the specified clause
     * @throws com.tlc.commons.code.ErrorCode if both clauses hold static values of incompatible types
     */
    @Override
    public int compareTo(final SelectClause selectClause) {
        final Column column = selectClause.getColumn();
        final Object staticValue = selectClause.getStaticValue();

        if (Objects.isNull(this.column)) {
            if (Objects.isNull(staticValue)) {
                return 1;
            }

            return Objects.compare(this.staticValue, staticValue, (currentObject, newObject) -> {

                if (currentObject instanceof final Number currentNumber && newObject instanceof final Number newNumber) {
                    return Double.compare(currentNumber.doubleValue(), newNumber.doubleValue());
                } else if (currentObject instanceof final String currentString && newObject instanceof final String newString) {
                    return currentString.compareTo(newString);
                }

                throw ErrorCode.get(SQLErrorCodes.INVALID_VALUE_TYPE, "Cannot compare static values of different types");
            });
        }

        if (Objects.isNull(column)) {
            return -1;
        }
        final int comparableColumnTypeOrder = column.getColumnType().getOrder();
        final int columnTypeOrder = this.column.getColumnType().getOrder();

        if (comparableColumnTypeOrder > columnTypeOrder) {
            return 1;
        } else if (comparableColumnTypeOrder == columnTypeOrder) {
            return this.column.equals(column) ? 0 : Integer.compare(this.column.hashCode(), column.hashCode());
        } else {
            return -1;
        }
    }
}
