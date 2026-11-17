package com.tlc.sql.olap.internal.parser.ch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.dml.Argument;
import com.tlc.sql.common.api.dml.function.SimpleArgument;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.dml.function.StringValue;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.FKDefinition;
import com.tlc.sql.common.api.meta.TableType;
import com.tlc.sql.common.api.parser.MetaDataParser;
import com.tlc.sql.common.api.parser.MetaTagConstants;
import com.tlc.sql.common.api.parser.TableDefinitionLoader;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.olap.api.dml.function.OlapSqlFunctionBuilderFactory;
import com.tlc.sql.olap.api.meta.OlapArrayColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapPKDefinition;
import com.tlc.sql.olap.internal.meta.ch.AggregateFunctionColumnDefinition;
import com.tlc.sql.olap.internal.meta.ch.CHDataType;
import com.tlc.sql.olap.internal.meta.ch.CHTableDefinition;
import com.tlc.sql.olap.internal.meta.ch.CHTableMetaInput;
import com.tlc.sql.olap.internal.meta.ch.MinmaxIndex;
import com.tlc.sql.olap.internal.meta.ch.NGramBloomFilter;
import com.tlc.sql.olap.internal.meta.ch.OrderByDefinition;
import com.tlc.sql.olap.internal.meta.ch.PartitionByColumn;
import com.tlc.sql.olap.internal.meta.ch.PartitionByDefinition;
import com.tlc.sql.olap.internal.meta.ch.PartitionByFunction;
import com.tlc.sql.olap.internal.meta.ch.ProbabilisticBloomFilter;
import com.tlc.sql.olap.internal.meta.ch.SetIndex;
import com.tlc.sql.olap.internal.meta.ch.SkipIndexDefinition;
import com.tlc.sql.olap.internal.meta.ch.TokenBloomFilter;
import com.tlc.sql.olap.internal.meta.engine.ch.CHTableEngineConfiguration;
import com.tlc.sql.olap.internal.meta.engine.ch.DistributedEngine;
import com.tlc.sql.olap.internal.parser.OlapMetaDataParser;
import com.tlc.sql.olap.internal.status.OlapErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * ClickHouse-specific metadata parser that reads meta.xml table definitions and builds
 * {@link CHTableMetaInput} instances with engine configuration, columns, order-by,
 * partition-by, and skip-index definitions.
 *
 * @author sundar
 * @version 1.0
 */
public class CHMetaDataParser extends OlapMetaDataParser<CHTableEngineConfiguration, CHTableMetaInput, CHTableDefinition> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CHMetaDataParser.class);
    private static final String DIST_TABLE_SUFFIX = "_dist";

    private static final class Instance {
        private static final MetaDataParser<CHTableEngineConfiguration, OlapColumnDefinition, CHTableDefinition, CHTableMetaInput> META_DATA_PARSER = new CHMetaDataParser();
    }

    /**
     * Returns the singleton instance of this parser.
     *
     * @return the shared {@link CHMetaDataParser} instance
     */
    public static MetaDataParser<CHTableEngineConfiguration, OlapColumnDefinition, CHTableDefinition, CHTableMetaInput> getInstance() {
        return Instance.META_DATA_PARSER;
    }

    @Override
    protected CHTableDefinition buildTableDefinition(final String name, final TableType type, final int seqId, final CHTableMetaInput metaInput) {
        return new CHTableDefinition(name, type, seqId, metaInput);
    }

    /**
     * Parses the engine-configuration block from the table element and builds a
     * {@link CHTableMetaInput}. For each {@code <param>} element, the attributes
     * {@code is-column} and {@code is-function} are checked universally to determine
     * param type. Distributed engine table params are validated for the {@code _dist} suffix.
     *
     * @param tableName    the logical table name
     * @param tableElement the DOM element representing the {@code <table>} tag
     * @return a fully populated {@link CHTableMetaInput}
     * @throws ErrorCode if a Distributed engine table name does not end with {@code _dist}
     */
    @Override
    protected CHTableMetaInput buildTableMetaInput(final String tableName, final Element tableElement) {
        final NodeList engineConfiguration = tableElement.getElementsByTagName(MetaTagConstants.ENGINE_CONFIGURATION);
        final CHTableEngineConfiguration tableEngineConfiguration;
        final String engineName = getAttribute(tableElement, MetaTagConstants.ENGINE);

        if (DistributedEngine.NAME.equalsIgnoreCase(engineName) && !tableName.endsWith(DIST_TABLE_SUFFIX)) {
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_TABLE_NAME,
                    "DistributedEngine table name '" + tableName + "' must end with '" + DIST_TABLE_SUFFIX + "'");
        }

        if (NumericalConstant.ONE == engineConfiguration.getLength()) {
            final Element engineConfigurationElement = (Element) engineConfiguration.item(NumericalConstant.ZERO);
            final NodeList paramList = engineConfigurationElement.getElementsByTagName(MetaTagConstants.PARAM);
            final int paramListLength = paramList.getLength();

            if (NumericalConstant.ZERO < paramListLength) {
                final CHTableEngineConfiguration chTableEngineConfiguration = new CHTableEngineConfiguration(engineName);

                processEngineConfigurationParams(engineConfigurationElement, chTableEngineConfiguration);
                tableEngineConfiguration = chTableEngineConfiguration;
            } else {
                throw ErrorCode.get(SQLErrorCodes.INVALID_TABLE_ENGINE_CONFIGURATION);
            }
        } else {
            tableEngineConfiguration = new CHTableEngineConfiguration(engineName);
        }
        return new CHTableMetaInput(tableName, tableEngineConfiguration);
    }

    @Override
    protected void loadForeignKeyDefinition(final String fkName, final String localTableName, final CHTableDefinition refTableDef,
                                            final ColumnDefinition localColumnDef, final ColumnDefinition refColumnDef,
                                            final FKDefinition.FKConstraint fkConstraint, final CHTableMetaInput metaInput,
                                            final Set<String> nameContainer) {
        final FKDefinition fkDefinition = new FKDefinition(fkName, localTableName, refTableDef, localColumnDef, refColumnDef, fkConstraint);

        metaInput.addFKDefinition(fkDefinition);
    }

    @Override
    protected void loadAdditionalDefinitions(final Element tableElement, final TableDefinitionLoader<OlapColumnDefinition, CHTableDefinition> definitionProvider,
                                             final CHTableMetaInput metaInput, final Set<String> nameContainer, final Pattern namePattern) {
        final NodeList orderByNodeList = tableElement.getElementsByTagName(MetaTagConstants.ORDER_BY);

        if (orderByNodeList.getLength() == 1) {
            final Element orderByElement = (Element) orderByNodeList.item(NumericalConstant.ZERO);

            loadOrderByDefinitions(orderByElement.getElementsByTagName(MetaTagConstants.ORDER_BY_COLUMN), metaInput);
        } else {
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_ORDER_BY_REQUIRED, "ORDER BY definition is required for Olap tables");
        }

        final NodeList partitionByNodeList = tableElement.getElementsByTagName(MetaTagConstants.PARTITION_BY);

        if (partitionByNodeList.getLength() == 1) {
            final Element partitionByElement = (Element) partitionByNodeList.item(NumericalConstant.ZERO);

            loadPartitionByDefinitions(partitionByElement.getElementsByTagName(MetaTagConstants.PARTITION_BY_ELEMENT), metaInput.getTableName(), metaInput);
        } else {
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_PARTITION_BY_REQUIRED, "PARTITION BY definition is required for Olap tables");
        }

        final NodeList indexesNodeList = tableElement.getElementsByTagName(MetaTagConstants.INDEXES);
        final int indexesLength = indexesNodeList.getLength();

        if (indexesLength == 1) {
            final Element indexesElement = (Element) indexesNodeList.item(NumericalConstant.ZERO);

            loadIndexAsSkipIndexDefinitions(indexesElement.getElementsByTagName(MetaTagConstants.INDEX), metaInput);
        } else if (indexesLength > 1) {
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_INVALID_SKIP_INDEXES_CONFIGURATION, "Found more than one indexes configuration");
        }
    }

    /**
     * Parses the unified {@code <indexes>} format and converts each {@code <index>} element
     * into a {@link SkipIndexDefinition} for OLAP. Supports optional OLAP-specific attributes
     * (type, false-positive-probability, granularity) within the standard index format.
     *
     * @param indexNodeList the list of {@code <index>} elements
     * @param metaInput     the table meta input to populate
     */
    private void loadIndexAsSkipIndexDefinitions(final NodeList indexNodeList, final CHTableMetaInput metaInput) {
        final int length = indexNodeList.getLength();

        if (length == NumericalConstant.ZERO) {
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_INVALID_SKIP_INDEXES_CONFIGURATION, "Indexes should not be empty within the indexes tag");
        }

        if (length > NumericalConstant.THREE) {
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_LIMIT_EXCEED, "Index column limit exceeded");
        }

        for (int i = 0; i < length; i++) {
            final Element indexElement = (Element) indexNodeList.item(i);
            final String name = getMandatoryAttribute(indexElement, MetaTagConstants.NAME,
                    ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED, "Index name is required"));

            final NodeList indexColumnNodes = indexElement.getElementsByTagName(MetaTagConstants.INDEX_COLUMN);

            if (indexColumnNodes.getLength() == NumericalConstant.ZERO) {
                throw ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED, "Index column is required");
            }

            final String columnName = indexColumnNodes.item(NumericalConstant.ZERO).getTextContent().trim();

            if (!metaInput.isColumnExists(columnName)) {
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_COLUMN, "Specified index column does not exist: " + columnName);
            }

            final String type = getAttribute(indexElement, MetaTagConstants.TYPE);
            final String falsePositiveProbability = getAttribute(indexElement, MetaTagConstants.FALSE_POSITIVE_PROBABILITY);
            final String granularity = getAttribute(indexElement, MetaTagConstants.GRANULARITY);

            final SkipIndexDefinition skipIndexDef;

            if (Objects.isNull(type) || type.equalsIgnoreCase("PROBABILISTICBLOOMFILTER")) {
                final float probability = Objects.nonNull(falsePositiveProbability)
                        ? Float.parseFloat(falsePositiveProbability) : 0.01f;

                skipIndexDef = Objects.isNull(granularity)
                        ? new ProbabilisticBloomFilter(name, columnName, probability)
                        : new ProbabilisticBloomFilter(name, columnName, Short.parseShort(granularity), probability);
            } else {
                skipIndexDef = buildSkipIndexDefinition(indexElement, columnName);
            }

            metaInput.addSkipIndexDefinitions(skipIndexDef);
        }
    }

    private SkipIndexDefinition buildSkipIndexDefinition(final Element skipIndexElement, final String columnName) {
        final String type = getMandatoryAttribute(skipIndexElement, MetaTagConstants.TYPE, ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED, "Skip index type is required"));
        final String name = getMandatoryAttribute(skipIndexElement, MetaTagConstants.NAME, ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED, "Skip index name is required"));
        final String granularity = getAttribute(skipIndexElement, MetaTagConstants.GRANULARITY);

        if (!name.startsWith("skip_idx_")) {
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_INVALID_SKIP_INDEX_NAME_PATTERN);
        }
        return switch (type.toUpperCase()) {

            case "MINMAX" -> Objects.isNull(granularity) ? new MinmaxIndex(name, columnName)
                    : new MinmaxIndex(name, Short.parseShort(granularity), columnName);
            case "SET" -> {
                final int maxDistinctValue = Integer.parseInt(getMandatoryAttribute(skipIndexElement, MetaTagConstants.MAX_DISTINCT_VALUES,
                        ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED, "Maximum distinct value is required for `Set` index")));

                yield Objects.isNull(granularity) ? new SetIndex(name, columnName, maxDistinctValue)
                        : new SetIndex(name, columnName, maxDistinctValue, Short.parseShort(granularity));
            }
            case "PROBABILISTICBLOOMFILTER" -> {
                final String falsePositiveProbabilityString = getAttribute(skipIndexElement, MetaTagConstants.FALSE_POSITIVE_PROBABILITY);
                final boolean granularityIsNull = Objects.isNull(granularity);

                if (Objects.isNull(falsePositiveProbabilityString)) {
                    yield granularityIsNull ? new ProbabilisticBloomFilter(name, columnName)
                            : new ProbabilisticBloomFilter(name, columnName, Short.parseShort(granularity));
                } else {
                    final float falsePositiveProbability = Float.parseFloat(falsePositiveProbabilityString);

                    if (NumericalConstant.ZERO < falsePositiveProbability && falsePositiveProbability < NumericalConstant.ONE) {
                        yield granularityIsNull ? new ProbabilisticBloomFilter(name, columnName, falsePositiveProbability)
                                : new ProbabilisticBloomFilter(name, columnName, Short.parseShort(granularity), falsePositiveProbability);
                    } else {
                        throw ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_INVALID_FALSE_POSITIVE_PROBABILITY,
                                "False positive probability should be greater than 0 and less than 1 for Probabilistic bloom filter");
                    }
                }
            }
            case "TOKENBLOOMFILTER" -> buildTextBloomFilter(name, columnName, granularity, skipIndexElement, true);
            case "NGRAMBLOOMFILTER" -> buildTextBloomFilter(name, columnName, granularity, skipIndexElement, false);
            default -> throw ErrorCode.get(OlapErrorCodes.DB_OLAP_INVALID_SKIP_INDEX);
        };
    }

    private SkipIndexDefinition buildTextBloomFilter(final String name, final String columnName, final String granularity,
                                                     final Element skipIndexElement, final boolean isTokenBloomFilter) {
        final int size = Integer.parseInt(getMandatoryAttribute(skipIndexElement, MetaTagConstants.SIZE,
                ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED, "Size attribute is required for bloom filter")));
        final int hashCount = Integer.parseInt(getMandatoryAttribute(skipIndexElement, MetaTagConstants.HASH_COUNT,
                ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED, "Hash count attribute is required for bloom filter")));
        final int seed = Integer.parseInt(getMandatoryAttribute(skipIndexElement, MetaTagConstants.SEED,
                ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED, "Seed attribute is required for bloom filter")));
        final boolean granularityIsNull = Objects.isNull(granularity);

        if (isTokenBloomFilter) {
            return granularityIsNull ? new TokenBloomFilter(name, columnName, size, hashCount, seed)
                    : new TokenBloomFilter(name, columnName, Short.parseShort(granularity), size, hashCount, seed);
        } else {
            final int ngramLength = Integer.parseInt(getMandatoryAttribute(skipIndexElement, MetaTagConstants.LENGTH,
                    ErrorCode.get(OlapErrorCodes.DB_OLAP_SKIP_INDEX_ATTRIBUTE_REQUIRED, "Length attribute is required for ngram bloom filter")));

            return granularityIsNull ? new NGramBloomFilter(name, columnName, ngramLength, size, hashCount, seed)
                    : new NGramBloomFilter(name, columnName, Short.parseShort(granularity), ngramLength, size, hashCount, seed);
        }
    }

    private String getMandatoryAttribute(final Element element, final String attributeName, final ErrorCode errorCode) {
        final String attribute = getAttribute(element, attributeName);

        if (Objects.isNull(attribute)) {
            throw errorCode;
        }
        return attribute;
    }

    private void loadPartitionByDefinitions(final NodeList partitionByNodeList, final String tableName, final CHTableMetaInput metaInput) {
        final int length = partitionByNodeList.getLength();

        if (NumericalConstant.ZERO < length) {

            if (length > NumericalConstant.THREE) {
                throw ErrorCode.get(OlapErrorCodes.DB_OLAP_PARTITION_BY_LIMIT_EXCEED, "Partition by column limit exceeded");
            }

            for (int i = 0; i < length; i++) {
                final Element partitionByElement = (Element) partitionByNodeList.item(i);
                final String isFunctionAttribute = getAttribute(partitionByElement, MetaTagConstants.IS_FUNCTION);

                if (Objects.isNull(isFunctionAttribute) || !Boolean.parseBoolean(isFunctionAttribute)) {
                    final String columnName = partitionByElement.getTextContent();

                    if (metaInput.isColumnExists(columnName)) {
                        metaInput.addPartitionByDefinition(new PartitionByDefinition(new PartitionByColumn(columnName)));
                    } else {
                        throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
                    }
                } else {
                    loadPartitionByDefinition(partitionByElement, tableName, metaInput);
                }
            }
        } else {
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_PARTITION_BY_REQUIRED, "PARTITION BY definition is required for Olap tables");
        }
    }

    private void loadPartitionByDefinition(final Element partitionByElement, final String tableName, final CHTableMetaInput metaInput) {
        final Element functionElement = (Element) partitionByElement.getElementsByTagName(MetaTagConstants.FUNCTION).item(NumericalConstant.ZERO);
        final String functionName = getMandatoryAttribute(functionElement, MetaTagConstants.NAME,
                ErrorCode.get(OlapErrorCodes.DB_OLAP_PARTITION_BY_ATTRIBUTE_REQUIRED, "Partition by element: Function name required"));

        OlapSqlFunctionBuilderFactory.getBuilder(functionName).ifPresentOrElse(functionBuilder -> {
            processPartitionByFunctionParams(functionElement, metaInput, functionBuilder);
            metaInput.addPartitionByDefinition(new PartitionByDefinition(new PartitionByFunction(functionBuilder.build())));
        }, () -> {
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_FUNCTION_NOT_FOUND);
        });
    }

    /**
     * Parses {@code <param>} children of a partition-by {@code <function>} element using
     * the same three-way detection used by engine-configuration functions:
     * <ul>
     *     <li>{@code is-function="true"} – recursively parsed as a nested function (unquoted)</li>
     *     <li>{@code is-column="true"}   – emitted as a bare column identifier (unquoted)</li>
     *     <li>otherwise                  – emitted as a quoted string literal</li>
     * </ul>
     */
    private void processPartitionByFunctionParams(final Element functionElement, final CHTableMetaInput metaInput,
                                                  final SqlFunction.Builder functionBuilder) {
        final NodeList paramList = functionElement.getElementsByTagName(MetaTagConstants.PARAM);
        final int paramListLength = paramList.getLength();

        if (Objects.equals(NumericalConstant.ZERO, paramListLength)) {
            throw ErrorCode.get(SQLErrorCodes.INVALID_TABLE_ENGINE_CONFIGURATION);
        }

        for (int paramIndex = 0; paramIndex < paramListLength; paramIndex++) {
            final Element paramElement = (Element) paramList.item(paramIndex);
            final String isFunctionAttribute = getAttribute(paramElement, MetaTagConstants.IS_FUNCTION);
            final String isColumnAttribute = getAttribute(paramElement, MetaTagConstants.IS_COLUMN);

            if (Objects.nonNull(isFunctionAttribute) && Boolean.parseBoolean(isFunctionAttribute)) {
                functionBuilder.addArgument(parseFunctionParam(paramElement));
            } else if (Objects.nonNull(isColumnAttribute) && Boolean.parseBoolean(isColumnAttribute)) {
                final String paramContent = paramElement.getTextContent().trim();

                if (!metaInput.isColumnExists(paramContent)) {
                    throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_COLUMN);
                }
                functionBuilder.addArgument(columnResolver -> paramContent);
            } else {
                final String paramContent = paramElement.getTextContent().trim();

                functionBuilder.addArgument(SimpleArgument.of(new StringValue(paramContent)));
            }
        }
    }

    @Override
    protected DataType getDataType(final String dataTypeStr) {

        try {
            return super.getDataType(dataTypeStr);
        } catch (ErrorCode errorCode) {
            final ErrorCode notSupported = ErrorCode.get(ErrorCodes.NOT_SUPPORTED);

            if (errorCode.equals(notSupported)) {
                return CHDataType.get(dataTypeStr);
            } else {
                throw notSupported;
            }
        }
    }

    @Override
    protected OlapColumnDefinition buildColumnDefinition(final Element columnElement, final String columnName, final DataType dataType,
                                                         final int convertedMaxLength, final Object convertedDefaultValue, final boolean isNullable) {

        if (CHDataType.AGGREGATE_FUNCTION == dataType) {
            final String aggregateFunctionName = getAttribute(columnElement, MetaTagConstants.AGG_FUNCTION);

            if (Objects.isNull(aggregateFunctionName)) {
                throw ErrorCode.get(OlapErrorCodes.DB_OLAP_AGGREGATE_FUNCTION_REQUIRED);
            }
            final String paramTypes = getAttribute(columnElement, MetaTagConstants.PARAM_TYPES);

            if (Objects.isNull(paramTypes)) {
                throw ErrorCode.get(OlapErrorCodes.DB_OLAP_PARAMETER_TYPES_REQUIRED);
            }
            final String[] segregatedParamTypes = paramTypes.split(",");
            final List<DataType> paramDataTypes = new ArrayList<>(segregatedParamTypes.length);

            for (final String dataTypeString : segregatedParamTypes) {
                paramDataTypes.add(getDataType(dataTypeString));
            }
            return new AggregateFunctionColumnDefinition(columnName, dataType, aggregateFunctionName, paramDataTypes);
        } else if (CommonDataType.ARRAY == dataType) {
            final String elementType = getAttribute(columnElement, MetaTagConstants.ELEMENT_TYPE);
            final DataType elementDataType = getDataType(elementType);

            return new OlapArrayColumnDefinition(columnName, elementDataType, convertedMaxLength, convertedDefaultValue, isNullable);
        }
        return super.buildColumnDefinition(columnElement, columnName, dataType, convertedMaxLength, convertedDefaultValue, isNullable);
    }

    @Override
    protected void loadPrimaryKeyDefinition(final Element tableElement, final String tableName, final Set<String> nameContainer,
                                            final CHTableMetaInput metaInput, final Pattern namePattern) {
        final NodeList primaryKey = tableElement.getElementsByTagName(MetaTagConstants.PRIMARY_KEY);

        if (primaryKey.getLength() == 0) {
            LOGGER.error("Primary key tag notFound in table : {}", tableName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_PK_NAME);
        }

        if (primaryKey.getLength() > 1) {
            LOGGER.error("Multiple primary-key tag found inside table : {}", metaInput.getTableName());
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_PK_LIMIT_EXCEED);
        }
        final Element primaryKeyElement = (Element) primaryKey.item(0);
        final String column = getAttribute(primaryKeyElement, MetaTagConstants.COLUMN);

        if (column == null) {
            LOGGER.error("name and column cannot be null in Primary key, refer table {}", metaInput.getTableName());
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
        }
        final ColumnDefinition columnDef = metaInput.getColumnDefinition(column);

        if (columnDef == null) {
            LOGGER.error("Unknown column found inside PrimaryKey : {}", column);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_COLUMN);
        }
        final DataType colDataType = columnDef.getDataType();

        if (!colDataType.isNumeric()) {
            LOGGER.error("Unsupported PK datatype provided, current datatype : {}", colDataType.getDataTypeStr());
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_PK_COLUMN);
        }
        metaInput.setPkDefinition(new OlapPKDefinition(column));
    }

    private void loadOrderByDefinitions(final NodeList orderByColumns, final CHTableMetaInput metaInput) {
        final int length = orderByColumns.getLength();

        if (length > NumericalConstant.THREE) {
            throw ErrorCode.get(OlapErrorCodes.DB_OLAP_ORDER_BY_LIMIT_EXCEED, "Order by column limit exceeded");
        }

        for (int i = 0; i < length; i++) {
            final Element orderByColumn = (Element) orderByColumns.item(i);
            final String columnName = orderByColumn.getTextContent();

            if (metaInput.isColumnExists(columnName)) {
                metaInput.addOrderByDefinition(new OrderByDefinition(columnName));
            } else {
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
            }
        }
    }


    /**
     * Parses all direct-child {@code <param>} elements of an {@code <engine-configuration>}
     * block using the universal {@code is-function} / {@code is-column} attribute pattern.
     * Each param is converted to the appropriate {@link Argument} subtype and appended
     * to the configuration's unified ordered param list. For legacy engines, column
     * references are also added to the column collection.
     *
     * @param engineConfigurationElement the DOM element for the engine-configuration block
     * @param configuration              the configuration to populate
     */
    private void processEngineConfigurationParams(final Element engineConfigurationElement,
                                                  final CHTableEngineConfiguration configuration) {
        final NodeList childNodes = engineConfigurationElement.getChildNodes();
        final int childCount = childNodes.getLength();

        for (int i = 0; i < childCount; i++) {

            if (childNodes.item(i) instanceof final Element paramElement
                    && MetaTagConstants.PARAM.equals(paramElement.getTagName())) {
                final String isFunctionAttribute = getAttribute(paramElement, MetaTagConstants.IS_FUNCTION);
                final String isColumnAttribute = getAttribute(paramElement, MetaTagConstants.IS_COLUMN);

                if (Objects.nonNull(isFunctionAttribute) && Boolean.parseBoolean(isFunctionAttribute)) {
                    final Argument functionArgument = parseFunctionParam(paramElement);

                    configuration.addEngineParam(functionArgument);
                } else if (Objects.nonNull(isColumnAttribute) && Boolean.parseBoolean(isColumnAttribute)) {
                    final String paramContent = paramElement.getTextContent().trim();

                    configuration.addColumnDefinition(paramContent);
                    configuration.addEngineParam(columnResolver -> paramContent);
                } else {
                    final String paramContent = paramElement.getTextContent().trim();

                    configuration.addEngineParam(SimpleArgument.of(new StringValue(paramContent)));
                }
            }
        }
    }

    /**
     * Parses a {@code <function>} tag nested inside a {@code <param>} element and returns
     * a function-call {@link Argument} that renders verbatim and unquoted in DDL.
     * Each nested {@code <param>} inside the function's argument list is resolved using
     * the same three-way type detection (is-function, is-column, scalar) as top-level
     * engine configuration params, enabling fully recursive function nesting.
     *
     * @param paramElement the {@code <param>} element containing the {@code <function>} child
     * @return an {@link Argument} wrapping the fully built {@link SqlFunction}
     */
    private Argument parseFunctionParam(final Element paramElement) {
        final Element functionElement = (Element) paramElement.getElementsByTagName(MetaTagConstants.FUNCTION).item(NumericalConstant.ZERO);
        final String functionName = getMandatoryAttribute(functionElement, MetaTagConstants.NAME,
                ErrorCode.get(SQLErrorCodes.INVALID_TABLE_ENGINE_CONFIGURATION, "Engine configuration: Function name required"));

        final SqlFunction.Builder functionBuilder = OlapSqlFunctionBuilderFactory.getBuilder(functionName)
                .orElseThrow(() -> ErrorCode.get(OlapErrorCodes.DB_OLAP_FUNCTION_NOT_FOUND));

        final NodeList argParamList = functionElement.getElementsByTagName(MetaTagConstants.PARAM);
        final int argCount = argParamList.getLength();

        for (int argIndex = 0; argIndex < argCount; argIndex++) {
            final Element argElement = (Element) argParamList.item(argIndex);
            final String isFunctionAttribute = getAttribute(argElement, MetaTagConstants.IS_FUNCTION);
            final String isColumnAttribute = getAttribute(argElement, MetaTagConstants.IS_COLUMN);

            if (Objects.nonNull(isFunctionAttribute) && Boolean.parseBoolean(isFunctionAttribute)) {
                final Argument nestedFunctionArgument = parseFunctionParam(argElement);

                functionBuilder.addArgument(nestedFunctionArgument);
            } else if (Objects.nonNull(isColumnAttribute) && Boolean.parseBoolean(isColumnAttribute)) {
                final String argContent = argElement.getTextContent().trim();

                functionBuilder.addArgument(columnResolver -> argContent);
            } else {
                final String argContent = argElement.getTextContent().trim();

                functionBuilder.addArgument(SimpleArgument.of(new StringValue(argContent)));
            }
        }
        final SqlFunction function = functionBuilder.build();

        return function::toString;
    }

}