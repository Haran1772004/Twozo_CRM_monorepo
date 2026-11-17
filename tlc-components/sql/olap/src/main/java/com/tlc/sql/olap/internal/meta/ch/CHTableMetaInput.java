package com.tlc.sql.olap.internal.meta.ch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.dml.Argument;
import com.tlc.sql.common.api.dml.function.SimpleArgument;
import com.tlc.sql.common.api.meta.TableEngine;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.olap.internal.meta.OlapTableMetaInput;
import com.tlc.sql.olap.internal.meta.engine.ch.AggregatingMergeTree;
import com.tlc.sql.olap.internal.meta.engine.ch.CHTableEngineConfiguration;
import com.tlc.sql.olap.internal.meta.engine.ch.DistributedEngine;
import com.tlc.sql.olap.internal.meta.engine.ch.MergeTree;
import com.tlc.sql.olap.internal.meta.engine.ch.ReplacingMergeTree;
import com.tlc.sql.olap.internal.meta.engine.ch.ReplicatedEngineProperties;
import com.tlc.sql.olap.internal.meta.engine.ch.ReplicatedReplacingMergeTree;
import com.tlc.sql.olap.internal.meta.engine.ch.SummingMergeTree;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * ClickHouse-specific table meta input that holds order-by, partition-by, skip-index
 * definitions, and builds the appropriate {@link TableEngine} instance based on the
 * engine name in the parsed configuration.
 *
 * @author sundar
 * @version 1.0
 */
@Getter
public class CHTableMetaInput extends OlapTableMetaInput<CHTableEngineConfiguration> {

    private final Collection<OrderByDefinition> orderByDefinitions;
    private final Collection<PartitionByDefinition> partitionByDefinitions;
    private final Collection<SkipIndexDefinition> skipIndexDefinitions;
    private final String cluster;

    /**
     * Creates a new ClickHouse table meta input with the given table name and engine configuration.
     *
     * @param tableName                  the logical table name
     * @param chTableEngineConfiguration the parsed engine configuration
     */
    public CHTableMetaInput(final String tableName, final CHTableEngineConfiguration chTableEngineConfiguration) {
        this(tableName, chTableEngineConfiguration, null);
    }

    public CHTableMetaInput(final String tableName, final CHTableEngineConfiguration chTableEngineConfiguration, final String cluster) {
        super(tableName, chTableEngineConfiguration);
        this.orderByDefinitions = new LinkedHashSet<>(NumericalConstant.FOUR);
        this.partitionByDefinitions = new LinkedHashSet<>(NumericalConstant.THREE);
        this.skipIndexDefinitions = new LinkedHashSet<>(NumericalConstant.THREE);
        this.cluster = cluster;
    }

    /**
     * Builds the concrete {@link TableEngine} instance for this table based on the engine
     * name in the configuration, resolving column definitions and scalar/function params.
     *
     * @return the constructed table engine
     * @throws ErrorCode if the engine name is unsupported or required params are missing
     */
    @Override
    public TableEngine buildTableEngine() {
        final String engineName = engineConfiguration.getName();

        return switch (engineName.toUpperCase()) {
            case "SUMMINGMERGETREE" -> new SummingMergeTree(getTableEngineColumnDefinitions(engineConfiguration));
            case "REPLACINGMERGETREE" -> {
                final List<ColumnDefinition> tableEngineColumnDefinitions = getTableEngineColumnDefinitions(engineConfiguration);
                final int size = tableEngineColumnDefinitions.size();

                if (NumericalConstant.ONE == size) {
                    yield new ReplacingMergeTree(tableEngineColumnDefinitions.get(NumericalConstant.ZERO));
                } else if (NumericalConstant.TWO == size) {
                    yield new ReplacingMergeTree(tableEngineColumnDefinitions.get(NumericalConstant.ZERO), tableEngineColumnDefinitions.get(NumericalConstant.ONE));
                } else {
                    throw ErrorCode.get(SQLErrorCodes.TABLE_ENGINE_INITIALIZATION_FAILED, "ReplacingMergeTree engine column definitions are incorrectly configured");
                }
            }
            case "REPLICATEDREPLACINGMERGETREE" -> {
                final List<ColumnDefinition> tableEngineColumnDefinitions = getTableEngineColumnDefinitions(engineConfiguration);
                final List<Argument> engineParams = engineConfiguration.getEngineParams();
                final List<String> scalarValues = resolveScalarParams(engineParams);
                final int colSize = tableEngineColumnDefinitions.size();
                final int scalarParamSize = scalarValues.size();
                final String zookeeperPath = scalarParamSize > NumericalConstant.ZERO ? scalarValues.get(NumericalConstant.ZERO) : ReplicatedEngineProperties.getZookeeperPath();
                final String replicaName = scalarParamSize > NumericalConstant.ONE ? scalarValues.get(NumericalConstant.ONE) : ReplicatedEngineProperties.getReplicaName();

                if (NumericalConstant.ONE == colSize) {
                    yield new ReplicatedReplacingMergeTree(tableEngineColumnDefinitions.get(NumericalConstant.ZERO), null, zookeeperPath, replicaName);
                } else if (NumericalConstant.TWO == colSize) {
                    yield new ReplicatedReplacingMergeTree(tableEngineColumnDefinitions.get(NumericalConstant.ZERO), tableEngineColumnDefinitions.get(NumericalConstant.ONE), zookeeperPath, replicaName);
                } else {
                    throw ErrorCode.get(SQLErrorCodes.TABLE_ENGINE_INITIALIZATION_FAILED, "ReplicatedReplacingMergeTree engine column definitions are incorrectly configured");
                }
            }
            case "DISTRIBUTED" -> {
                final List<Argument> engineParams = engineConfiguration.getEngineParams();
                final List<String> scalarValues = resolveScalarParams(engineParams);
                final Argument functionParam = resolveFunctionParam(engineParams);

                if (scalarValues.size() < NumericalConstant.THREE || functionParam == null) {
                    throw ErrorCode.get(SQLErrorCodes.TABLE_ENGINE_INITIALIZATION_FAILED, "DistributedEngine requires cluster, database, table and shardingKey params");
                }
                yield new DistributedEngine(scalarValues.get(NumericalConstant.ZERO), scalarValues.get(NumericalConstant.ONE),
                        scalarValues.get(NumericalConstant.TWO), functionParam);
            }
            case "AGGREGATINGMERGETREE" -> new AggregatingMergeTree(columnDefinitions.values());
            case "MERGETREE" -> new MergeTree();
            default -> throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_TABLE_ENGINE);
        };
    }

    /**
     * Adds an ORDER BY definition to this table's ordered collection.
     *
     * @param orderByDefinition the order-by definition to add
     */
    public void addOrderByDefinition(final OrderByDefinition orderByDefinition) {
        orderByDefinitions.add(orderByDefinition);
    }

    /**
     * Adds a PARTITION BY definition to this table's ordered collection.
     *
     * @param partitionByDefinition the partition-by definition to add
     */
    public void addPartitionByDefinition(final PartitionByDefinition partitionByDefinition) {
        partitionByDefinitions.add(partitionByDefinition);
    }

    /**
     * Adds a skip index definition to this table's collection.
     *
     * @param skipIndexDefinition the skip index definition to add
     */
    public void addSkipIndexDefinitions(final SkipIndexDefinition skipIndexDefinition) {
        skipIndexDefinitions.add(skipIndexDefinition);
    }

    /**
     * Resolves engine configuration column names into their corresponding {@link ColumnDefinition} instances.
     *
     * @param chTableEngineConfiguration the engine configuration containing column names
     * @return an ordered list of resolved column definitions
     * @throws ErrorCode if any column name does not exist in this table's column definitions
     */
    private List<ColumnDefinition> getTableEngineColumnDefinitions(final CHTableEngineConfiguration chTableEngineConfiguration) {
        final Collection<String> columns = chTableEngineConfiguration.getColumns();
        final List<ColumnDefinition> engineColumnDefinitions = new ArrayList<>(columns.size());

        for (final String columnName : columns) {

            if (columnDefinitions.containsKey(columnName)) {
                engineColumnDefinitions.add(columnDefinitions.get(columnName));
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
            }
        }
        return engineColumnDefinitions;
    }

    /**
     * Extracts scalar (non-column, non-function) parameter values from the unified engine param list.
     *
     * @param engineParams the ordered list of engine parameters
     * @return a list of scalar string values in their original declaration order
     */
    private List<String> resolveScalarParams(final List<Argument> engineParams) {
        final List<String> scalarValues = new ArrayList<>();

        for (final Argument param : engineParams) {

            if (param instanceof final SimpleArgument simpleArgument && !simpleArgument.isColumn()) {
                final String value = simpleArgument.getValue().toString();

                scalarValues.add(value.startsWith("'") && value.endsWith("'") ? value.substring(1, value.length() - 1) : value);
            }
        }
        return scalarValues;
    }

    /**
     * Finds the first function-type parameter from the unified engine param list.
     *
     * @param engineParams the ordered list of engine parameters
     * @return the first non-{@link SimpleArgument} param (function call), or null if none found
     */
    private Argument resolveFunctionParam(final List<Argument> engineParams) {

        for (final Argument param : engineParams) {

            if (!(param instanceof SimpleArgument)) {
                return param;
            }
        }
        return null;
    }

    public String getCluster() {
        return cluster;
    }
}