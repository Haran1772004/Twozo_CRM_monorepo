package com.tlc.sql.olap.internal.handler.ch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.handler.AbstractDDLHandler;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.PKDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.meta.TableEngine;
import com.tlc.sql.common.util.DDLDMLUtil;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.internal.handler.OlapDDLHandler;
import com.tlc.sql.olap.internal.meta.ch.CHDataType;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import com.tlc.sql.olap.internal.meta.ch.OrderByDefinition;
import com.tlc.sql.olap.internal.meta.ch.PartitionByColumn;
import com.tlc.sql.olap.internal.meta.ch.PartitionByDefinition;
import com.tlc.sql.olap.internal.meta.ch.PartitionByExpression;
import com.tlc.sql.olap.internal.meta.ch.PartitionByFunction;
import com.tlc.sql.olap.internal.meta.ch.SkipIndexDefinition;
import com.tlc.sql.olap.internal.status.OlapErrorCodes;
import com.tlc.sql.olap.internal.util.CHDdlDmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * DDL handler for ClickHouse table definitions. Generates CREATE TABLE, ALTER TABLE,
 * and index management SQL statements for all supported CH engine types including
 * Distributed and ReplicatedReplacingMergeTree.
 *
 * @author sundar
 * @version 1.0
 */
public class CHDDLHandler extends AbstractDDLHandler<OlapColumnDefinition, CHTableDefinition> implements OlapDDLHandler<CHTableDefinition> {

    private static final DDLDMLUtil.IdentifierQuote CH_IDENTIFIER_QUOTE = DDLDMLUtil.IdentifierQuote.BACK_TICK;
    private static final String DISTRIBUTED_ENGINE_NAME = "Distributed";

    private static final Logger LOGGER = LoggerFactory.getLogger( CHDDLHandler.class );
    /**
     * Generates the CREATE TABLE SQL statement(s) for the given ClickHouse table definition.
     * Dispatches on engine name to handle Distributed tables (which skip PARTITION BY,
     * ORDER BY, PRIMARY KEY) separately from MergeTree-family engines.
     *
     * @param tableDefinition the ClickHouse table definition
     * @return an array of SQL statements constituting the CREATE TABLE operation
     */
    @Override
    public String[] getCreateTableSQL(final CHTableDefinition tableDefinition) {
        final List<String> sqlStatements = new LinkedList<>();
        final StringBuilder createBuffer = new StringBuilder(750);
        final String convertedTableName = getTableName(tableDefinition);

        createBuffer.append("CREATE TABLE ");
        createBuffer.append(convertedTableName);
        tableDefinition.getCluster().ifPresent(cluster -> createBuffer.append(" ON CLUSTER ").append(cluster));
        createBuffer.append(" ( ");

        final Map<String, OlapColumnDefinition> definitions = tableDefinition.getColumnDefinition();

        for (final OlapColumnDefinition columnDefinition : definitions.values()) {
            createBuffer.append(getColumnSQL(columnDefinition)).append(", ");
        }
        final Map<String, SkipIndexDefinition> skipIndexDefinitions = tableDefinition.getSkipIndexDefinitions();

        for (final Map.Entry<String, SkipIndexDefinition> skipIndexDefinitionEntry : skipIndexDefinitions.entrySet()) {
            final String name = skipIndexDefinitionEntry.getKey();
            final SkipIndexDefinition skipIndexDefinition = skipIndexDefinitionEntry.getValue();

            createBuffer.append("INDEX ").append(name).append(" ")
                    .append(skipIndexDefinition.getColumnName()).append(" ")
                    .append("TYPE ").append(skipIndexDefinition.getSkipIndexExpression()).append(", ");
        }
        StringUtil.replaceSuffix(createBuffer, NumericalConstant.TWO, " ) ");

        final TableEngine engine = Objects.requireNonNull(tableDefinition.getEngine());

        if (DISTRIBUTED_ENGINE_NAME.equalsIgnoreCase(engine.getName().split("\\(")[0])) {
            createBuffer.append("ENGINE = ").append(engine.getName()).append(" ");
            sqlStatements.add(createBuffer.toString());
            return sqlStatements.toArray(new String[0]);
        }

        createBuffer.append("ENGINE = ").append(engine.getName()).append(" ");
        createBuffer.append("PARTITION BY (");

        final Collection<PartitionByDefinition> partitionByDefinitions = tableDefinition.getPartitionByDefinitions();

        for (final PartitionByDefinition partitionByDefinition : partitionByDefinitions) {
            final PartitionByExpression partitionByExpression = partitionByDefinition.partitionByExpression();
            final String partitionByParam;

            //FIXME: Replace it with swich case (class cast) after JDK 25 upgrade
            if (partitionByExpression instanceof final PartitionByColumn partitionByColumn) {
                partitionByParam = partitionByColumn.columnName();
            } else if (partitionByExpression instanceof final PartitionByFunction partitionByFunction) {
                partitionByParam = partitionByFunction.function().toString(Column::getColumnName);
            } else {
                throw ErrorCode.get(OlapErrorCodes.DB_OLAP_INVALID_PARTITION_BY_CONFIGURATION);
            }
            createBuffer.append(partitionByParam).append(", ");
        }
        StringUtil.replaceSuffix(createBuffer, NumericalConstant.TWO, ") ");

        final PKDefinition pkDefinition = tableDefinition.getPkDefinition();

        createBuffer.append(String.format("PRIMARY KEY (%s) ", pkDefinition.getColumnName()));
        createBuffer.append("ORDER BY (");

        final Collection<OrderByDefinition> orderByDefinitions = tableDefinition.getOrderByDefinitions();

        for (final OrderByDefinition orderByDefinition : orderByDefinitions) {
            createBuffer.append(orderByDefinition.columnName()).append(", ");
        }
        StringUtil.replaceSuffix(createBuffer, NumericalConstant.TWO, ") ");
        sqlStatements.add(createBuffer.toString());

        return sqlStatements.toArray(new String[0]);
    }

    /**
     * Retrieves the SQL statement to change the data type of a column in ClickHouse.
     *
     * @param tableDefinition the table definition containing the column
     * @param columnDefinition the column definition to modify
     * @return an array of SQL statements to change the column data type
     */
    @Override
    public String[] getChangeColumnDataTypeSQL(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition) {
        return new String[] {getChangeColumnDataTypeSQL(tableDefinition, columnDefinition, columnDefinition.isNullable())};
    }

    /**
     * ClickHouse (>= 22) requires the {@code COLUMN} keyword in {@code ALTER TABLE ... ADD COLUMN ...}.
     * The base implementation emits {@code ALTER TABLE ... ADD <name> <type>} which is accepted by
     * PostgreSQL/MySQL but rejected by modern ClickHouse with a syntax error.
     */
    @Override
    public String getAddColumnSQL(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition) {
        return "ALTER TABLE " + getTableName(tableDefinition) + " ADD COLUMN " + getColumnSQL(columnDefinition);
    }

    /**
     * Retrieves the SQL statement to make a column nullable in ClickHouse.
     *
     * @param tableDefinition the table definition containing the column
     * @param columnDefinition the column definition to modify
     * @return the SQL statement to make the column nullable
     */
    @Override
    public String getChangeColumnNullableSQL(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition) {
        return getChangeColumnDataTypeSQL(tableDefinition, columnDefinition, true);
    }

    /**
     * Retrieves the SQL statement to make a column not nullable in ClickHouse.
     *
     * @param tableDefinition the table definition containing the column
     * @param columnDefinition the column definition to modify
     * @return the SQL statement to make the column not nullable
     */
    @Override
    public String getChangeColumnNotNullableSQL(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition) {
        return getChangeColumnDataTypeSQL(tableDefinition, columnDefinition, false);
    }

    /**
     * Retrieves the SQL statements to increase the decimal weight of a column in ClickHouse.
     *
     * @param tableDefinition the table definition containing the column
     * @param columnDefinition the column definition to modify
     * @return an array of SQL statements to increase the decimal weight
     */
    @Override
    public String[] getIncreaseDecimalWeightSQL(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition) {
        return getChangeColumnDataTypeSQL(tableDefinition, columnDefinition);
    }

    /**
     * Generates the SQL statement to change the data type of a column with nullable option.
     *
     * @param tableDefinition the table definition containing the column
     * @param columnDefinition the column definition to modify
     * @param isNullable whether the column should be nullable
     * @return the SQL statement to change the column data type
     */
    public String getChangeColumnDataTypeSQL(final CHTableDefinition tableDefinition, final OlapColumnDefinition columnDefinition, final boolean isNullable) {
        return String.format("ALTER TABLE %s MODIFY COLUMN %s", getTableName(tableDefinition), getColumnSQL(columnDefinition, isNullable));
    }

    /**
     * Generates the SQL statement to create a skip index in ClickHouse.
     *
     * @param tableDefinition the table definition containing the index
     * @param skipIndexDefinition the skip index definition to create
     * @return the SQL statement to create the skip index
     */
    public String getCreateSkipIndexSql(final CHTableDefinition tableDefinition, final SkipIndexDefinition skipIndexDefinition) {
        return String.format("ALTER TABLE %s ADD INDEX %s %s TYPE %s", tableDefinition.getName(), skipIndexDefinition.getName(),
                skipIndexDefinition.getColumnName(), skipIndexDefinition.getSkipIndexExpression());
    }

    /**
     * Generates the SQL statements to update a skip index in ClickHouse.
     *
     * @param tableDefinition the table definition containing the index
     * @param skipIndexDefinition the skip index definition to update
     * @return an array of SQL statements to update the skip index
     */
    public String[] getUpdateSkipIndexSql(final CHTableDefinition tableDefinition, final SkipIndexDefinition skipIndexDefinition) {
        return new String[]{getDropSkipIndexSql(tableDefinition, skipIndexDefinition), getCreateSkipIndexSql(tableDefinition, skipIndexDefinition)};
    }

    /**
     * Generates the SQL statement to drop a skip index in ClickHouse.
     *
     * @param tableDefinition the table definition containing the index
     * @param skipIndexDefinition the skip index definition to drop
     * @return the SQL statement to drop the skip index
     */
    public String getDropSkipIndexSql(final CHTableDefinition tableDefinition, final SkipIndexDefinition skipIndexDefinition) {
        return String.format("ALTER TABLE %s DROP INDEX %s", tableDefinition.getName(), skipIndexDefinition.getName());
    }

    /**
     * Retrieves the table name with proper identifier quoting for ClickHouse.
     *
     * @param tableDefinition the table definition containing the table name
     * @return the quoted table name
     */
    @Override
    protected String getTableName(final TableDefinition<?> tableDefinition) {
        return DDLDMLUtil.getTableName(tableDefinition, CH_IDENTIFIER_QUOTE);
    }

    /**
     * Retrieves the column name with proper identifier quoting for ClickHouse.
     *
     * @param columnDefinition the column definition containing the column name
     * @return the quoted column name
     */
    @Override
    protected String getColumnName(final ColumnDefinition columnDefinition) {
        return DDLDMLUtil.getColumnName(columnDefinition, CH_IDENTIFIER_QUOTE);
    }

    /**
     * Retrieves the column name with proper identifier quoting for ClickHouse.
     *
     * @param column the column name as a string
     * @return the quoted column name
     */
    @Override
    protected String getColumnName(final String column) {
        return DDLDMLUtil.getColumnName(column, CH_IDENTIFIER_QUOTE);
    }

    /**
     * Retrieves the data type for a column in ClickHouse.
     *
     * @param columnDefinition the column definition containing the data type
     * @param maxLength the maximum length of the data type
     * @return the data type as a string
     */
    @Override
    protected String getDataType(final OlapColumnDefinition columnDefinition, final int maxLength) {
        return CHDdlDmlUtil.getDataType(columnDefinition);
    }

    /**
     * Generates the SQL for a column definition in ClickHouse.
     *
     * @param definition the column definition containing metadata
     * @return the SQL string for the column definition
     */
    @Override
    protected String getColumnSQL(final OlapColumnDefinition definition) {
        return getColumnSQL(definition, definition.isNullable());
    }

    /**
     * Generates the SQL for a column definition in ClickHouse with nullable option.
     *
     * @param definition the column definition containing metadata
     * @param isNullable whether the column should be nullable
     * @return the SQL string for the column definition
     */
    private String getColumnSQL(final OlapColumnDefinition definition, final boolean isNullable) {
        final StringBuilder builder = new StringBuilder();
        final String dataTypeStr = getDataType(definition, definition.getMaxLength());
        final DataType dataType = definition.getDataType();

        builder.append(getColumnName(definition));

        if (isNullable && CommonDataType.ARRAY != dataType && !(dataType instanceof CHDataType)) {
            builder.append(String.format(" Nullable(%s) ", dataTypeStr));
        } else {
            builder.append(" ").append(dataTypeStr).append(" ");
        }

        return builder.toString();
    }
}