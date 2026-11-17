package com.tlc.crm.core.action.analytics.reports.data.export.internal;

import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.fields.models.StoreType;

import com.tlc.i18n.I18nResolver;

import com.tlc.pk.export.csvexporter.data.Cell;
import com.tlc.pk.export.csvexporter.data.Row;

import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Converts analytics graph data into a {@code Data} object for CSV export.
 *
 * <p>This class implements {@code CSVDataConverter} to transform a list of
 * {@code GraphAxis<Object>} into a structured {@code Data} object, containing
 * headers and rows suitable for CSV generation.</p>
 *
 * @author Rahul
 * @author Kathir
 * @version 1.0
 *
 */

class GraphCSVDataConverter extends AnalyticsCSVDataConverter
{
    private static final String HYPHEN = "-";
    private final UserService userService;

    private GraphCSVDataConverter() {
        this.userService = UserService.getInstance();
    }

    private static class Instance
    {
        private static final GraphCSVDataConverter INSTANCE = new GraphCSVDataConverter();
    }

    public static GraphCSVDataConverter getInstance()
    {
        return Instance.INSTANCE;
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
        final int rowCount = graphAxes.get(NumericalConstant.ZERO).getAxisValues().size();

        final List<Row> rows = IntStream.range(NumericalConstant.ZERO, rowCount)
                .mapToObj(index -> Row.builder().cells(new ArrayList<>()).build())
                .collect(Collectors.toList());

        final Map<Long, Long> columnIdToFieldId = graphAxes.stream()
                .filter(axis -> Objects.nonNull(axis.getReportField()))
                .collect(Collectors.toMap(axis -> axis.getReportField().getColumnId(),
                        axis -> axis.getReportField().getFieldId(),
                        (existing, duplicate) -> existing));

        final Map<Long, StoreType> fieldIdToStoreType = columnIdToFieldId.isEmpty()
                ? Collections.emptyMap()
                : resolveStoreTypes(orgId, new HashSet<>(columnIdToFieldId.values()), reportModularFieldMap);

        final Map<Long, StoreType> storeTypeMap = columnIdToFieldId.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> fieldIdToStoreType.get(entry.getValue())));

        for (final GraphAxis<Object> graphAxis : graphAxes) {
            final Long columnId = graphAxis.getReportField().getColumnId();
            final List<AxisValue<Object>> axisValues =  graphAxis.getAxisValues();

            for (int index = NumericalConstant.ZERO; index < rowCount; index++) {
                final Object value = axisValues.get(index).getValue();

                final Object cellValue = Objects.isNull(value) ? HYPHEN
                        : resolveCellValue(orgId, columnId, value, userMap, storeTypeMap.get(columnId), i18nResolver);

                rows.get(index)
                        .getCells()
                        .add(Cell.builder().value(Objects.toString(cellValue, HYPHEN)) .build());
            }
        }
        return rows;
    }

    /**
     * Extracts headers from the given list of graph axis data.
     *
     * <p>This method processes a list of {@code GraphAxis<Object>} and retrieves
     * the corresponding headers, which will be used as column names in the CSV export.</p>
     *
     * @param graphAxes the list of graph axis data from which headers are extracted (must not be {@code null})
     * @return a list of header names representing the column names in the CSV
     */
    protected @NonNull List<String> extractHeaders( final @NonNull List<GraphAxis<Object>> graphAxes )
    {
        return graphAxes.stream()
                .map( axis ->
                {
                    ReportField reportField = axis.getReportField();
                    return Objects.nonNull( reportField ) ? reportField.getLabel() : HYPHEN;
                })
                .collect( Collectors.toList() );
    }
}
