package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.dml.secure.CTESelectQuery;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import lombok.NonNull;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class Criteria
{
    protected final Column column;
    protected final Object value;
    protected final Operator operator;

    /**
     * Constructs a new {@link Criteria} with the specified column, value, and operator.
     *
     * @param column   the {@link Column} to apply the criteria on
     * @param value    the value to compare against
     * @param operator the {@link Operator} for the comparison
     */
    public Criteria(Column column, Object value, Operator operator)
    {
        this.column = column;
        this.value = value;
        this.operator = Objects.requireNonNull(operator);
    }

    /**
     * Returns the column associated with this criteria.
     *
     * @return the {@link Column} used in this criteria
     */
    public Column getColumn()
    {
        return column;
    }

    /**
     * Returns the value associated with this criteria.
     *
     * @return the value used in the comparison
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * Returns the operator associated with this criteria.
     *
     * @return the {@link Operator} used in the comparison
     */
    public Operator getOperator()
    {
        return operator;
    }

    /**
     * Returns a string representation of this criteria in the format "column operator value".
     *
     * @return string representation of this criteria
     */
    @Override
    public String toString()
    {
        if (operator == Operator.EXISTS) {
            return StringUtil.join(StringUtil.Delimiter.EMPTY_SPACE, operator.name(), String.valueOf(value));
        }
        return column + " " + operator + " " + value;
    }

    /**
     * Creates a new {@link Criteria} after validating that the column is not a storage type.
     *
     * @param column   the {@link Column} to apply the criteria on
     * @param value    the value to compare against
     * @param operator the {@link Operator} for the comparison
     * @return a new {@link Criteria} instance
     * @throws ErrorCode if the column is a storage type
     */
    private static Criteria create(Column column, Object value, Operator operator)
    {
        final DataType dataType = column.getColumnDefinition().getDataType();
        if(dataType.isStorageType())
        {
            throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COLUMN);
        }
        return new Criteria(column, value, operator);
    }

    /**
     * Creates a criteria for an equality comparison.
     *
     * @param column the {@link Column} to compare
     * @param value  the value to compare against
     * @return a new {@link Criteria} with {@link Operator#EQUAL}
     */
    public static Criteria eq(Column column, Object value)
    {
        return create(column, value, Operator.EQUAL);
    }

    /**
     * Creates a criteria for a bitwise AND comparison.
     *
     * @param column the {@link Column} to apply the bitwise AND on
     * @param value  the bitmask value
     * @param result the expected result of the bitwise AND operation
     * @return a new {@link Criteria} with {@link Operator#BIT_AND}
     */
    public static Criteria bitAnd(Column column, int value, int result)
    {
        return create(column, new Number[]{value, result}, Operator.BIT_AND);
    }

    /**
     * Creates a criteria for a not-equal comparison.
     *
     * @param column the {@link Column} to compare
     * @param value  the value to compare against
     * @return a new {@link Criteria} with {@link Operator#NOT_EQUAL}
     */
    public static Criteria notEq(Column column, Object value)
    {
        return create(column, value, Operator.NOT_EQUAL);
    }

    /**
     * Creates a criteria for a starts-with comparison.
     *
     * @param column the {@link Column} to compare
     * @param value  the prefix string to match
     * @return a new {@link Criteria} with {@link Operator#STARTS_WITH}
     */
    public static Criteria startsWith(Column column, String value)
    {
        return create(column, value, Operator.STARTS_WITH);
    }

    /**
     * Creates a criteria for a not-starts-with comparison.
     *
     * @param column the {@link Column} to compare
     * @param value  the prefix string to exclude
     * @return a new {@link Criteria} with {@link Operator#NOT_STARTS_WITH}
     */
    public static Criteria notStartsWith(Column column, String value)
    {
        return create(column, value, Operator.NOT_STARTS_WITH);
    }

    /**
     * Creates a criteria for a contains comparison.
     *
     * @param column the {@link Column} to compare
     * @param value  the substring to match
     * @return a new {@link Criteria} with {@link Operator#CONTAINS}
     */
    public static Criteria contains(Column column, String value)
    {
        return create(column, value, Operator.CONTAINS);
    }

    /**
     * Creates a criteria for a not-contains comparison.
     *
     * @param column the {@link Column} to compare
     * @param value  the value to exclude
     * @return a new {@link Criteria} with {@link Operator#NOT_CONTAINS}
     */
    public static Criteria notContains(Column column, Object value)
    {
        return create(column, value, Operator.NOT_CONTAINS);
    }

    /**
     * Creates a criteria for an IN comparison with a collection of values.
     *
     * @param column the {@link Column} to compare
     * @param value  the collection of values to match against
     * @return a new {@link Criteria} with {@link Operator#IN}
     */
    public static Criteria in(Column column, Collection<?> value)
    {
        return create(column, value, Operator.IN);
    }

    /**
     * Creates a criteria for an IN comparison with a sub-query.
     *
     * @param column      the {@link Column} to compare
     * @param selectQuery the sub-query whose results are used for the IN comparison
     * @return a new {@link Criteria} with {@link Operator#IN}
     */
    public static Criteria in(Column column, SelectQuery selectQuery)
    {
        return create(column, selectQuery, Operator.IN);
    }

    /**
     * Creates a criteria for a NOT IN comparison with a collection of values.
     *
     * @param column the {@link Column} to compare
     * @param value  the collection of values to exclude
     * @return a new {@link Criteria} with {@link Operator#NOT_IN}
     */
    public static Criteria notIn(Column column, Collection<?>  value)
    {
        return create(column, value, Operator.NOT_IN);
    }

    /**
     * Creates a criteria for a NOT IN comparison with a sub-query.
     *
     * @param column      the {@link Column} to compare
     * @param selectQuery the sub-query whose results are used for the NOT IN comparison
     * @return a new {@link Criteria} with {@link Operator#NOT_IN}
     */
    public static Criteria notIn(Column column, SelectQuery selectQuery)
    {
        return create(column, selectQuery, Operator.NOT_IN);
    }

    /**
     * Creates a criteria for a greater-than-or-equal comparison with a numeric value.
     *
     * @param column the {@link Column} to compare
     * @param value  the numeric value to compare against
     * @return a new {@link Criteria} with {@link Operator#GREATER_EQUAL}
     */
    public static Criteria ge(Column column, Number value)
    {
        return create(column, value, Operator.GREATER_EQUAL);
    }

    /**
     * Creates a criteria for a less-than-or-equal comparison with a numeric value.
     *
     * @param column the {@link Column} to compare
     * @param value  the numeric value to compare against
     * @return a new {@link Criteria} with {@link Operator#LESS_EQUAL}
     */
    public static Criteria le(Column column, Number value)
    {
        return create(column, value, Operator.LESS_EQUAL);
    }

    /**
     * Creates a criteria for a less-than comparison with a numeric value.
     *
     * @param column the {@link Column} to compare
     * @param value  the numeric value to compare against
     * @return a new {@link Criteria} with {@link Operator#LESS_THAN}
     */
    public static Criteria lt(Column column, Number value)
    {
        return create(column, value, Operator.LESS_THAN);
    }

    /**
     * Creates a criteria for a greater-than comparison with a numeric value.
     *
     * @param column the {@link Column} to compare
     * @param value  the numeric value to compare against
     * @return a new {@link Criteria} with {@link Operator#GREATER_THAN}
     */
    public static Criteria gt(Column column, Number value)
    {
        return create(column, value, Operator.GREATER_THAN);
    }

    /**
     * Creates a criteria for a greater-than comparison using a SQL function.
     *
     * @param column the {@link Column} to compare
     * @param value  the {@link SqlFunction.Builder} whose built result is compared against
     * @return a new {@link Criteria} with {@link Operator#GREATER_THAN}
     */
    public static Criteria gt(Column column, SqlFunction.Builder value) {
        return create(column, value.build(), Operator.GREATER_THAN);
    }

    /**
     * Creates a criteria for a greater-than-or-equal comparison using a SQL function.
     *
     * @param column the {@link Column} to compare
     * @param value  the {@link SqlFunction.Builder} whose built result is compared against
     * @return a new {@link Criteria} with {@link Operator#GREATER_EQUAL}
     */
    public static Criteria ge(Column column, SqlFunction.Builder value) {
        return create(column, value.build(), Operator.GREATER_EQUAL);
    }

    /**
     * Creates a criteria for a less-than comparison using a SQL function.
     *
     * @param column the {@link Column} to compare
     * @param value  the {@link SqlFunction.Builder} whose built result is compared against
     * @return a new {@link Criteria} with {@link Operator#LESS_THAN}
     */
    public static Criteria lt(Column column, SqlFunction.Builder value) {
        return create(column, value.build(), Operator.LESS_THAN);
    }

    /**
     * Creates a criteria for a BETWEEN comparison with two numeric values.
     *
     * @param column the {@link Column} to compare
     * @param start  the start of the range (inclusive)
     * @param end    the end of the range (inclusive)
     * @return a new {@link Criteria} with {@link Operator#BETWEEN}
     */
    public static Criteria between(Column column, Number start, Number end)
    {
        return create(column, new Number[]{start, end}, Operator.BETWEEN);
    }
    
    /**
     * Creates a criteria for a less-than-or-equal comparison using a SQL function.
     *
     * @param column the {@link Column} to compare
     * @param value  the {@link SqlFunction.Builder} whose built result is compared against
     * @return a new {@link Criteria} with {@link Operator#LESS_EQUAL}
     */
    public static Criteria le(Column column, SqlFunction.Builder value) {
        return create(column, value.build(), Operator.LESS_EQUAL);
    }

    /**
     * Creates a criteria for a BETWEEN comparison using two SQL functions.
     *
     * @param column the {@link Column} to compare
     * @param start  the {@link SqlFunction.Builder} representing the start of the range
     * @param end    the {@link SqlFunction.Builder} representing the end of the range
     * @return a new {@link Criteria} with {@link Operator#BETWEEN}
     */
    public static Criteria between(Column column, SqlFunction.Builder start, SqlFunction.Builder end)
    {
        return create(column, new SqlFunction[]{start.build(), end.build()}, Operator.BETWEEN);
    }

    /**
     * Creates a criteria for a NOT BETWEEN comparison with two numeric values.
     *
     * @param column the {@link Column} to compare
     * @param start  the start of the range (inclusive)
     * @param end    the end of the range (inclusive)
     * @return a new {@link Criteria} with {@link Operator#NOT_BETWEEN}
     */
    public static Criteria notBetween(Column column, Number start, Number end)
    {
        return create(column, new Number[]{start, end}, Operator.NOT_BETWEEN);
    }

    /**
     * <p>
     * Creates a new Criteria object that specifies a greater than comparison between the given column
     * and the result of the specified sub-query.
     * The sub-query must return a single value.
     * </p>
     *
     * @param column      the {@link Column} to compare with the sub-query result
     * @param selectQuery the sub-query to execute
     * @return a new Criteria object that specifies a greater than comparison
     */
    public static Criteria gt(final Column column, final SelectQuery selectQuery) {
        return create(column, selectQuery, Operator.GREATER_THAN);
    }

    /**
     * <p>
     * Creates a new Criteria object that specifies a less than comparison between the given column
     * and the result of the specified sub-query.
     * The sub-query must return a single value.
     * </p>
     *
     * @param column      the {@link Column} to compare with the sub-query result
     * @param selectQuery the sub-query to execute
     * @return a new Criteria object that specifies a less than comparison
     */
    public static Criteria lt(final Column column, final SelectQuery selectQuery) {
        return create(column, selectQuery, Operator.LESS_THAN);
    }

    /**
     * <p>
     * Creates a new Criteria object that specifies a greater than or equal comparison between the given column
     * and the result of the specified sub-query.
     * The sub-query must return a single value.
     * </p>
     *
     * @param column      the {@link Column} to compare with the sub-query result
     * @param selectQuery the sub-query to execute
     * @return a new Criteria object that specifies a less than comparison
     */
    public static Criteria ge(final Column column, final SelectQuery selectQuery) {
        return create(column, selectQuery, Operator.GREATER_EQUAL);
    }

    /**
     * <p>
     * Creates a new Criteria object that specifies a less than or equal comparison between the given column
     * and the result of the specified sub-query.
     * The sub-query must return a single value.
     * </p>
     *
     * @param column      the {@link Column} to compare with the sub-query result
     * @param selectQuery the sub-query to execute
     * @return a new Criteria object that specifies a less than comparison
     */
    public static Criteria le(final Column column, final SelectQuery selectQuery) {
        return create(column, selectQuery, Operator.LESS_EQUAL);
    }

    /**
     * Creates a criteria for checking existence of rows returned by a sub-query.
     *
     * @param selectQuery the sub-query to check for existence
     * @return a new {@link Criteria} with {@link Operator#EXISTS}
     */
    public static Criteria exists(final SelectQuery selectQuery) {
        return new Criteria(null, selectQuery, Operator.EXISTS);
    }

    /**
     * Creates a criteria for checking existence of rows returned by a CTE sub-query.
     *
     * @param cteSelectQuery the CTE sub-query to check for existence
     * @return a new {@link Criteria} with {@link Operator#EXISTS}
     */
    public static Criteria exists(final CTESelectQuery cteSelectQuery) {
        return new Criteria(null, cteSelectQuery, Operator.EXISTS);
    }

    /**
     * <p>
     * Creates a new {@link Criteria} object that applies an ends with condition for the specified column.
     * </p>
     *
     * @param column The {@link Column} on which the ends with condition is applied
     * @param value  The specific value used as the column data in the ends with condition
     * @return A new Criteria object that specifies ends with comparison
     */
    public static Criteria endsWith( final @NonNull Column column, final Object value )
    {
        return create( column, value, Operator.ENDS_WITH );
    }
}
