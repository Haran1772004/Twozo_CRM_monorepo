package com.tlc.crm.entity.service;

import com.tlc.crm.fields.sql.resource.OMETADDRESS;
import com.tlc.crm.fields.sql.resource.OMETDATERANGE;
import com.tlc.crm.fields.sql.resource.OMETDATETIMERANGE;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.fields.sql.resource.OMETMONETARY;
import com.tlc.crm.fields.sql.resource.OMETPHONE;
import com.tlc.crm.fields.sql.resource.OMETTIMERANGE;
import com.tlc.crm.entity.sql.resource.OMETOPTION;
import com.tlc.crm.security.sql.resource.OMCRMUSER;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Centralized provider for custom field foreign key relationship metadata.
 * This class replaces the dependency on FK definitions in meta.xml for custom wide tables.
 * </p>
 * <p>
 * The provider supplies relationship metadata for persistent custom fields including:
 * <ul>
 *     <li>Reference table resolution based on column patterns</li>
 *     <li>Join metadata for lookup fields</li>
 *     <li>Model field and entity field metadata</li>
 * </ul>
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class CustomFieldRelationProvider {

    private static final String CUSTOM_FIELD_TABLE_SUFFIX = "CustomFieldValues";

    // Column pattern prefixes for reference resolution
    private static final String CONTACT_ID_PREFIX = "CONTACT_ID";
    private static final String COMPANY_ID_PREFIX = "COMPANY_ID";
    private static final String PHONE_ID_PREFIX = "PHONE_ID";
    private static final String EMAIL_ID_PREFIX = "EMAIL_ID";
    private static final String MONETARY_ID_PREFIX = "MONETARY_ID";
    private static final String DATE_RANGE_ID_PREFIX = "DATE_RANGE_ID";
    private static final String TIME_RANGE_ID_PREFIX = "TIME_RANGE_ID";
    private static final String DATE_TIME_RANGE_ID_PREFIX = "DATE_TIME_RANGE_ID";
    private static final String ADDRESS_ID_PREFIX = "ADDRESS_ID";
    private static final String D_OPTION_ID_PREFIX = "D_OPTION_ID";
    private static final String M_OPTION_ID_PREFIX = "M_OPTION_ID";
    private static final String OWNER_ID_PREFIX = "OWNER_ID";

    // Reserved columns that should not have FK metadata removed
    public static final String ORG_ID = "ORG_ID";
    public static final String ENTITY_ID = "ENTITY_ID";

    // Reference table names
    private static final String MCO_CONTACT_TABLE = "MCOContact";
    private static final String MCO_COMPANY_TABLE = "MCOCompany";

    // Pattern for extracting column index
    private static final Pattern COLUMN_INDEX_PATTERN = Pattern.compile("(\\D+)(\\d+)$");

    // Custom wide table names
    private static final Set<String> CUSTOM_WIDE_TABLES = Set.of(
            "MCOContactCustomFieldValues",
            "MCOCompanyCustomFieldValues",
            "MDODealCustomFieldValues",
            "MPOProductCustomFieldValues"
    );

    private static class Instance {
        private static final CustomFieldRelationProvider INSTANCE = new CustomFieldRelationProvider();
    }

    private CustomFieldRelationProvider() {
    }

    /**
     * Gets the singleton instance of the provider.
     *
     * @return the singleton instance
     */
    public static CustomFieldRelationProvider getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Checks if the given table is a custom wide table.
     *
     * @param table the table to check
     * @return true if the table is a custom wide table
     */
    public boolean isCustomWideTable(final Table table) {
        return Objects.nonNull(table) && isCustomWideTable(table.getName());
    }

    /**
     * Checks if the given table is a custom wide table.
     *
     * @param tableName the table name to check
     * @return true if the table is a custom wide table
     */
    private boolean isCustomWideTable(final String tableName) {
        return Objects.nonNull(tableName) && (CUSTOM_WIDE_TABLES.contains(tableName) || tableName.endsWith(CUSTOM_FIELD_TABLE_SUFFIX));
    }

    /**
     * Gets the reference table name for a given column name in a custom wide table.
     *
     * @param columnName the column name
     * @return the reference table name, or null if not a reference column
     */
    public String getReferenceTable(final String columnName) {
        if (Objects.isNull(columnName) || columnName.isEmpty()) {
            return null;
        }
        final String prefix = extractColumnPrefix(columnName);

        return switch (prefix) {
            case CONTACT_ID_PREFIX -> MCO_CONTACT_TABLE;
            case COMPANY_ID_PREFIX -> MCO_COMPANY_TABLE;
            case PHONE_ID_PREFIX -> OMETPHONE.TABLE;
            case EMAIL_ID_PREFIX -> OMETEMAIL.TABLE;
            case MONETARY_ID_PREFIX -> OMETMONETARY.TABLE;
            case DATE_RANGE_ID_PREFIX -> OMETDATERANGE.TABLE;
            case TIME_RANGE_ID_PREFIX -> OMETTIMERANGE.TABLE;
            case DATE_TIME_RANGE_ID_PREFIX -> OMETDATETIMERANGE.TABLE;
            case ADDRESS_ID_PREFIX -> OMETADDRESS.TABLE;
            case D_OPTION_ID_PREFIX, M_OPTION_ID_PREFIX -> OMETOPTION.TABLE;
            case OWNER_ID_PREFIX -> OMCRMUSER.TABLE;
            default -> null;
        };
    }

    /**
     * <p>
     * Fetches the FK columns (reference table to local column mapping) for a custom wide table.
     * This replaces the direct dependency on table.getTableDefinition().getFKDefinitions().
     * </p>
     *
     * @param table the custom wide table
     * @return map of reference table name to local column
     */
    public Map<String, Column> fetchFkColumns(final Table table) {
        if (!isCustomWideTable(table)) {
            return Collections.emptyMap();
        }
        final Map<String, Column> fkColumns = new HashMap<>();
        final Set<String> columns = table.getTableDefinition().getColumns();

        for (final String columnName : columns) {
            if (isReservedColumn(columnName)) {
                continue;
            }
            final String referenceTable = getReferenceTable(columnName);

            if (Objects.nonNull(referenceTable)) {
                fkColumns.putIfAbsent(referenceTable, table.getColumn(columnName));
            }
        }
        return fkColumns;
    }

    /**
     * Checks if the column is a reserved column that should retain FK definition.
     *
     * @param columnName the column name
     * @return true if reserved
     */
    public boolean isReservedColumn(final String columnName) {
        return ORG_ID.equals(columnName) || ENTITY_ID.equals(columnName);
    }

    /**
     * Extracts the column prefix from a column name.
     *
     * @param columnName the column name
     * @return the prefix
     */
    private String extractColumnPrefix(final String columnName) {
        final Matcher matcher = COLUMN_INDEX_PATTERN.matcher(columnName);

        if (matcher.matches()) {
            return matcher.group(1);
        }
        return columnName;
    }
}