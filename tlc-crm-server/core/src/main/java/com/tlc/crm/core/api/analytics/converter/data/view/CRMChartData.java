package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.core.api.ChartRequest;

import com.tlc.commons.util.constants.NumericalConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles the processing of chart data requests within the CRM reporting system.
 * Merges multiple chart request graph axes into a single aligned result by
 * unifying X-axis values across all requests and aligning corresponding Y-axis values.
 *
 * @author jamesh
 * @version 1.0
 * @see com.tlc.crm.core.api.analytics.converter.data.CRMReportsData
 */
class CRMChartData extends AbstractViewData {

    private CRMChartData() {
    }

    private static class Instance {
        private static final CRMChartData INSTANCE = new CRMChartData();
    }

    /**
     * Returns the singleton instance of {@link CRMChartData}.
     *
     * @return the singleton instance
     */
    public static CRMChartData INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * Merges multiple chart request graph axes into a unified result by:
     * <ol>
     *   <li>Collecting all unique X-axis values across all requests (preserving insertion order),
     *       capturing X-axis metadata, column maps, and per-request X→Y mappings in a single pass</li>
     *   <li>Aligning each Y-axis series to the complete unified X-axis
     *       (filling {@code null} for missing values)</li>
     * </ol>
     *
     * @param graphAxisMap a map where each key is a {@link ChartRequest} and each value
     *                     is a list of {@link GraphAxis} objects
     * @return a combined list containing the unified X-axis followed by all aligned Y-axis instances,
     *         or an empty list if any request has empty graph axes
     */
    @Override
    protected List<GraphAxis<Object>> mergeGraphAxis(final Map<ChartRequest, List<GraphAxis<Object>>> graphAxisMap) {
        if (graphAxisMap.isEmpty() || hasEmptyGraphAxes(graphAxisMap)) {
            return Collections.emptyList();
        }

        final Set<Object> uniqueXValues = new LinkedHashSet<>();
        final Map<Long, Object> mergedColumnMap = new HashMap<>();
        final Map<ReportField, Map<Object, Object>> yAxisXToYMaps = new LinkedHashMap<>();

        ReportField xAxisField = null;
        Long xRowIdx = null;

        for (final List<GraphAxis<Object>> graphAxes : graphAxisMap.values()) {
            final GraphAxis<Object> xAxis = graphAxes.get(NumericalConstant.ZERO);
            final List<AxisValue<Object>> xValues = xAxis.getAxisValues();
            final ReportField xReportField = xAxis.getReportField();

            if (xAxisField == null) {
                xAxisField = xReportField;
                xRowIdx = xAxis.getRowIdx();
            }

            for (final AxisValue<Object> xValue : xValues) {
                uniqueXValues.add(xValue.getValue());
            }

            if (xReportField.getAxis() == Axis.X_AXIS) {
                mergedColumnMap.putAll(xAxis.getColumnMap());
            }

            final int xValuesSize = xValues.size();
            final int graphAxesSize = graphAxes.size();

            for (int index = NumericalConstant.ONE; index < graphAxesSize; index++) {
                final GraphAxis<Object> yAxis = graphAxes.get(index);
                final List<AxisValue<Object>> yValues = yAxis.getAxisValues();

                final Map<Object, Object> xToYMap = new HashMap<>(xValuesSize);
                for (int i = 0; i < xValuesSize; i++) {
                    xToYMap.put(xValues.get(i).getValue(), yValues.get(i).getValue());
                }

                yAxisXToYMaps.put(yAxis.getReportField(), xToYMap);
            }
        }

        return buildMergedResult(xRowIdx, xAxisField, uniqueXValues, mergedColumnMap, yAxisXToYMaps);
    }

    /**
     * Checks whether any chart request in the map has an empty list of graph axes.
     * Uses a simple loop instead of stream to avoid allocation overhead.
     *
     * @param graphAxisMap the map of chart requests to their graph axes
     * @return {@code true} if any entry has an empty graph axes list
     */
    private boolean hasEmptyGraphAxes(final Map<ChartRequest, List<GraphAxis<Object>>> graphAxisMap) {
        for (final List<GraphAxis<Object>> axes : graphAxisMap.values()) {
            if (axes.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Assembles the final merged result list with the unified X-axis as the first element
     * followed by all aligned Y-axis entries. Converts the unique X-values set to an array
     * for cache-friendly iteration during Y-axis alignment.
     *
     * @param xRowIdx        the row index for the X-axis
     * @param xAxisField     the report field for the X-axis
     * @param uniqueXValues  the unified set of X-axis values
     * @param columnMap      the merged column map from all X-axis sources
     * @param yAxisXToYMaps  the map of report field to its X→Y value mapping
     * @return the merged list of graph axes
     */
    private List<GraphAxis<Object>> buildMergedResult(final Long xRowIdx,
                                                      final ReportField xAxisField,
                                                      final Set<Object> uniqueXValues,
                                                      final Map<Long, Object> columnMap,
                                                      final Map<ReportField, Map<Object, Object>> yAxisXToYMaps) {
        final Object[] xValuesArray = uniqueXValues.toArray();
        final List<GraphAxis<Object>> result = new ArrayList<>(1 + yAxisXToYMaps.size());

        final GraphAxis<Object> xAxis = GraphAxis.create(xRowIdx, xAxisField);
        for (final Object xValue : xValuesArray) {
            xAxis.getAxisValues().add(AxisValue.create(xValue));
        }
        xAxis.getColumnMap().putAll(columnMap);
        result.add(xAxis);

        for (final Map.Entry<ReportField, Map<Object, Object>> entry : yAxisXToYMaps.entrySet()) {
            final GraphAxis<Object> yAxis = GraphAxis.create(1L, entry.getKey());
            final Map<Object, Object> xToYMap = entry.getValue();

            for (final Object xValue : xValuesArray) {
                yAxis.getAxisValues().add(AxisValue.create(xToYMap.get(xValue)));
            }

            result.add(yAxis);
        }

        return result;
    }
}
