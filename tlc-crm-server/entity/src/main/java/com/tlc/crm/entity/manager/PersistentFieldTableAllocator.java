package com.tlc.crm.entity.manager;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.StringUtil;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;
import com.tlc.crm.fields.models.FieldStore;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * Manages Persistent field table allocation and column index assignment across multiple field tables.
 * This implementation is flexible and supports dynamic column ranges based on the FieldStore's COLUMN_RANGE and TABLE_RANGE configuration.
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class PersistentFieldTableAllocator {


    /**
     * <p>
     * Gets the maximum columns per table from the field store's columnRange configuration.
     * </p>
     *
     * @param fieldStore Represents the {@link FieldStore}
     * @return The number of columns per table
     */
    public static int getColumnsPerTable(final FieldStore fieldStore) {
        final Integer maxRange = extractMaxRange(fieldStore.columnRange());

        if (Objects.isNull(maxRange)) {
            throw ErrorCode.get(EntityErrorCodes.COLUMN_RANGE_NOT_FOUND);
        }
        return maxRange;
    }

    /**
     * <p>
     * Gets the maximum number of tables from the field store's table range configuration.
     * Falls back to default if not configured.
     * </p>
     *
     * @param fieldStore Represents the {@link FieldStore}
     * @return The maximum number of tables
     */
    private static int getMaxTables(final FieldStore fieldStore) {
        final Integer maxRange = extractMaxRange(fieldStore.tableRange());

        if (Objects.isNull(maxRange)) {
            throw ErrorCode.get(EntityErrorCodes.TABLE_RANGE_NOT_FOUND);
        }
        return maxRange;
    }

    /**
     * <p>
     * Extracts the max range from the total range
     * </p>
     *
     * @param range Represents the given total range
     * @return The max range
     */
    private static Integer extractMaxRange(final String range) {

        if (Objects.nonNull(range) && !range.isEmpty()) {
            try {
                final String[] availableRanges = range.split(StringUtil.Delimiter.HYPHEN.getDelimiter());

                return Integer.parseInt(availableRanges[1].trim());
            } catch (final Exception exception) {
                throw ErrorCode.get(CommonErrorCodes.INVALID_NUMBER_FORMAT);
            }
        }
        return null;
    }

    /**
     * <p>
     * Allocates a column index and table index for a new custom field.
     * The allocation is flexible and based on the FieldStore's column type configuration.
     * </p>
     *
     * @param fieldStore Holds field store information
     * @return An allocation record containing tableIndex, columnIndex, and tableName
     */
    public FieldTableAllocation allocateColumnSlot(final FieldStore fieldStore, final Collection<Integer> usedColumnRanges) {
        final int columnsPerTable = getColumnsPerTable(fieldStore);
        final int maxTables = getMaxTables(fieldStore);
        final int maxColumns = maxTables * columnsPerTable;

        for (int globalColumnIndex = 1; globalColumnIndex <= maxColumns; globalColumnIndex++) {

            if (!usedColumnRanges.contains(globalColumnIndex)) {
                final short tableIndex = ((short) getTableIndexForColumn(globalColumnIndex, columnsPerTable));
                final String tableName = generateTableName(fieldStore, tableIndex);

                usedColumnRanges.add(globalColumnIndex);

                return new FieldTableAllocation(tableIndex, globalColumnIndex, tableName);
            }
        }
        throw ErrorCode.get(EntityErrorCodes.NO_COLUMNS_ARE_AVAILABLE, "i18n.entity.fields.limit.reached");
    }

    /**
     * <p>
     * Computes the table index for a given global column index using the configurable columns per table.
     * </p>
     *
     * @param globalColumnIndex The global column index
     * @param columnsPerTable The number of columns per table
     * @return The table index
     */
    public static int getTableIndexForColumn(final int globalColumnIndex, final int columnsPerTable) {
        return ((globalColumnIndex - 1) / columnsPerTable) + 1;
    }

    /**
     * <p>
     * Computes the column slot using the configurable columns per table.
     * </p>
     *
     * @param globalColumnIndex The global column index
     * @param columnsPerTable The number of columns per table
     * @return The column slot within the table
     */
    public static int getColumnSlotForIndex(final int globalColumnIndex, final int columnsPerTable) {
        return ((globalColumnIndex - 1) % columnsPerTable) + 1;
    }

    /**
     * <p>
     * Generates the physical table name for a custom field table.
     * Uses SourceCustomFieldTableResolver to resolve source-specific table names.
     * </p>
     *
     * @param fieldStore The field store
     * @param tableIndex The table index
     * @return The table name
     */
    private String generateTableName(final FieldStore fieldStore, final int tableIndex) {
        final String baseTableName = fieldStore.table();

        if (1 == tableIndex) {
            return baseTableName;
        } else {
            return String.format("%s%d", baseTableName, tableIndex);
        }
    }

    /**
     * <p>
     * Data class holding the result of a field table allocation.
     * </p>
     */
    public static class FieldTableAllocation {

        private final short tableIndex;
        private final int columnSlot;
        private final String tableName;

        public FieldTableAllocation(final short tableIndex, final int columnSlot, final String tableName) {
            this.tableIndex = tableIndex;
            this.columnSlot = columnSlot;
            this.tableName = tableName;
        }

        public short getTableIndex() {
            return tableIndex;
        }

        public int getColumnSlot() {
            return columnSlot;
        }

        public String getTableName() {
            return tableName;
        }
    }
}