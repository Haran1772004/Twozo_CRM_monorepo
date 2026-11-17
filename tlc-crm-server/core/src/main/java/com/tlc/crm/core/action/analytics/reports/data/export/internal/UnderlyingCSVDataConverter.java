package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;

import com.tlc.crm.fields.models.StoreType;

import com.tlc.i18n.I18nResolver;

import com.tlc.pk.export.csvexporter.data.Cell;
import com.tlc.pk.export.csvexporter.data.Data;
import com.tlc.pk.export.csvexporter.data.Row;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Singleton CSV data converter implementation for converting a list of {@link GraphAxis} objects
 * into a structured CSV data format, containing headers and rows.
 *
 * @author Kathir
 * @version 1.0
 */
class UnderlyingCSVDataConverter extends AnalyticsCSVDataConverter
{
    private static final String HYPHEN = "-";
    private static final Cell<?> HYPHEN_CELL = Cell.builder().value(HYPHEN).build();

    private record ColumnOrder( String columnName, Long columnId )
    {
    }

    /**
     * <p>
     * Converts graph axis data into CSV rows using default column ordering.
     * </p>
     *
     * @param orgId                 the organization ID (must not be null)
     * @param graphAxes             the list of graph axes representing the underlying data (must not be null)
     * @param userMap               the user ID to CrmUser mapping for value resolution
     * @param reportModularFieldMap a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @param i18nResolver          the i18n resolver instance for internationalization (must not be null)
     * @return a {@link Data} object ready for CSV export with default column ordering
     */
    @Override
    public @NonNull Data convertToCSVRows(final @NonNull Long orgId,
                                          final @NonNull List<GraphAxis<Object>> graphAxes,
                                          final Map<Long, CrmUser> userMap,
                                          final @NonNull Map<Long, ReportModularField> reportModularFieldMap,
                                          final @NonNull I18nResolver i18nResolver) {
        return convertToCSVRows(orgId, graphAxes, userMap, reportModularFieldMap, Collections.emptyList(), i18nResolver);
    }

    /**
     * Converts graph axis data into CSV rows, ordering columns exactly as specified by {@code selectFieldIds}.
     *
     * @param orgId                 organization ID
     * @param graphAxes             list of graph axes representing the underlying data
     * @param userMap               user ID to CrmUser mapping
     * @param reportModularFieldMap a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @param selectFieldIds        ordered list of fieldIds from the request {@code select} array
     * @param i18nResolver          i18n resolver
     * @return a {@link Data} object ready for CSV export
     */
    public @NonNull Data convertToCSVRows(final @NonNull Long orgId,
                                          final @NonNull List<GraphAxis<Object>> graphAxes,
                                          final Map<Long, CrmUser> userMap,
                                          final @NonNull Map<Long, ReportModularField> reportModularFieldMap,
                                          final @NonNull List<Long> selectFieldIds,
                                          final @NonNull I18nResolver i18nResolver) {
        final List<ColumnOrder> sortedColumns = getSortedColumns(orgId, graphAxes, selectFieldIds);

        return Data.builder()
                .headers(Optional.of(sortedColumns.stream().map(ColumnOrder::columnName).collect(Collectors.toList())))
                .rows(extractAndConvertData(orgId, graphAxes, userMap, sortedColumns, reportModularFieldMap, i18nResolver))
                .stringConverter(cell -> String.valueOf(cell.getValue()))
                .build();
    }


    private UnderlyingCSVDataConverter() {
    }

    private static class Instance
    {
        private static final UnderlyingCSVDataConverter INSTANCE = new UnderlyingCSVDataConverter();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link UnderlyingCSVDataConverter}.
     * </p>
     *
     * @return The singleton instance of UnderlyingCSVDataConverter
     */
    public static UnderlyingCSVDataConverter getInstance()
    {
        return Instance.INSTANCE;
    }

    /**
     * Extracts unique header names from the provided {@code graphAxes} list.
     *
     * @param graphAxes the list of GraphAxis objects containing chart data
     * @return a list of column headers for CSV export
     */
    protected @NonNull List<String> extractHeaders( final @NonNull List<GraphAxis<Object>> graphAxes )
    {
        return getSortedColumns(null, graphAxes, Collections.emptyList()).stream()
                .map(ColumnOrder::columnName)
                .collect(Collectors.toList());
    }

    /**
     * Converts graph axis data into a list of CSV-ready rows.
     *
     * <p>Each {@link GraphAxis} represents a column with multiple values.
     * This method transforms the column-wise data into row-wise {@link Row} objects,
     * resolving values based on their type and formatting rules.</p>
     *
     * @param orgId                 organization ID used for context-based resolution
     * @param graphAxes             list of graph axes representing column data (must not be null)
     * @param userMap               map of user IDs to user details for value resolution
     * @param reportModularFieldMap a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @param i18nResolver          an instance of {@link I18nResolver} for handling internationalization
     * @return a collection of {@link Row} objects ready for CSV export
     */
    protected @NonNull Collection<Row> extractAndConvertData(final @NonNull Long orgId,
                                                             final @NonNull List<GraphAxis<Object>> graphAxes,
                                                             final Map<Long, CrmUser> userMap,
                                                             final @NonNull Map<Long, ReportModularField> reportModularFieldMap,
                                                             final @NonNull I18nResolver i18nResolver) {
        return extractAndConvertData(orgId, graphAxes, userMap, getSortedColumns(orgId, graphAxes, Collections.emptyList()), reportModularFieldMap, i18nResolver);
    }

    /**
     * <p>
     * Converts graph axis data into a list of CSV-ready rows with explicit column ordering.
     * </p>
     *
     * @param orgId                 the organization ID used for context-based value resolution
     * @param graphAxes             the list of graph axes representing column data (must not be null)
     * @param userMap               the map of user IDs to user details for user column value resolution
     * @param sortedColumns         the list of ColumnOrder objects specifying the exact order and position of columns in the output
     * @param reportModularFieldMap a map of field ID to {@link ReportModularField} objects containing modular field metadata
     * @param i18nResolver          the i18n resolver instance for handling internationalization
     * @return a collection of {@link Row} objects with cells populated in the order specified by sortedColumns, ready for CSV export
     */
    private @NonNull Collection<Row> extractAndConvertData(final @NonNull Long orgId,
                                                           final @NonNull List<GraphAxis<Object>> graphAxes,
                                                           final Map<Long, CrmUser> userMap,
                                                           final @NonNull List<ColumnOrder> sortedColumns,
                                                           final @NonNull Map<Long, ReportModularField> reportModularFieldMap,
                                                           final @NonNull I18nResolver i18nResolver) {
        final int columnsPerRow = sortedColumns.size();

        if (Objects.equals(NumericalConstant.ZERO, columnsPerRow)) {
            return Collections.emptyList();
        }

        final Map<Long, Integer> sortedColumnIndex = IntStream.range(NumericalConstant.ZERO, columnsPerRow)
                .boxed()
                .collect(Collectors.toMap(index -> sortedColumns.get(index).columnId(), index -> index,
                        (existing, current) -> existing, LinkedHashMap::new));

        final Map<String, Integer> sortedColumnNameIndex = IntStream.range(NumericalConstant.ZERO, columnsPerRow)
                .boxed()
                .collect(Collectors.toMap(index -> sortedColumns.get(index).columnName(), index -> index,
                        (existing, current) -> existing, LinkedHashMap::new));

        final Map<Long, Long> columnIdToFieldId = new HashMap<>();
        int validColumnCount = NumericalConstant.ZERO;

        for (final GraphAxis<Object> axis : graphAxes) {
            if (!CRMReportKeys.ID.equalsIgnoreCase(String.valueOf(axis.getColumnName()))) {
                validColumnCount++;
            }
            columnIdToFieldId.putAll(axis.getColumnToFieldIdMap());
        }

        final int totalRows = validColumnCount / columnsPerRow;
        final List<Row> rows = new ArrayList<>(totalRows);
        final List<List<Cell<?>>> rowCells = new ArrayList<>(totalRows);

        for (int row = NumericalConstant.ZERO; row < totalRows; row++) {
            final List<Cell<?>> cells = new ArrayList<>(columnsPerRow);

            for (int columnIndex = NumericalConstant.ZERO; columnIndex < columnsPerRow; columnIndex++) {
                cells.add(HYPHEN_CELL);
            }

            rowCells.add(cells);
            rows.add(Row.builder().cells(cells).build());
        }

        final Map<Long, StoreType> fieldIdToStoreTypes = columnIdToFieldId.isEmpty()
                ? Collections.emptyMap()
                : resolveStoreTypes(orgId, new HashSet<>(columnIdToFieldId.values()), reportModularFieldMap);

        final Map<Long, StoreType> storeTypeMaps = columnIdToFieldId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> fieldIdToStoreTypes.get(entry.getValue())));
        int dataIndex = NumericalConstant.ZERO;

        for (final GraphAxis<Object> axis : graphAxes) {
            if (CRMReportKeys.ID.equalsIgnoreCase(Objects.toString(axis.getColumnName(), ""))) {
                continue;
            }

            final int rowIndex = dataIndex / columnsPerRow;

            if (rowIndex >= rows.size()) {
                break;
            }

            final String columnName = Objects.toString(axis.getColumnName(), "");
            final Long columnId = resolveColumnId(axis, columnName);
            final Integer targetColumnIndex = Objects.nonNull(columnId)
                    ? sortedColumnIndex.get(columnId)
                    : sortedColumnNameIndex.get(columnName);

            if (Objects.isNull(targetColumnIndex)) {
                dataIndex++;
                continue;
            }

            final Object value = axis.getValue();
            final Object cellValue = Objects.isNull(value) ? HYPHEN
                    : resolveCellValue(orgId, columnId, value, userMap, storeTypeMaps.get(columnId), i18nResolver);

            final String finalStringValue = Objects.toString(cellValue, HYPHEN);
            final Cell<?> cellToInsert;

            if (HYPHEN.equals(finalStringValue)) {
                cellToInsert = HYPHEN_CELL;
            } else {
                cellToInsert = Cell.builder().value(finalStringValue).build();
            }

            rowCells.get(rowIndex).set(targetColumnIndex, cellToInsert);
            dataIndex++;
        }

        return rows;
    }

    /**
     * <p>
     * Extracts column names and IDs from the first row of graph axes, then sorts them according to selectFieldIds order.
     * </p>
     *
     * @param orgId          The organization ID (may be null; not used in current implementation)
     * @param graphAxes      The list of graph axes to extract column information from
     * @param selectFieldIds The ordered list of field IDs specifying the desired column order;
     *                       empty list results in natural order
     * @return A list of ColumnOrder objects representing columns in the requested order
     */
    @SuppressWarnings( "unused" )
    private List<ColumnOrder> getSortedColumns(final Long orgId, final List<GraphAxis<Object>> graphAxes,
                                               final List<Long> selectFieldIds) {
        final Map<String, Long> firstRowColumns = new LinkedHashMap<>();
        final Map<Long, Long> columnIdToFieldId = new HashMap<>();
        String firstDataColumn = null;

        for (final GraphAxis<Object> graphAxis : graphAxes) {
            if (Objects.nonNull(graphAxis.getColumnToFieldIdMap())) {
                columnIdToFieldId.putAll(graphAxis.getColumnToFieldIdMap());
            }

            final String columnName = Objects.toString(graphAxis.getColumnName(), "");

            if (CRMReportKeys.ID.equalsIgnoreCase(columnName)) {
                continue;
            }

            if (Objects.isNull(firstDataColumn)) {
                firstDataColumn = columnName;
            } else if (Objects.equals(firstDataColumn, columnName) && firstRowColumns.containsKey(columnName)) {
                break;
            }

            firstRowColumns.putIfAbsent(columnName, resolveColumnId(graphAxis, columnName));
        }

        final List<ColumnOrder> columns = firstRowColumns.entrySet().stream()
                .map(entry -> new ColumnOrder(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        if (selectFieldIds.isEmpty()) {
            return columns;
        }

        final Map<Long, Integer> fieldIdToSelectPosition = new HashMap<>();

        for (int i = NumericalConstant.ZERO; i < selectFieldIds.size(); i++) {
            fieldIdToSelectPosition.put(selectFieldIds.get(i), i);
        }

        columns.sort(Comparator.comparingInt(col ->
        {
            final Long colId = col.columnId();
            final Long fieldId = Objects.nonNull(colId) ? columnIdToFieldId.get(colId) : null;

            return fieldIdToSelectPosition.getOrDefault(fieldId, Integer.MAX_VALUE);
        }));

        return columns;
    }


    /**
     * <p>
     * Resolves the column ID for a given column name from the {@link GraphAxis}.
     * </p>
     *
     * @param graphAxis  The graph axis containing the column name to ID mapping
     * @param columnName The name of the column to resolve the ID for
     * @return The column ID if found, otherwise the first available column ID from the mapping, or null if no mapping exists
     */
    private Long resolveColumnId(final GraphAxis<Object> graphAxis, final String columnName) {
        final Map<String, Long> columnNameMap = graphAxis.getColumnNameMap();

        if (Objects.isNull(columnNameMap) || columnNameMap.isEmpty()) {
            return null;
        }

        final Long columnId = columnNameMap.get(columnName);

        if (Objects.nonNull(columnId)) {
            return columnId;
        }

        return columnNameMap.values().stream().findFirst().orElse(null);
    }
}
