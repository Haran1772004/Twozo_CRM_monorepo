package com.tlc.sql.oltp.internal.parser;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.FKDefinition;
import com.tlc.sql.common.api.meta.IndexDefinition;
import com.tlc.sql.common.api.meta.SequenceDefinition;
import com.tlc.sql.common.api.meta.TableType;
import com.tlc.sql.common.api.meta.UKDefinition;
import com.tlc.sql.common.api.parser.MetaDataParser;
import com.tlc.sql.common.api.parser.MetaTagConstants;
import com.tlc.sql.common.api.parser.TableDefinitionLoader;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.oltp.api.meta.OltpArrayColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpDataType;
import com.tlc.sql.oltp.api.meta.OltpDecimalColumnDefinition;
import com.tlc.sql.oltp.api.meta.OltpPKDefinition;
import com.tlc.sql.oltp.api.meta.OltpTableDefinition;
import com.tlc.sql.oltp.internal.meta.OltpTableMetaInput;
import com.tlc.sql.oltp.internal.meta.engine.OltpTableEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class OltpMetaDataParser extends MetaDataParser<OltpTableEngineConfiguration, OltpColumnDefinition, OltpTableDefinition, OltpTableMetaInput> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OltpMetaDataParser.class);
    private static final String DEFAULT_TABLE_ENGINE_NAME = "DEFAULT";

    private static final class Instance {
        private static final MetaDataParser<OltpTableEngineConfiguration, OltpColumnDefinition, OltpTableDefinition, OltpTableMetaInput> META_DATA_PARSER = new OltpMetaDataParser();
    }

    public static MetaDataParser<OltpTableEngineConfiguration, OltpColumnDefinition, OltpTableDefinition, OltpTableMetaInput> getInstance() {
        return Instance.META_DATA_PARSER;
    }

    @Override
    protected OltpTableDefinition buildTableDefinition(final String name, final TableType type, final int seqId, final OltpTableMetaInput metaInput) {
        return new OltpTableDefinition(name, type, seqId, metaInput);
    }

    protected void loadPrimaryKeyDefinition(final Element tableElement, final String tableName, final Set<String> nameContainer,
                                            final OltpTableMetaInput metaInput, final Pattern namePattern) {

        final NodeList primaryKey = tableElement.getElementsByTagName(MetaTagConstants.PRIMARY_KEY);

        if (primaryKey.getLength() == 0) {
            LOGGER.error("Primary key tag notFound in table : {}", tableName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_PK_NAME);
        }

        if (primaryKey.getLength() > 1) {
            LOGGER.error("Multiple primary-key tag found inside table : {}", metaInput.getTableName());
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_PK_LIST);
        }
        final Element primaryKeyElement = (Element) primaryKey.item(0);
        final String name = getAttribute(primaryKeyElement, MetaTagConstants.NAME);
        final String column = getAttribute(primaryKeyElement, MetaTagConstants.COLUMN);

        if (name == null || column == null) {
            LOGGER.error("name and column cannot be null in Primary key, refer table {}", metaInput.getTableName());
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
        }
        if (!namePattern.matcher(name).matches()) {
            LOGGER.error("Invalid PKName exists,Expected format [A-Za-z0-9_], current {}", name);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_PK_NAME);
        }
        if (!nameContainer.add(name)) {
            LOGGER.error("Already Key with the same name exists, Name : {}", name);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_DUPLICATE_KEY_NAME);
        }

        final ColumnDefinition columnDef = metaInput.getColumnDefinition(column);
        if (columnDef == null) {
            LOGGER.error("Unknown column found inside PrimaryKey : {}", column);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_COLUMN);
        }
        final DataType colDataType = columnDef.getDataType();

        if (!colDataType.isBigint()) {
            LOGGER.error("Unsupported PK datatype provided, current datatype : {}", colDataType.getDataTypeStr());
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_PK_COLUMN);
        }
        final String seqBatch = getAttribute(primaryKeyElement, MetaTagConstants.SEQUENCE_BATCH);
        final String seqGenerator = getAttribute(primaryKeyElement, MetaTagConstants.SEQUENCE_GENERATOR);

        final int sequenceBatch = seqBatch == null ? 50 : Integer.parseInt(seqBatch);
        if (sequenceBatch < 50) {
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_SEQUENCE_CONFIG, "value is < 50");
        }
        if (seqGenerator == null) {
            metaInput.setPkDefinition(new OltpPKDefinition(name, column));
        } else {
            metaInput.setPkDefinition(new OltpPKDefinition(name, column, new SequenceDefinition(seqGenerator, sequenceBatch)));
        }
    }

    @Override
    protected void loadAdditionalDefinitions(final Element tableElement, final TableDefinitionLoader<OltpColumnDefinition, OltpTableDefinition> definitionProvider,
                                             final OltpTableMetaInput metaInput, final Set<String> nameContainer, final Pattern namePattern) {
        if (definitionProvider.loadKeys()) {
            final NodeList uniqueKeys = tableElement.getElementsByTagName(MetaTagConstants.UNIQUE_KEYS);

            if (uniqueKeys.getLength() > 0) {
                loadUniqueKeyDefinitions(uniqueKeys, metaInput, nameContainer, namePattern);
            }
            final NodeList indexes = tableElement.getElementsByTagName(MetaTagConstants.INDEXES);

            if (indexes.getLength() > 0) {
                loadIndexDefinitions(indexes, metaInput, nameContainer, namePattern);
            }
        }
    }

    @Override
    protected OltpTableMetaInput buildTableMetaInput(final String tableName, final Element tableElement) {
        final String engineName = Objects.requireNonNullElse(getAttribute(tableElement, MetaTagConstants.ENGINE), DEFAULT_TABLE_ENGINE_NAME);

        return new OltpTableMetaInput(tableName, new OltpTableEngineConfiguration(engineName));
    }

    @Override
    protected DataType getDataType(final String dataTypeStr) {

        try {
            return CommonDataType.get(dataTypeStr);
        } catch (ErrorCode errorCode) {

            if (Objects.equals(errorCode.getCode(), ErrorCode.get(ErrorCodes.NOT_SUPPORTED).getCode())) {
                return OltpDataType.get(dataTypeStr);
            } else {
                throw errorCode;
            }
        }
    }

    @Override
    protected OltpColumnDefinition buildColumnDefinition(final Element columnElement, final String columnName, final DataType dataType,
                                                         final int convertedMaxLength, final Object convertedDefaultValue, final boolean isNullable) {
        if (dataType == CommonDataType.ARRAY) {
            final String elementTypeStr = getAttribute(columnElement, MetaTagConstants.ELEMENT_TYPE);

            if (elementTypeStr == null) {
                LOGGER.error("element-type attribute missing for ARRAY column: {}", columnName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
            }
            return new OltpArrayColumnDefinition(columnName, getDataType(elementTypeStr), convertedMaxLength, convertedDefaultValue, isNullable);
        }

        if (dataType instanceof final OltpDataType oltpDataType && oltpDataType.isArray()) {
            final CommonDataType elementType = oltpDataType.getElementDataType();

            if (elementType == null) {
                LOGGER.error("Unable to resolve element type for array column: {}", columnName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
            }
            return new OltpArrayColumnDefinition(columnName, elementType, convertedMaxLength, convertedDefaultValue, isNullable);
        }
        return new OltpColumnDefinition(columnName, dataType, convertedMaxLength, convertedDefaultValue, isNullable);
    }

    @Override
    protected OltpDecimalColumnDefinition buildDecimalColumnDefinition(final String columnName, final int convertedMaxLength,
                                                                       final int convertedPrecision, final int convertedScale,
                                                                       final Object convertedDefaultValue, final boolean isNullable) {
        return new OltpDecimalColumnDefinition(columnName, convertedMaxLength, convertedPrecision, convertedScale, convertedDefaultValue, isNullable);
    }

    @Override
    protected DatabaseType getDatabaseType() {
        return DatabaseType.OLTP;
    }

    @Override
    protected void loadForeignKeyDefinition(final String fkName, final String localTableName, final OltpTableDefinition refTableDef,
                                            final ColumnDefinition localColumnDef, final ColumnDefinition refColumnDef,
                                            final FKDefinition.FKConstraint fkConstraint, final OltpTableMetaInput metaInput,
                                            final Set<String> nameContainer) {
        final FKDefinition fkDefinition = new FKDefinition(fkName, localTableName, refTableDef, localColumnDef, refColumnDef, fkConstraint);

        loadIndexFromFKDefinition(metaInput, fkDefinition, nameContainer);
        metaInput.addFKDefinition(fkDefinition);
    }

    private void loadIndexFromFKDefinition(OltpTableMetaInput metaInput, FKDefinition fkDefinition, Set<String> nameContainer) {
        final String autoIndexDefName = fkDefinition.getName() + "_auto_index";

        if (!nameContainer.add(autoIndexDefName)) {
            LOGGER.error("Already Key with the same Index name(auto) exists, Name : {}", autoIndexDefName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_DUPLICATE_KEY_NAME);
        }
        metaInput.addIndexDefinition(new IndexDefinition(autoIndexDefName, List.of(fkDefinition.getLocal().getColumnName())));
    }

    private void loadUniqueKeyDefinitions(NodeList uniqueKeys, OltpTableMetaInput metaInput, Set<String> nameContainer, Pattern namePattern) {

        if (uniqueKeys.getLength() > 1) {
            LOGGER.error("Multiple unique-keys tag found inside table : {}", metaInput.getTableName());
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_UK_LIST);
        }
        final String pkColumn = metaInput.getPkDefinition().getColumnName();
        final Element uniqueKeysElement = (Element) uniqueKeys.item(0);
        final NodeList uniqueKeyNodes = uniqueKeysElement.getElementsByTagName(MetaTagConstants.UNIQUE_KEY);
        final int length = uniqueKeyNodes.getLength();
        final Set<String> existingColumns = metaInput.getColumns();

        for (int index = 0; index < length; index++) {
            final Element uniqueKeyElement = (Element) uniqueKeyNodes.item(index);
            final String name = getAttribute(uniqueKeyElement, MetaTagConstants.NAME);

            if (name == null) {
                LOGGER.error("UniqueKey name not found inside table : {}", metaInput.getTableName());
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_UK_NAME);
            }

            if (!namePattern.matcher(name).matches()) {
                LOGGER.error("Invalid UKName, Expected format [A-Za-z0-9_], current {}", name);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_UK_NAME);
            }

            if (!nameContainer.add(name)) {
                LOGGER.error("Already Key with the same name exists, Name : {}", name);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_DUPLICATE_KEY_NAME);
            }
            final Set<String> ukColumns = new HashSet<>();
            final NodeList uniqueKeyNode = uniqueKeyElement.getElementsByTagName(MetaTagConstants.UNIQUE_KEY_COLUMN);
            final int uniqueKeyNodeLength = uniqueKeyNode.getLength();

            for (int innerIndex = 0; innerIndex < uniqueKeyNodeLength; innerIndex++) {
                final Element element = (Element) uniqueKeyNode.item(innerIndex);
                final String nodeValue = element.getTextContent();
                if (nodeValue == null || nodeValue.isEmpty() || !existingColumns.contains(nodeValue)) {
                    LOGGER.error("Unknown column found inside UniqueKey : {}", name);
                    throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_COLUMN);
                }
                if (nodeValue.equals(pkColumn)) {
                    LOGGER.error("PK column found inside UniqueKey : {}", name);
                    throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
                }
                final ColumnDefinition columnDefinition = metaInput.getColumnDefinition(nodeValue);
                if (columnDefinition.getDataType().isStorageType()) {
                    LOGGER.error("Column cannot be configured for UniqueKey : {}", nodeValue);
                    throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
                }
                ukColumns.add(nodeValue);
            }

            if (ukColumns.isEmpty()) {
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_UK_CONFIG, name);
            }
            metaInput.addUKDefinition(new UKDefinition(name, ukColumns));
        }
    }

    private void loadIndexDefinitions(NodeList indexes, OltpTableMetaInput metaInput, Set<String> nameContainer, Pattern namePattern) {

        if (indexes.getLength() > 1) {
            LOGGER.error("Multiple indexes tag found inside table : {}", metaInput.getTableName());
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_INDEX_LIST);
        }
        final Set<String> notAllowedColumns = new HashSet<>();

        notAllowedColumns.add(metaInput.getPkDefinition().getColumnName());
        metaInput.getFkDefinitions().values().stream().map(FKDefinition::getLocal).map(ColumnDefinition::getColumnName).forEach(notAllowedColumns::add);

        final Element indexesElement = (Element) indexes.item(0);
        final NodeList indexNodes = indexesElement.getElementsByTagName(MetaTagConstants.INDEX);
        final Set<String> existingColumns = metaInput.getColumns();
        final int length = indexNodes.getLength();

        for (int index = 0; index < length; index++) {
            final Element indexElement = (Element) indexNodes.item(index);
            final String name = getAttribute(indexElement, MetaTagConstants.NAME);
            if (name == null) {
                LOGGER.error("Index name not found inside table : {}", metaInput.getTableName());
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_INDEX_NAME);
            }
            if (!namePattern.matcher(name).matches()) {
                LOGGER.error("Invalid IndexName exists, Expected format [A-Za-z0-9_], current {}", name);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_INDEX_NAME);
            }
            if (!nameContainer.add(name)) {
                LOGGER.error("Already Key with the same name exists, Name : {}", name);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_DUPLICATE_KEY_NAME);
            }
            final List<String> indexColumns = new ArrayList<>();
            final NodeList uniqueKeyNode = indexElement.getElementsByTagName(MetaTagConstants.INDEX_COLUMN);

            for (int innerIndex = 0; innerIndex < uniqueKeyNode.getLength(); innerIndex++) {
                final Element element = (Element) uniqueKeyNode.item(innerIndex);
                final String nodeValue = element.getTextContent();

                if (nodeValue == null || nodeValue.isEmpty() || !existingColumns.contains(nodeValue)) {
                    LOGGER.error("Unknown column found inside Index : {}", name);
                    throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_COLUMN);
                }

                if (notAllowedColumns.contains(nodeValue)) {
                    LOGGER.error("Invalid column found inside Index : {}", nodeValue);
                    throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
                }
                final ColumnDefinition columnDefinition = metaInput.getColumnDefinition(nodeValue);

                if (columnDefinition.getDataType().isStorageType()) {
                    LOGGER.error("Column cannot be configured for index : {}", nodeValue);
                    throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
                }

                if (!indexColumns.contains(nodeValue)) {
                    indexColumns.add(nodeValue);
                } else {
                    LOGGER.error("Column cannot be configured for index : {}", nodeValue);
                    throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_DUPLICATE_COLUMN_NAME);
                }
            }

            if (indexColumns.isEmpty()) {
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_IDX_CONFIG, name);
            }
            metaInput.addIndexDefinition(new IndexDefinition(name, indexColumns));
        }
    }
}