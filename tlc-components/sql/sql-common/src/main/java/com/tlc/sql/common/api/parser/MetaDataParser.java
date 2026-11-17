package com.tlc.sql.common.api.parser;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.DecimalColumnDefinition;
import com.tlc.sql.common.api.meta.FKDefinition;
import com.tlc.sql.common.api.meta.ImmutableColumns;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.meta.TableEngineConfiguration;
import com.tlc.sql.common.api.meta.TableMetaInput;
import com.tlc.sql.common.api.meta.TableType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public abstract class MetaDataParser<E extends TableEngineConfiguration, C extends ColumnDefinition, T extends TableDefinition<C>, M extends TableMetaInput<E, C>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataParser.class);
    private static final AtomicInteger TABLE_ORDER = new AtomicInteger(100);
    private static final String NAME_FORMAT = "[A-Za-z0-9_]";
    private static final Pattern GLOBAL_PATTERN = Pattern.compile("^" + NAME_FORMAT + "+$");

    public void processMetaData(final InputStream inputStream, final TableDefinitionLoader<C, T> definitionProvider) {

        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(inputStream);
            final Element rootElement = doc.getDocumentElement();
            final NodeList modules = rootElement.getElementsByTagName(MetaTagConstants.MODULE);
            final int totalModules = modules.getLength();

            LOGGER.info("Total Modules found : {}", totalModules);

            for (int index = 0; index < totalModules; index++) {
                final Element moduleElement = (Element) modules.item(index);
                final String databaseTypeName = getAttribute(moduleElement, MetaTagConstants.DATABASE_TYPE);
                final DatabaseType moduleDatabaseType = Objects.isNull(databaseTypeName) ? DatabaseType.OLTP : DatabaseType.get(Integer.parseInt(databaseTypeName));

                if (moduleDatabaseType == getDatabaseType()) {
                    final String moduleName = getAttribute(moduleElement, MetaTagConstants.NAME);
                    final NodeList allTables = moduleElement.getElementsByTagName(MetaTagConstants.TABLE);
                    final int totalTables = allTables.getLength();

                    LOGGER.info("Total tables found : {}, Module : {}", totalTables, moduleName);

                    for (int iIndex = 0; iIndex < totalTables; iIndex++) {
                        final Element element = (Element) allTables.item(iIndex);
                        final String name = getAttribute(element, MetaTagConstants.NAME);

                        if (name == null) {
                            LOGGER.error("Table tag found without name or type");
                            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_TABLE_NAME, "Empty tableName in module : " + moduleName);
                        }

                        try {
                            loadTableDefinition(name, element, TABLE_ORDER.getAndIncrement(), definitionProvider);
                        } catch (Exception exception) {
                            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_FILE, "Unable to parse table : " + name, exception);
                        }
                    }
                }
            }
            LOGGER.info("Successfully parsed XmlFile");
        } catch (Exception exp) {
            LOGGER.info("Failed to process XmlFile ");
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_FILE, exp);
        }
    }

    protected String getAttribute(final Element element, final String name) {
        final String value = element.getAttribute(name);
        return !value.isEmpty() ? value : null;
    }

    protected abstract void loadAdditionalDefinitions(final Element tableElement, final TableDefinitionLoader<C, T> definitionProvider,
                                                      final M metaInput, final Set<String> nameContainer, final Pattern namePattern);

    protected abstract T buildTableDefinition(final String name, final TableType type, final int seqId, final M metaInput);

    protected abstract M buildTableMetaInput(final String tableName, final Element tableElement);

    protected abstract DataType getDataType(final String dataTypeStr);

    protected abstract C buildColumnDefinition(final Element columnElement, final String columnName, final DataType dataType, final int convertedMaxLength,
                                               final Object convertedDefaultValue, final boolean isNullable);

    protected abstract C buildDecimalColumnDefinition(final String columnName, final int convertedMaxLength, final int convertedPrecision,
                                                      final int convertedScale, final Object convertedDefaultValue, final boolean isNullable);

    protected abstract DatabaseType getDatabaseType();

    protected abstract void loadPrimaryKeyDefinition(final Element tableElement, final String tableName,
                                                     final Set<String> nameContainer, final M metaInput,
                                                     final Pattern namePattern);

    protected abstract void loadForeignKeyDefinition(final String fkName, final String localTableName, final T refTableDef, final ColumnDefinition localColumnDef, final ColumnDefinition refColumnDef, final FKDefinition.FKConstraint fkConstraint, final M metaInput, final Set<String> nameContainer);

    private void loadTableDefinition(final String tableName, final Element tableElement, final int tableOrder,
                                     final TableDefinitionLoader<C, T> definitionProvider) {
        final String tableType = getAttribute(tableElement, MetaTagConstants.TYPE);
        final TableType typeEnum = tableType != null ? TableType.get(Integer.parseInt(tableType)) : TableType.COMMON;

        if (!GLOBAL_PATTERN.matcher(tableName).matches()) {
            LOGGER.error("Invalid tableName exists, Expected format [A-Za-z0-9_], current {}", tableName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_TABLE_NAME);
        }

        if (definitionProvider.isDuplicate(tableName)) {
            LOGGER.error("Table with same name already exists, table name : {}", tableName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_DUPLICATE_TABLE_NAME);
        }
        final NodeList columns = tableElement.getElementsByTagName(MetaTagConstants.COLUMNS);

        if (columns.getLength() == 0) {
            LOGGER.error("Columns tag notFound in table : {}", tableName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN_LIST);
        }
        final Pattern namePattern = Pattern.compile("^" + tableName + "_" + NAME_FORMAT + "+$");
        final M metaInput = buildTableMetaInput(tableName, tableElement);

        loadColumnDefinition(tableName, columns, metaInput);

        if (typeEnum.isOrgDependent() && metaInput.getColumnDefinition(ImmutableColumns.ORG_ID.getName()) == null) {
            LOGGER.error("OrgId column missing in table : {}", tableName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_ORG_COLUMN_MISSING);
        }
        final Set<String> nameContainer = new HashSet<>();

        loadPrimaryKeyDefinition(tableElement, tableName, nameContainer, metaInput, namePattern);
        final NodeList foreignKeys = tableElement.getElementsByTagName(MetaTagConstants.FOREIGN_KEYS);

        if (foreignKeys.getLength() > 0) {
            loadForeignKeyDefinitions(foreignKeys, metaInput, definitionProvider, nameContainer, namePattern);
        }

        loadAdditionalDefinitions(tableElement, definitionProvider, metaInput, nameContainer, namePattern);
        definitionProvider.add(buildTableDefinition(tableName, typeEnum, tableOrder, metaInput));
    }

    private void loadColumnDefinition(final String tableName, final NodeList columns, final M metaInput) {

        if (columns.getLength() > 1) {
            LOGGER.error("Multiple Columns tag found inside table : {}", tableName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN_LIST);
        }
        final Element columnsElement = (Element) columns.item(0);
        final NodeList allColumns = columnsElement.getElementsByTagName(MetaTagConstants.COLUMN);
        final int allColumnsLength = allColumns.getLength();

        if (allColumnsLength == 0) {
            LOGGER.error("No valid Column tag found inside table : {}", tableName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN_LIST);
        }

        for (int index = 0; index < allColumnsLength; index++) {
            final Element columnElement = (Element) allColumns.item(index);
            final String columnName = getAttribute(columnElement, MetaTagConstants.NAME);
            final String dataTypeStr = getAttribute(columnElement, MetaTagConstants.DATATYPE);

            if (columnName == null || dataTypeStr == null) {
                LOGGER.error("ColumnName and dataType cannot be empty, TableName : {}", tableName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN_NAME);
            }

            if (!GLOBAL_PATTERN.matcher(columnName).matches()) {
                LOGGER.error("Invalid columnName exists, Expected format [A-Za-z0-9_], TableName : {}  Column : {}", tableName, columnName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN_NAME);
            }

            if (metaInput.getColumnDefinition(columnName) != null) {
                LOGGER.error("Already Column with the same name exists, TableName : {}  Column : {}", tableName, columnName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_DUPLICATE_COLUMN_NAME);
            }
            metaInput.addColumnDefinition(buildColumnDefinition(tableName, columnName, columnElement));
        }
    }


    private void loadForeignKeyDefinitions(final NodeList foreignKeysTag, final M metaInput,
                                           final TableDefinitionLoader<C, T> definitionProvider,
                                           final Set<String> nameContainer, final Pattern namePattern) {
        final String localTableName = metaInput.getTableName();

        if (foreignKeysTag.getLength() > 1) {
            LOGGER.error("Multiple foreign-keys tag found inside table : {}", localTableName);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_FK_LIST);
        }
        final Element foreignKeysElement = (Element) foreignKeysTag.item(0);
        final NodeList foreignKeys = foreignKeysElement.getElementsByTagName(MetaTagConstants.FOREIGN_KEY);
        final int foreignKeysLength = foreignKeys.getLength();

        for (int index = 0; index < foreignKeysLength; index++) {
            final Element fkElement = (Element) foreignKeys.item(index);
            final String fkName = getAttribute(fkElement, MetaTagConstants.NAME);

            if (fkName == null) {
                LOGGER.error("ForeignKey name not found inside table : {}", localTableName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_FK_NAME);
            }

            if (!namePattern.matcher(fkName).matches()) {
                LOGGER.error("Invalid FKName exists, Expected format [A-Za-z0-9_], current {}", fkName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_FK_NAME);
            }

            if (!nameContainer.add(fkName)) {
                LOGGER.error("Already Key with the same name exists, Name : {}", fkName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_DUPLICATE_KEY_NAME);
            }
            final String referencedTable = getAttribute(fkElement, MetaTagConstants.FK_REFERENCETABLE);

            if (referencedTable == null) {
                LOGGER.error("Invalid table name in FKName {}", fkName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_TABLE);
            }
            final T refTableDef = definitionProvider.resolve(referencedTable);

            if (refTableDef == null) {
                LOGGER.error("Unable to load FK {}, No such table exists {}", fkName, referencedTable);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_TABLE);
            }
            final String localColumn = getAttribute(fkElement, MetaTagConstants.FK_LOCALCOLUMN);
            final String refColumnColumn = getAttribute(fkElement, MetaTagConstants.FK_REFERENCECOLUMN);

            if (localColumn == null || refColumnColumn == null) {
                LOGGER.error("Invalid column name found in fk-column, FK name : {}", fkName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN_NAME);
            }
            final ColumnDefinition refColumnDef = refTableDef.getColumnDefinition(refColumnColumn);
            final ColumnDefinition localColumnDef = metaInput.getColumnDefinition(localColumn);

            if (refColumnDef == null || localColumnDef == null) {
                LOGGER.error("Unknown column name found in fk-column, FK name : {}", fkName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_COLUMN);
            }
            final DataType localDataType = localColumnDef.getDataType();

            if (localDataType.isStorageType()) {
                LOGGER.error("Column cannot be configured for ForeignKey : {}", localColumn);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_COLUMN);
            }

            if (refColumnDef.getDataType() != localDataType) {
                LOGGER.error("Remote and Local column type mismatch inside fk-column, FK name : {}", fkName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_FK_COLUMN_TYPE_MISMATCH);
            }
            final FKDefinition.FKConstraint fkConstraint = FKDefinition.FKConstraint.get(getAttribute(fkElement, MetaTagConstants.FK_CONSTRAINT));

            if (fkConstraint == null) {
                LOGGER.error("Invalid FK Constraints found, FK name : {}", fkName);
                throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_FK_INVALID_CONSTRAINT);
            }
            loadForeignKeyDefinition(fkName, localTableName, refTableDef, localColumnDef, refColumnDef, fkConstraint, metaInput, nameContainer);
        }
    }

    private C buildColumnDefinition(final String tableName, final String columnName, final Element columnElement) {
        final String dataTypeStr = getAttribute(columnElement, MetaTagConstants.DATATYPE);
        final DataType dataType = getDataType(dataTypeStr);

        if (dataType == null) {
            LOGGER.error("Unsupported dataType provided, TableName : {}  Column : {} DataType : {}", tableName, columnName, dataTypeStr);
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
        }
        final int convertedMaxLength = getMaxLength(columnElement);
        final boolean isNullable = Boolean.parseBoolean(getAttribute(columnElement, MetaTagConstants.NULLABLE));
        final String defaultValue = getAttribute(columnElement, MetaTagConstants.DEFAULTVALUE);
        final Object convertedDefaultValue;

        if (defaultValue != null && !defaultValue.equalsIgnoreCase("NULL")) {
            convertedDefaultValue = parseDefaultValue(dataType, defaultValue);
        } else {
            convertedDefaultValue = null;
        }

        if (dataType.isDecimal()) {
            final String precision = getAttribute(columnElement, MetaTagConstants.PRECISION);
            final String scale = getAttribute(columnElement, MetaTagConstants.SCALE);
            final int convertedPrecision = precision != null ? Integer.parseInt(precision)
                    : DecimalColumnDefinition.DEFAULT_PRECISION;
            final int convertedScale = scale != null ? Integer.parseInt(scale)
                    : DecimalColumnDefinition.DEFAULT_SCALE;

            return buildDecimalColumnDefinition(columnName, convertedMaxLength,
                    convertedPrecision, convertedScale, convertedDefaultValue, isNullable);
        } else {
            return buildColumnDefinition(columnElement, columnName, dataType, convertedMaxLength, convertedDefaultValue, isNullable);
        }
    }

    private int getMaxLength(final Element columnElement) {
        final String maxLength = getAttribute(columnElement, MetaTagConstants.MAXLENGTH);

        if (maxLength != null) {
            return Integer.parseInt(maxLength);
        } else {
            return ColumnDefinition.DEFAULT_COLUMN_LENGTH;
        }
    }

    /**
     * Parse default value for columns, with special handling for complex types.
     *
     * For complex types like Array, Map, and Tuple, the default value format is:
     * - Empty Array: "[]"
     * - Array with values: "[value1, value2, ...]"
     * - Empty Map: "{}"
     * - Map with values: "{key1: value1, key2: value2, ...}"
     *
     * For simple types, delegates to dataType.getUnWrappedValue()
     *
     * @param dataType The data type of the column
     * @param defaultValue The string representation of the default value
     * @return The parsed default value object, or null if parsing fails
     */
    protected Object parseDefaultValue(final DataType dataType, final String defaultValue) {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return null;
        }

        if ("[]".equals(defaultValue.trim())) {
            return new java.util.ArrayList<>();
        }

        if ("{}".equals(defaultValue.trim())) {
            return new java.util.LinkedHashMap<>();
        }

        try {
            return dataType.getUnWrappedValue(defaultValue);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse default value '{}' for dataType {}, returning null", defaultValue, dataType);
            return null;
        }
    }

}