package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.sql.common.api.dml.function.FunctionArgument;
import com.tlc.sql.common.api.dml.function.NumberValue;
import com.tlc.sql.common.api.dml.function.PredefinedSqlFunctionBuilderFactory;
import com.tlc.sql.common.api.dml.function.SimpleArgument;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.dml.function.StringValue;
import com.tlc.sql.common.api.handler.DMLHandler;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.handler.AbstractDMLHandler;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractColumn implements Column {

    @Getter
    protected final Table table;
    @Getter
    protected final String columnName;
    @Getter
    protected final String columnAlias;
    @Getter
    protected Column.ColumnType columnType;

    protected Integer hashCode = null;
    @Getter
    private final ColumnDefinition columnDefinition;
    private final boolean hasAlias;
    @Getter
    private final boolean isPkColumn;

    private SqlFunction function;
    private Expression expression;

    protected AbstractColumn(final Table table, final String name, final String alias, final boolean isPkColumn) {
        this.columnDefinition = table.getTableDefinition().getColumnDefinition(name);
        if (columnDefinition == null) {
            throw ErrorCode.get(SQLErrorCodes.DB_ROW_UNKNOWN_COLUMN, name);
        }
        this.isPkColumn = isPkColumn;
        this.table = Objects.requireNonNull(table);
        this.columnName = Objects.requireNonNull(name).trim();
        this.columnAlias = Objects.requireNonNull(alias).trim();
        this.columnType = ColumnType.NORMAL;
        this.hasAlias = !columnName.equals(columnAlias);
    }

    /**
     * Constructor that accepts column definition directly, used for derived table columns
     * where the table doesn't have a TableDefinition with column info.
     *
     * @param table the table (can be a DerivedTable)
     * @param columnDefinition the column definition from the source column
     * @param name the column name
     * @param alias the column alias
     */
    protected AbstractColumn(final Table table, final ColumnDefinition columnDefinition, final String name, final String alias) {
        this.columnDefinition = Objects.requireNonNull(columnDefinition);
        this.isPkColumn = false;
        this.table = Objects.requireNonNull(table);
        this.columnName = Objects.requireNonNull(name).trim();
        this.columnAlias = Objects.requireNonNull(alias).trim();
        this.columnType = ColumnType.NORMAL;
        this.hasAlias = !columnName.equals(columnAlias);
    }

    /**
     * Constructor that conditionally bypasses table-definition lookup.
     * When {@code isDerivedTable} is true, uses the source column's existing
     * {@link ColumnDefinition} and table directly. When false, delegates to the
     * standard table-definition lookup.
     *
     * @param sourceColumn   the source column providing table, name, and definition
     * @param alias          the alias for this column
     * @param isDerivedTable true to bypass table-definition lookup
     */
    protected AbstractColumn(final Column sourceColumn, final String alias, final boolean isDerivedTable) {
        if (isDerivedTable) {
            this.columnDefinition = Objects.requireNonNull(sourceColumn.getColumnDefinition());
            this.table = Objects.requireNonNull(sourceColumn.getTable());
        } else {
            this.columnDefinition = sourceColumn.getTable().getTableDefinition()
                    .getColumnDefinition(sourceColumn.getColumnName());
            if (this.columnDefinition == null) {
                throw ErrorCode.get(SQLErrorCodes.DB_ROW_UNKNOWN_COLUMN, sourceColumn.getColumnName());
            }
            this.table = Objects.requireNonNull(sourceColumn.getTable());
        }
        this.isPkColumn = false;
        this.columnName = Objects.requireNonNull(sourceColumn.getColumnName()).trim();
        this.columnAlias = Objects.requireNonNull(alias).trim();
        this.columnType = ColumnType.NORMAL;
        this.hasAlias = !columnName.equals(columnAlias);
    }

    /**
     * Constructor for wrapping an existing column with a different column type.
     * Used for aggregate operations (COUNT, SUM, etc.) and by ConditionalColumn subclasses.
     *
     * @param column the source column to wrap
     * @param type   the column type to apply
     */
    protected AbstractColumn(final Column column, final Column.ColumnType type) {
        this.table = column.getTable();
        this.isPkColumn = column.isPkColumn();
        this.hasAlias = type != ColumnType.NORMAL;
        this.columnName = column.getColumnName();
        this.columnAlias = column.getColumnAlias();
        this.columnType = Objects.requireNonNull(type);
        this.columnDefinition = column.getColumnDefinition();
    }

    protected AbstractColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function) {
        this(table, columnDefinition, function, false);
    }

    protected AbstractColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function, final boolean isPkColumn) {
        this.table = table;
        this.isPkColumn = isPkColumn;
        this.columnName = columnDefinition.getColumnName();
        this.columnAlias = columnName;
        this.columnType = ColumnType.FUNCTION;
        this.function = function;
        this.columnDefinition = columnDefinition;
        this.hasAlias = Objects.nonNull(function);
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the column has an alias, false otherwise
     */
    public boolean hasAlias() {
        return hasAlias;
    }

    /**
     * {@inheritDoc}
     *
     * @return the table name
     */
    public String getTableName() {
        return table.getName();
    }


    /**
     * {@inheritDoc}
     *
     * @return a new column representing COUNT(column)
     */
    public Column count() {
        return buildFunctionColumn(PredefinedSqlFunctionBuilderFactory.count().addArguments(this).build(), this.columnAlias);
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing COUNT(DISTINCT column)
     */
    public Column countWithDistinct() {
        return buildAggregateWithDistinct(PredefinedSqlFunctionBuilderFactory.count());
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing MAX(column)
     */
    public Column max() {
        return buildAggregateOnSelf(PredefinedSqlFunctionBuilderFactory.max());
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing MIN(column)
     */
    public Column min() {
        return buildAggregateOnSelf(PredefinedSqlFunctionBuilderFactory.min());
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing AVG(column)
     */
    public Column average() {
        return buildAggregateOnSelf(PredefinedSqlFunctionBuilderFactory.avg());
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing AVG(DISTINCT column)
     */
    public Column averageWithDistinct() {
        return buildAggregateWithDistinct(PredefinedSqlFunctionBuilderFactory.avg());
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing DISTINCT column
     */
    public Column distinct() {
        return buildAggregateOnSelf(PredefinedSqlFunctionBuilderFactory.distinct());
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing SUM(column)
     */
    public Column sum() {
        return buildAggregateOnSelf(PredefinedSqlFunctionBuilderFactory.sum());
    }

    /**
     * {@inheritDoc}
     *
     * @param functionBuilder the SQL function builder
     * @param aliasName the alias for the resulting column
     * @param argumentPosition the position to insert the column as an argument
     * @return a new column with the function applied
     */
    @Override
    public Column function(final SqlFunction.Builder functionBuilder, final String aliasName, final int argumentPosition) {
        return buildFunctionColumn(functionBuilder.insertArgument(argumentPosition, this).build(), aliasName);
    }

    /**
     * {@inheritDoc}
     *
     * @param functionBuilder the SQL function builder
     * @param argument the additional argument
     * @param aliasName the alias for the resulting column
     * @param argumentPosition the position to insert the argument
     * @return a new column with the function applied
     */
    @Override
    public Column function(final SqlFunction.Builder functionBuilder, final Argument argument, final String aliasName, final int argumentPosition) {
        return buildFunctionColumn(functionBuilder.insertArgument(argumentPosition, argument).build(), aliasName);
    }

    /**
     * {@inheritDoc}
     *
     * @param functionName the name of the SQL function
     * @param aliasName the alias for the resulting column
     * @return a new column with the function applied
     */
    @Override
    public Column function(final String functionName, final String aliasName) {
        return buildFunctionColumn(SqlFunction.Builder.create().addArguments(this).setName(functionName).build(), aliasName);
    }

    protected Column buildFunctionColumn(final SqlFunction function, final String aliasName) {
        final DataType returnType = function.getReturnType();
        final ColumnDefinition functionColumnDefinition;

        if (CommonDataType.DECIMAL == returnType) {
            functionColumnDefinition = buildDecimalColumnDefinition(aliasName);
        } else {
            functionColumnDefinition = buildColumnDefinition(aliasName, returnType);
        }
        return buildFunctionColumn(table, functionColumnDefinition, function, this.isPkColumn);
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing TRIM(column)
     */
    public Column trim() {
        return buildFunctionColumn(PredefinedSqlFunctionBuilderFactory.trim().addArguments(this)
                .setReturnType(getTextDataType()).build(), this.columnAlias);
    }

    /**
     * {@inheritDoc}
     *
     * @param columnsToBeConcat the columns to concatenate
     * @return a new column representing the concatenated and trimmed result
     */
    public Column concatWithTrim(final Column... columnsToBeConcat) {
        return concatWithTrim(StringUtil.Delimiter.EMPTY_SPACE, columnsToBeConcat);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds: {@code LOWER(CONCAT(TRIM(this), 'sep', col2, 'sep', col3, ...))}</p>
     *
     * @param concatenationSeparator the delimiter to use between values
     * @param columnsToBeConcat the columns to concatenate
     * @return a new column representing the concatenated and trimmed result
     */
    public Column concatWithTrim(final StringUtil.Delimiter concatenationSeparator, final Column... columnsToBeConcat) {
        final SqlFunction trimFunction = PredefinedSqlFunctionBuilderFactory.trim()
                .addArguments(this)
                .setReturnType(getTextDataType())
                .build();

        return buildConcatFunction(FunctionArgument.create(trimFunction), concatenationSeparator, columnsToBeConcat);
    }

    /**
     * {@inheritDoc}
     *
     * @param columnsToBeConcat the columns to concatenate
     * @return a new column representing the concatenation
     */
    public Column concat(final Column... columnsToBeConcat) {
        return concat(StringUtil.Delimiter.EMPTY_SPACE, columnsToBeConcat);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds: {@code LOWER(CONCAT(this, 'sep', col2, 'sep', col3, ...))}</p>
     *
     * @param concatenationSeparator the delimiter to use between values
     * @param columnsToBeConcat the columns to concatenate
     * @return a new column representing the concatenation
     */
    public Column concat(final StringUtil.Delimiter concatenationSeparator, final Column... columnsToBeConcat) {
        return buildConcatFunction(SimpleArgument.of(this), concatenationSeparator, columnsToBeConcat);
    }

    /**
     * {@inheritDoc}
     *
     * @return a new column representing SUM(DISTINCT column)
     */
    public Column sumWithDistinct() {
        return buildAggregateWithDistinct(PredefinedSqlFunctionBuilderFactory.sum());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column divide(final long value) {
        return buildArithmeticFunctionColumn(Operator.DIVIDE, new NumberValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column multiply(final long value) {
        return buildArithmeticFunctionColumn(Operator.MULTIPLY, new NumberValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column add(final Column other) {
        return buildArithmeticFunctionColumn(Operator.ADD, SimpleArgument.of(other));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column subtract(final Column other) {
        return buildArithmeticFunctionColumn(Operator.SUBTRACT, SimpleArgument.of(other));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column add(final long value) {
        return buildArithmeticFunctionColumn(Operator.ADD, new NumberValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column subtract(final long value) {
        return buildArithmeticFunctionColumn(Operator.SUBTRACT, new NumberValue(value));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column coalesce(final List<Column> columns) {
        return buildCoalesceFunction(
                SimpleArgument.of(this),
                columns.stream().map(SimpleArgument::of).collect(Collectors.toUnmodifiableSet())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column coalesce(final Column... columns) {
        return buildCoalesceFunction(
                SimpleArgument.of(this),
                Arrays.stream(columns).map(SimpleArgument::of).collect(Collectors.toUnmodifiableSet())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column coalesce(final long... values) {
        return buildCoalesceFunction(
                SimpleArgument.of(this),
                Arrays.stream(values).mapToObj(NumberValue::new).map(SimpleArgument::of).collect(Collectors.toUnmodifiableSet())
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Column coalesce(final String... values) {
        return buildCoalesceFunction(
                SimpleArgument.of(this),
                Arrays.stream(values).map(StringValue::new).map(SimpleArgument::of).collect(Collectors.toUnmodifiableSet())
        );
    }

    /**
     * Builds a new EXPRESSION column wrapping a binary arithmetic expression.
     *
     * @param operator the arithmetic operator
     * @param rightOperand the right-hand operand as an {@link Argument}
     * @return a new column representing the arithmetic operation
     */
    private Column buildArithmeticFunctionColumn(final Operator operator, final Argument rightOperand) {
        final Argument leftOperand = getLeftOperand();
        final BinaryExpression binaryExpression = BinaryExpression.create(leftOperand, operator, rightOperand);

        return buildExpressionColumn(binaryExpression);
    }

    /**
     * Builds a new EXPRESSION column wrapping a binary arithmetic expression
     * where the right operand is a {@link NumberValue} constant.
     *
     * @param operator the arithmetic operator
     * @param numberValue the numeric constant operand
     * @return a new column representing the arithmetic operation
     */
    private Column buildArithmeticFunctionColumn(final Operator operator, final NumberValue numberValue) {
        return buildArithmeticFunctionColumn(operator, SimpleArgument.of(numberValue));
    }

    /**
     * Returns this column as an {@link Argument} suitable for use in expressions.
     * If this column is already an EXPRESSION column, uses the existing
     * expression to allow proper chaining of arithmetic operations.
     *
     * @return an {@link Argument} representing this column
     */
    private Argument getLeftOperand() {
        if (Objects.equals(ColumnType.EXPRESSION, columnType) && Objects.nonNull(expression)) {
            return expression;
        }
        return SimpleArgument.of(this);
    }

    /**
     * Builds a new column that wraps an {@link Expression}.
     *
     * @param expression the expression to wrap
     * @return a new column representing the expression
     */
    protected abstract Column buildExpressionColumn(final Expression expression);

    /**
     * {@inheritDoc}
     */
    @Override
    public Expression getExpression() {
        return expression;
    }

    /**
     * Sets the expression for this column.
     *
     * @param expression the expression to set
     */
    protected void setExpression(final Expression expression) {
        this.expression = expression;
    }

    /**
     * {@inheritDoc}
     *
     * @return the SQL function
     */
    @Override
    public SqlFunction getFunction() {
        return function;
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public boolean isFunctionApplied(final String functionName) {
        if (Objects.isNull(function) || Objects.isNull(functionName)) {
            return false;
        }

        if (matchesFunctionName(function, functionName)) {
            return true;
        }

        return function.getArguments().stream()
                .anyMatch(argument -> argument instanceof FunctionArgument functionArgument
                        && matchesFunctionName(functionArgument.getFunction(), functionName));
    }

    /**
     * Returns {@code true} when the given {@link SqlFunction} is named {@code functionName}
     * (case-insensitive).
     *
     * @param sqlFunction  the function to inspect; may be {@code null}
     * @param functionName the function name to match against
     * @return {@code true} if {@code sqlFunction} carries the given name, {@code false} otherwise
     */
    private static boolean matchesFunctionName(final SqlFunction sqlFunction, final String functionName) {
        return Objects.nonNull(sqlFunction) && functionName.equalsIgnoreCase(sqlFunction.getName());
    }

    @Override
    public String toString() {
        if (columnName.equals(columnAlias)) {
            return columnAlias;
        } else {
            return columnName + "_" + columnAlias;
        }
    }

    @Override
    public boolean equals(Object object) {

        if (object instanceof Column) {
            return this.hashCode() == object.hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == null) {
            final int prime = 31;
            int result = 1;
            result = prime * result + table.hashCode();
            result = prime * result + columnName.hashCode();
            result = prime * result + columnAlias.hashCode();
            result = prime * result + columnType.getOrder();
            this.hashCode = result;
        }
        return hashCode;
    }

    @Override
    public int compareTo(Column column) {
        final int comparableColumnTypeOrder = column.getColumnType().getOrder();
        final int columnTypeOrder = this.columnType.getOrder();

        if (comparableColumnTypeOrder > columnTypeOrder) {
            return 1;
        } else if (comparableColumnTypeOrder == columnTypeOrder) {
            return equals(column) ? 0 : Integer.compare(hashCode(), column.hashCode());
        } else {
            return -1;
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
        return handler.getColumnString(this, needColumnAlias);
    }
    /**
     * {@inheritDoc}
     * <p>
     * For {@link ColumnType#FUNCTION} columns, the associated data type is resolved from
     * the {@link SqlFunction#getReturnType()}. For aggregate column types (COUNT, MAX, MIN, AVG, SUM)
     * used by {@link ConditionalColumn}, the data type is resolved directly.
     * </p>
     *
     * @param columnType the column type to check
     * @return true if the column type has an associated data type, false otherwise
     */
    @Override
    public boolean hasAssociatedDataType(final ColumnType columnType) {
        if (columnType == ColumnType.FUNCTION) {
            return function != null && function.getReturnType() != null;
        }
        return isAggregateColumnType(columnType);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For {@link ColumnType#FUNCTION} columns, returns the return type from the {@link SqlFunction}.
     * For aggregate column types (COUNT, MAX, MIN, AVG, SUM) used by {@link ConditionalColumn},
     * returns {@link CommonDataType#BIGINT}.
     * </p>
     *
     * @param columnType the column type to retrieve the data type for
     * @return an Optional containing the associated data type, or empty if none exists
     */
    @Override
    public Optional<DataType> getAssociatedDataType(final ColumnType columnType) {
        if (columnType == ColumnType.FUNCTION) {
            return Optional.ofNullable(function != null ? function.getReturnType() : null);
        }
        if (isAggregateColumnType(columnType)) {
            return Optional.of(CommonDataType.BIGINT);
        }
        return Optional.empty();
    }

    /**
     * Checks whether the given column type is a numeric aggregate type.
     * These types are still used by {@link ConditionalColumn} subclasses which
     * wrap CASE expressions with aggregate functions (e.g., {@code SUM(CASE ... END)}).
     *
     * @param columnType the column type to check
     * @return true if the column type is an aggregate type
     */
    private static boolean isAggregateColumnType(final ColumnType columnType) {
        return columnType == ColumnType.COUNT || columnType == ColumnType.MAX
                || columnType == ColumnType.MIN || columnType == ColumnType.AVG
                || columnType == ColumnType.SUM;
    }

    /**
     * Builds a new column with the specified column type.
     *
     * @param columnType the type of the column
     * @return the newly built column
     */
    protected abstract Column buildColumn(final Column.ColumnType columnType);

    /**
     * Builds a function column with the specified table, column definition, and SQL function.
     *
     * @param table the table associated with the column
     * @param columnDefinition the column definition
     * @param function the SQL function applied to the column
     * @return the newly built function column
     */
    protected abstract Column buildFunctionColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function);

    /**
     * Builds a function column with the given table, column definition, SQL function, and PK flag.
     *
     * @param table the table associated with the column
     * @param columnDefinition the column definition
     * @param function the SQL function applied to the column
     * @param isPkColumn whether the source column is a primary key column
     * @return the newly built function column
     */
    protected abstract Column buildFunctionColumn(final Table table, final ColumnDefinition columnDefinition, final SqlFunction function, final boolean isPkColumn);

    /**
     * Builds a column definition with the specified alias name and return type.
     *
     * @param aliasName the alias name for the column definition
     * @param returnType the return type of the column definition
     * @return the newly built column definition
     */
    protected abstract ColumnDefinition buildColumnDefinition(final String aliasName, final DataType returnType);

    /**
     * Builds a decimal column definition with the specified alias name.
     *
     * @param aliasName the alias name for the decimal column definition
     * @return the newly built column definition
     */
    protected abstract ColumnDefinition buildDecimalColumnDefinition(final String aliasName);

    /**
     * Returns the text-like {@link DataType} appropriate for this column's database platform.
     * Used when building CONCAT/TRIM SQL functions.
     *
     * @return the text data type for this platform (e.g., CHAR for OLTP, TEXT for OLAP)
     */
    protected abstract DataType getTextDataType();

    /**
     * Builds an aggregate function with DISTINCT modifier using {@link SqlFunction}
     * and {@link FunctionArgument}.
     * <p>Produces standard SQL syntax: {@code FUNC(DISTINCT(column))},
     * e.g., {@code COUNT(DISTINCT(col))}, {@code SUM(DISTINCT(col))}.</p>
     *
     * <p><b>Note:</b> For databases that use different functions for distinct aggregation
     * (e.g., ClickHouse uses {@code uniqExact()}), override the calling method
     * or use the platform-specific factory.</p>
     *
     * @param aggregateBuilder the pre-configured aggregate function builder from
     *                         {@link PredefinedSqlFunctionBuilderFactory}
     * @return a new column representing the aggregate with DISTINCT
     */
    private Column buildAggregateWithDistinct(final SqlFunction.Builder aggregateBuilder) {
        final SqlFunction distinctFunction = PredefinedSqlFunctionBuilderFactory.distinct()
                .addArguments(this)
                .setReturnType(CommonDataType.BIGINT)
                .build();

        final SqlFunction sqlFunction = aggregateBuilder
                .addArgument(FunctionArgument.create(distinctFunction))
                .setReturnType(getColumnDataType())
                .build();

        return buildFunctionColumn(sqlFunction, this.columnAlias);
    }

    /**
     * Builds an aggregate (or unary) SQL function applied directly to this column
     *
     * @param functionBuilder the pre-configured function builder from
     *                        {@link PredefinedSqlFunctionBuilderFactory}
     * @return a new column representing the function applied to this column
     */
    private Column buildAggregateOnSelf(final SqlFunction.Builder functionBuilder) {
        final SqlFunction sqlFunction = functionBuilder
                .addArguments(this)
                .setReturnType(getColumnDataType())
                .build();

        return buildFunctionColumn(sqlFunction, this.columnAlias);
    }

    /**
     * Convenience accessor for this column's underlying {@link DataType}.
     *
     * @return the data type from this column's {@link ColumnDefinition}
     */
    private DataType getColumnDataType() {
        return this.columnDefinition.getDataType();
    }

    /**
     * Builds a {@code LOWER(CONCAT(firstArg, 'sep', col2, 'sep', col3, ...))} SQL function.
     *
     * @param firstArgument the first argument to CONCAT (may be a TRIM-wrapped or plain column reference)
     * @param separator     the delimiter between concatenated values
     * @param columnsToBeConcat the additional columns to concatenate
     * @return a new column representing the LOWER(CONCAT(...)) expression
     */
    private Column buildConcatFunction(final Argument firstArgument, final StringUtil.Delimiter separator,
                                       final Column... columnsToBeConcat) {
        final DataType textDataType = getTextDataType();

        final SqlFunction.Builder concatBuilder = PredefinedSqlFunctionBuilderFactory.concat()
                .setReturnType(textDataType)
                .addArgument(firstArgument);

        for (final Column col : columnsToBeConcat) {
            concatBuilder.addArgument(SimpleArgument.of(new StringValue(separator.getDelimiter())));
            concatBuilder.addArgument(SimpleArgument.of(col));
        }

        final SqlFunction lowerFunction = PredefinedSqlFunctionBuilderFactory.lower()
                .setReturnType(textDataType)
                .addArgument(FunctionArgument.create(concatBuilder.build()))
                .build();

        return buildFunctionColumn(lowerFunction, this.columnAlias);
    }

    /**
     * Builds a {@code COALESCE(firstArg, col2, col3, ...)} SQL function.
     *
     * @param firstArgument the first argument to COALESCE (may be a TRIM-wrapped or plain column reference)
     * @param coalesceArguments the additional arguments to COALESCE
     * @return a new column representing the COALESCE(...) expression
     */
    private Column buildCoalesceFunction(final Argument firstArgument, final Collection<Argument> coalesceArguments) {
        final DataType dataType = getColumnDataType();

        final SqlFunction.Builder coalesceBuilder = PredefinedSqlFunctionBuilderFactory.coalesce()
                .setReturnType(dataType)
                .addArgument(firstArgument);

        for (final Argument coalesceArgument : coalesceArguments) {
            coalesceBuilder.addArgument(coalesceArgument);
        }

        return buildFunctionColumn(coalesceBuilder.build(), this.columnAlias);
    }
}
