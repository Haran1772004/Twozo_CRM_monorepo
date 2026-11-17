package com.tlc.sql.common.api.dml;

import com.tlc.commons.util.StringUtil;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;
import lombok.Getter;

import java.util.List;
import java.util.Optional;

/**
 * @author Abishek
 * @version 1.0
 */
public interface Column extends Comparable<Column> {

    /**
     * Checks if the column is a primary key column.
     *
     * @return true if the column is a primary key, false otherwise
     */
    boolean isPkColumn();

    /**
     * Retrieves the name of the column.
     *
     * @return the column name
     */
    String getColumnName();

    /**
     * Retrieves the alias of the column.
     *
     * @return the column alias
     */
    String getColumnAlias();

    /**
     * Checks if the column has an alias.
     *
     * @return true if the column has an alias, false otherwise
     */
    boolean hasAlias();

    /**
     * Retrieves the table associated with the column.
     *
     * @return the table
     */
    Table getTable();

    /**
     * Retrieves the name of the table associated with the column.
     *
     * @return the table name
     */
    String getTableName();

    /**
     * Creates a new column that applies a COUNT aggregation.
     *
     * @return a new column representing COUNT(column)
     */
    Column count();


    /**
     * Creates a new column that applies a COUNT aggregation with DISTINCT
     * to count unique values.
     *
     * @return a new column representing COUNT(DISTINCT column)
     */
    Column countWithDistinct();

    /**
     * Creates a new column that applies a MAX aggregation.
     *
     * @return a new column representing MAX(column)
     */
    Column max();

    /**
     * Creates a new column that applies a MIN aggregation.
     *
     * @return a new column representing MIN(column)
     */
    Column min();

    /**
     * Creates a new column that applies an AVG aggregation.
     *
     * @return a new column representing AVG(column)
     */
    Column average();

    /**
     * Creates a new column that applies an AVG aggregation with DISTINCT
     * to compute the average of unique values.
     *
     * @return a new column representing AVG(DISTINCT column)
     */
    Column averageWithDistinct();

    /**
     * Creates a new column that applies a DISTINCT modifier.
     *
     * @return a new column representing DISTINCT column
     */
    Column distinct();

    /**
     * Applies a SQL function to the column, inserting it at the specified argument position.
     *
     * @param functionBuilder the SQL function builder
     * @param aliasName the alias for the resulting column
     * @param argumentPosition the position to insert the column as an argument
     * @return a new column with the function applied
     */
    Column function(final SqlFunction.Builder functionBuilder, final String aliasName, final int argumentPosition);

    /**
     * Applies a SQL function to the column with an additional argument, inserting at the specified position.
     *
     * @param functionBuilder the SQL function builder
     * @param argument the additional argument
     * @param aliasName the alias for the resulting column
     * @param argumentPosition the position to insert the argument
     * @return a new column with the function applied
     */
    Column function(final SqlFunction.Builder functionBuilder, final Argument argument, final String aliasName, final int argumentPosition);

    /**
     * Applies a named SQL function to the column.
     *
     * @param functionName the name of the SQL function
     * @param aliasName the alias for the resulting column
     * @return a new column with the function applied
     */
    Column function(final String functionName, final String aliasName);

    /**
     * Creates a new column that applies a SUM aggregation.
     *
     * @return a new column representing SUM(column)
     */
    Column sum();

    /**
     * Creates a new column that applies a TRIM function to remove leading and trailing whitespace.
     *
     * @return a new column representing TRIM(column)
     */
    Column trim();

    /**
     * Concatenates this column with the specified columns using a default space separator
     * and applies a TRIM operation to the result.
     *
     * @param columnsToBeConcat the columns to concatenate
     * @return a new column representing the concatenated and trimmed result
     */
    Column concatWithTrim(final Column... columnsToBeConcat);

    /**
     * Concatenates this column with the specified columns using the given separator
     * and applies a TRIM operation to the result.
     *
     * @param concatenationSeparator the delimiter to use between values
     * @param columnsToBeConcat the columns to concatenate
     * @return a new column representing the concatenated and trimmed result
     */
    Column concatWithTrim(final StringUtil.Delimiter concatenationSeparator, final Column... columnsToBeConcat);

    /**
     * Concatenates this column with the specified columns using a default space separator.
     *
     * @param columnsToBeConcat the columns to concatenate
     * @return a new column representing the concatenation
     */
    Column concat(final Column... columnsToBeConcat);

    /**
     * Concatenates this column with the specified columns using the given separator.
     *
     * @param concatenationSeparator the delimiter to use between values
     * @param columnsToBeConcat the columns to concatenate
     * @return a new column representing the concatenation
     */
    Column concat(final StringUtil.Delimiter concatenationSeparator, final Column... columnsToBeConcat);

    /**
     * Creates a new column that applies a SUM aggregation with DISTINCT
     * to sum unique values.
     *
     * @return a new column representing SUM(DISTINCT column)
     */
    Column sumWithDistinct();

    /**
     * Retrieves the column type.
     *
     * @return the column type
     */
    ColumnType getColumnType();


    /**
     * Retrieves the column definition.
     *
     * @return the column definition
     */
    ColumnDefinition getColumnDefinition();

    /**
     * Checks if the specified column type has an associated data type.
     *
     * @param columnType the column type to check
     * @return true if the column type has an associated data type, false otherwise
     */
    boolean hasAssociatedDataType(final ColumnType columnType);

    /**
     * Retrieves the associated data type for the specified column type.
     *
     * @param columnType the column type to retrieve the data type for
     * @return an Optional containing the associated data type, or empty if none exists
     */
    Optional<DataType> getAssociatedDataType(final ColumnType columnType);

    /**
     * Retrieves the SQL function applied to the column.
     *
     * @return the SQL function
     */
    SqlFunction getFunction();

    /**
     * Checks whether this column is decorated with the given SQL function — either
     * directly or as the argument of an enclosing function.
     *
     * @param functionName the SQL function name to look for
     * @return {@code true} if this column carries the named function, {@code false} otherwise
     */
    boolean isFunctionApplied(final String functionName);

    /**
     * Retrieves the arithmetic expression associated with this column.
     *
     * @return the expression, or null if none is set
     */
    Expression getExpression();

    /**
     * Creates a JSON column with the specified alias.
     *
     * @param columnAlias the alias for the JSON column
     * @return the newly created JSON column
     */
    JsonColumn jsonColumn(final String columnAlias);

    /**
     * Creates a JSON column with the specified alias and return type.
     *
     * @param columnAlias the alias for the JSON column
     * @param returnType the return type of the JSON column
     * @return the newly created JSON column
     */
    JsonColumn jsonColumn(final String columnAlias, final DataType returnType);

    /**
     * Creates a JSON column with the specified alias, return type, and array flag.
     *
     * @param columnAlias the alias for the JSON column
     * @param returnType the return type of the JSON column
     * @param isArray whether the JSON column is an array
     * @return the newly created JSON column
     */
    JsonColumn jsonColumn(final String columnAlias, final DataType returnType, final boolean isArray);

    /**
     * Creates a JSON decimal column with the specified alias, precision, and scale.
     *
     * @param columnAlias the alias for the JSON decimal column
     * @param precision the precision of the decimal column
     * @param scale the scale of the decimal column
     * @return the newly created JSON decimal column
     */
    JsonColumn jsonDecimalColumn(final String columnAlias, final int precision, final int scale);

    /**
     * Creates a JSON decimal column with the specified alias, precision, scale, and array flag.
     *
     * @param columnAlias the alias for the JSON decimal column
     * @param precision the precision of the decimal column
     * @param scale the scale of the decimal column
     * @param isArray whether the JSON decimal column is an array
     * @return the newly created JSON decimal column
     */
    JsonColumn jsonDecimalColumn(final String columnAlias, final int precision, final int scale, final boolean isArray);

    /**
     * Returns the string representation of this column using the specified DML handler.
     *
     * @param handler the DML handler used to build the column SQL string
     * @return the SQL string representation of this column
     */
    String toString(final DMLHandler handler, final boolean needColumnAlias);

    /**
     * Creates a new column representing the division of this column's value by the specified constant.
     *
     * @param value the divisor
     * @return a new column representing (column / value)
     */
    Column divide(final long value);

    /**
     * Creates a new column representing the multiplication of this column's value by the specified constant.
     *
     * @param value the multiplier
     * @return a new column representing (column * value)
     */
    Column multiply(final long value);

    /**
     * Creates a new column representing the addition of this column's value with another column's value.
     *
     * @param other the column to add
     * @return a new column representing (column + other)
     */
    Column add(final Column other);

    /**
     * Creates a new column representing the subtraction of another column's value from this column's value.
     *
     * @param other the column to subtract
     * @return a new column representing (column - other)
     */
    Column subtract(final Column other);

    /**
     * Creates a new column representing the addition of this column's value with a constant.
     *
     * @param value the value to add
     * @return a new column representing (column + value)
     */
    Column add(final long value);

    /**
     * Creates a new column representing the subtraction of a constant from this column's value.
     *
     * @param value the value to subtract
     * @return a new column representing (column - value)
     */
    Column subtract(final long value);

    /**
     * Creates a new column that represents the COALESCE of this column and a list of columns.
     *
     * @param columns the columns to coalesce with
     * @return a new column representing COALESCE(this, column)
     */
    Column coalesce(final List<Column> columns);

    /**
     * Creates a new column that represents the COALESCE of this column and another column.
     *
     * @param columns the columns to coalesce with
     * @return a new column representing COALESCE(this, column)
     */
    Column coalesce(final Column... columns);

    /**
     * Creates a new column that represents the COALESCE of this column and a constant value.
     *
     * @param values the values to coalesce with
     * @return a new column representing COALESCE(this, value)
     */
    Column coalesce(final long... values);

    /**
     * Creates a new column that represents the COALESCE of this column and string constants.
     *
     * @param values the values to coalesce with
     * @return a new column representing COALESCE(this, value)
     */
    Column coalesce(final String... values);

    enum ColumnType {

        NORMAL(0),
        COUNT(1),
        MAX(2),
        MIN(3),
        AVG(4),
        SUM(5),
        FUNCTION(6),
        EXPRESSION(7);

        @Getter
        private final int order;

        ColumnType(final int order) {
            this.order = order;
        }

        public boolean isNormal() {
            return this == NORMAL;
        }
    }
}