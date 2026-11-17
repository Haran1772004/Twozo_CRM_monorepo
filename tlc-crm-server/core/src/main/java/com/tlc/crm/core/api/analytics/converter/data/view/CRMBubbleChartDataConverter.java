package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;
import com.tlc.analytics.service.column.ColumnDetailsService;

import com.tlc.commons.Counter;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.analytics.converter.data.DisplayValueResolver;
import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunction;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.l10n.temporal.model.Temporal;
import lombok.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeMap;
import java.util.Date;
import java.util.Comparator;

/**
 * Converter implementation for generating Bubble Chart data in CRM analytics views.
 * Supports three layout configurations:
 * <ul>
 *   <li>Two metrics + two view-by fields</li>
 *   <li>Two metrics + one view-by field</li>
 *   <li>One metric + two view-by fields</li>
 * </ul>
 *
 * @author karthick V
 * @version 1.0
 */
public class CRMBubbleChartDataConverter extends AbstractViewDataConverter {
    private final DisplayValueResolver displayValueResolver;
    private final ColumnDetailsService columnDetailsService;

    /**
     * Common context record for bubble chart data generation.
     * Contains shared parameters used across all chart generation methods.
     *
     * @param orgId              the organization ID
     * @param graphMeta          the graph metadata JSON object
     * @param graphDataArray     the array to hold graph data
     * @param response           the response JSON object
     * @param primaryXAxisValues the primary X axis values
     * @param yAxisValues        the Y axis values
     * @param columnId           the column ID for display resolution
     * @param functionName       the time period function name
     * @param i18nResolver       resolver for localizing values
     */
    private record BubbleChartContext(Long orgId, JsonObject graphMeta, JsonArray graphDataArray, JsonObject response,
                                      List<AxisValue<Object>> primaryXAxisValues, List<AxisValue<Object>> yAxisValues,
                                      Long columnId, String functionName, I18nResolver i18nResolver) {
    }

    private CRMBubbleChartDataConverter() {
        this.displayValueResolver = DisplayValueResolver.INSTANCE();
        this.columnDetailsService = ColumnDetailsService.getInstance();
    }

    private static class Instance {
        private static final CRMBubbleChartDataConverter INSTANCE = new CRMBubbleChartDataConverter();
    }

    /**
     * Gets the singleton {@link CRMBubbleChartDataConverter} instance.
     *
     * @return the singleton instance
     */
    public static CRMBubbleChartDataConverter INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * Routes the chart data generation to the appropriate handler based on the
     * maximum X-axis and Y-axis order values found among the graph axes.
     *
     * @param orgId        the ID of the organization (must not be null)
     * @param graphAxis    the list of {@link GraphAxis} objects containing report field and axis details (must not be null)
     * @param crmUserMap   a map of CRM user IDs to their corresponding {@link CrmUser} objects (must not be null)
     * @param i18nResolver the internationalization resolver used for localizing output (must not be null)
     * @return a {@link JsonObject} containing the processed chart data
     */
    @Override
    protected JsonObject getData(final @NonNull Long orgId, final @NonNull List<GraphAxis<Object>> graphAxis,
                                 final @NonNull Map<Long, CrmUser> crmUserMap, final @NonNull I18nResolver i18nResolver) {

        //TODO Change the key of Map from Object.
        long maxXOrder = 0;
        long maxYOrder = 0;

        for (final GraphAxis<Object> axis : graphAxis) {
            final ReportField field = axis.getReportField();
            final Axis axisType = field.getAxis();
            final long order = field.getOrder();

            if (axisType == Axis.X_AXIS) {
                maxXOrder = Math.max(maxXOrder, order);
            } else {
                maxYOrder = Math.max(maxYOrder, order);
            }
        }

        if (maxXOrder == NumericalConstant.TWO && maxYOrder == NumericalConstant.TWO) {
            return getTwoMetricsAndTwoViewBy(orgId, graphAxis, crmUserMap, i18nResolver);
        }

        if (maxXOrder == NumericalConstant.TWO && maxYOrder == NumericalConstant.ONE) {
            return getOneMetricAndTwoViewBy(orgId, graphAxis, crmUserMap, i18nResolver);
        }

        return getTwoMetricsAndOneViewBy(orgId, graphAxis, crmUserMap, i18nResolver);
    }

    /**
     * Builds bubble chart data for configurations containing two metrics and two view-by fields.
     * <p>
     * Splits the collection into first (indices 0–2) and second (indices 3–5) groups,
     * cross-references X-axis values between them, and produces Y/R metric values with
     * zero-fill for unmatched entries.
     * </p>
     *
     * @param orgId        the organization ID
     * @param collection   chart axis definitions
     * @param crmUserMap   mapping of CRM user IDs to {@link CrmUser}
     * @param i18nResolver resolver for localizing values
     * @return a {@link JsonObject} containing the chart's meta and dataset values
     */
    public JsonObject getTwoMetricsAndTwoViewBy(final Long orgId,
                                                final List<GraphAxis<Object>> collection,
                                                final Map<Long, CrmUser> crmUserMap,
                                                final I18nResolver i18nResolver) {
        final int collectionSize = collection.size();
        final List<GraphAxis<Object>> firstGraphAxes = collection.subList(NumericalConstant.ZERO, Math.min(NumericalConstant.THREE, collectionSize));
        final List<GraphAxis<Object>> secondGraphAxes = collectionSize > NumericalConstant.THREE
                ? collection.subList(NumericalConstant.THREE, Math.min(NumericalConstant.SIX, collectionSize))
                : Collections.emptyList();
        final JsonObject response = Json.object();
        final JsonObject graphMeta = Json.object();
        final JsonArray graphDataArray = Json.array();
        final GraphAxis<Object> yAxis = firstGraphAxes.get(NumericalConstant.TWO);
        final GraphAxis<Object> rAxis = secondGraphAxes.get(NumericalConstant.TWO);
        final ReportField yReportField = yAxis.getReportField();
        final ReportField rReportField = rAxis.getReportField();
        final Long yReportFieldColumnId = yReportField.getColumnId();
        final Long rReportFieldColumnId = rReportField.getColumnId();

        graphMeta.put(CRMViewPropertyKeys.Y, getMetaInfo(orgId, yAxis));
        graphMeta.put(CRMViewPropertyKeys.R, getMetaInfo(orgId, rAxis));

        final GraphAxis<Object> primaryXAxis = findAxis(NumericalConstant.ONE, firstGraphAxes, Axis.X_AXIS);
        final GraphAxis<Object> dataSetXAxis = findAxis(NumericalConstant.TWO, firstGraphAxes, Axis.X_AXIS);
        final GraphAxis<Object> secondaryPrimaryXAxis = findAxis(NumericalConstant.ONE, secondGraphAxes, Axis.X_AXIS);
        final GraphAxis<Object> secondaryDataSetXAxis = findAxis(NumericalConstant.TWO, secondGraphAxes, Axis.X_AXIS);
        final ReportField primaryXReportField = primaryXAxis.getReportField();
        final ReportField dataSetReportField = dataSetXAxis.getReportField();
        final Long xReportFieldColumnId = primaryXReportField.getColumnId();
        final Long dataSetReportFieldColumnId = dataSetReportField.getColumnId();
        final String primaryXFunction = primaryXReportField.getFunctionName();
        final String dataSetFunction = dataSetReportField.getFunctionName();

        graphMeta.put(CRMViewPropertyKeys.X, getMetaInfo(orgId, primaryXAxis));
        graphMeta.put(CRMViewPropertyKeys.DATA_SET, getMetaInfo(orgId, dataSetXAxis));

        final List<AxisValue<Object>> primaryXAxisValues = primaryXAxis.getAxisValues();
        final List<AxisValue<Object>> yAxisValues = yAxis.getAxisValues();
        final List<AxisValue<Object>> rAxisValues = rAxis.getAxisValues();
        final List<AxisValue<Object>> secondaryDataSetValues = secondaryDataSetXAxis.getAxisValues();
        final List<AxisValue<Object>> secondaryPrimaryXAxisValues = secondaryPrimaryXAxis.getAxisValues();
        final List<AxisValue<Object>> primaryDataSetValues = dataSetXAxis.getAxisValues();

        if (Objects.nonNull(primaryXFunction) && TimePeriodFunction.get(primaryXFunction) != null) {
            return getTwoMetricsAndTwoViewByWithXAxisTimePeriod(
                    new BubbleChartContext(orgId, graphMeta, graphDataArray, response, primaryXAxisValues, yAxisValues,
                            dataSetReportFieldColumnId, primaryXFunction, i18nResolver),
                    dataSetXAxis, primaryDataSetValues, rAxisValues, secondaryPrimaryXAxisValues, secondaryDataSetValues, crmUserMap);
        }

        if (Objects.nonNull(dataSetFunction) && TimePeriodFunction.get(dataSetFunction) != null) {
            return getTwoMetricsAndTwoViewByWithDataSetTimePeriod(
                    new BubbleChartContext(orgId, graphMeta, graphDataArray, response, primaryXAxisValues, yAxisValues,
                            xReportFieldColumnId, dataSetFunction, i18nResolver),
                    primaryXAxis, primaryDataSetValues, rAxisValues, secondaryPrimaryXAxisValues, secondaryDataSetValues, crmUserMap);
        }

        final int secondaryDataSetSize = secondaryDataSetValues.size();
        final Map<Object, Integer> secondaryIndexMap = new HashMap<>(secondaryDataSetSize);

        for (int index = NumericalConstant.ZERO; index < secondaryDataSetSize; index++) {
            final AxisValue<Object> axisValue = secondaryDataSetValues.get(index);

            if (axisValue != null) {
                secondaryIndexMap.put(normalizeKeyValue(axisValue.getValue()), index);
            }
        }

        final int dataSetCount = primaryDataSetValues.size();
        final Set<Object> processedDataSets = new HashSet<>(dataSetCount);

        for (int index = NumericalConstant.ZERO; index < dataSetCount; index++) {
            final AxisValue<Object> primaryDataSetAxisValue = primaryDataSetValues.get(index);
            final Object axisValue = Objects.nonNull(primaryDataSetAxisValue) ? primaryDataSetAxisValue.getValue() : null;
            final Object normalizedAxisValue = normalizeKeyValue(axisValue);
            final JsonObject graphDataJson = Json.object();
            final JsonArray valueArray = Json.array();

            graphDataJson.put(CRMViewPropertyKeys.DATA_SET, displayValueResolver.resolveDisplayValue(
                    orgId, getUserValue(axisValue, dataSetXAxis, crmUserMap), dataSetReportFieldColumnId, i18nResolver));
            processedDataSets.add(normalizedAxisValue);

            final AxisValue<Object> primaryXAxisValue = primaryXAxisValues.get(index);
            final Object xValue = Objects.nonNull(primaryXAxisValue) ? primaryXAxisValue.getValue() : null;
            final AxisValue<Object> yValue = yAxisValues.get(index);
            final Object resolvedXValue = getUserValue(xValue, primaryXAxis, crmUserMap);
            final Integer secondaryIndex = secondaryIndexMap.get(normalizedAxisValue);
            final JsonObject valueJson = Json.object();

            valueJson.put(CRMViewPropertyKeys.X, displayValueResolver.resolveDisplayValue(
                    orgId, resolvedXValue, xReportFieldColumnId, i18nResolver));
            valueJson.put(CRMViewPropertyKeys.Y, displayValueResolver.resolveDisplayValue(
                    orgId, getValueOrZero(Objects.nonNull(yValue) ? yValue.getValue() : null), yReportFieldColumnId, i18nResolver));

            if (secondaryIndex == null) {
                valueJson.put(CRMViewPropertyKeys.R, NumericalConstant.ZERO);
                valueArray.put(valueJson);
            } else {
                final AxisValue<Object> secondaryPrimaryXAxisValue = secondaryPrimaryXAxisValues.get(secondaryIndex);
                final Object secondaryXValue = Objects.nonNull(secondaryPrimaryXAxisValue) ? secondaryPrimaryXAxisValue.getValue() : null;
                final AxisValue<Object> rAxisValue = rAxisValues.get(secondaryIndex);
                final Object rValue = getValueOrZero(Objects.nonNull(rAxisValue) ? rAxisValue.getValue() : null);

                if (Objects.equals(normalizeKeyValue(resolvedXValue), normalizeKeyValue(secondaryXValue))) {
                    valueJson.put(CRMViewPropertyKeys.R, displayValueResolver.resolveDisplayValue(
                            orgId, rValue, rReportFieldColumnId, i18nResolver));
                    valueArray.put(valueJson);
                } else {
                    valueJson.put(CRMViewPropertyKeys.R, NumericalConstant.ZERO);

                    final JsonObject secondValueJson = Json.object();

                    secondValueJson.put(CRMViewPropertyKeys.X, displayValueResolver.resolveDisplayValue(
                            orgId, getUserValue(secondaryXValue, primaryXAxis, crmUserMap),
                            xReportFieldColumnId, i18nResolver));
                    secondValueJson.put(CRMViewPropertyKeys.Y, NumericalConstant.ZERO);
                    secondValueJson.put(CRMViewPropertyKeys.R, displayValueResolver.resolveDisplayValue(
                            orgId, rValue, rReportFieldColumnId, i18nResolver));
                    valueArray.put(valueJson);
                    valueArray.put(secondValueJson);
                }
            }

            graphDataJson.put(CRMViewPropertyKeys.VALUES, valueArray);
            graphDataArray.put(graphDataJson);
        }

        for (int index = NumericalConstant.ZERO; index < secondaryDataSetSize; index++) {
            final AxisValue<Object> secondaryDataSetAxisValue = secondaryDataSetValues.get(index);
            final Object axisValue = Objects.nonNull(secondaryDataSetAxisValue) ? secondaryDataSetAxisValue.getValue() : null;

            if (!processedDataSets.contains(normalizeKeyValue(axisValue))) {
                final JsonObject graphDataJson = Json.object();
                final JsonArray valueArray = Json.array();
                final JsonObject valueJson = Json.object();

                graphDataJson.put(CRMViewPropertyKeys.DATA_SET, displayValueResolver.resolveDisplayValue(
                        orgId, getUserValue(axisValue, dataSetXAxis, crmUserMap),
                        dataSetReportFieldColumnId, i18nResolver));

                final AxisValue<Object> secondaryPrimaryXAxisValue = secondaryPrimaryXAxisValues.get(index);

                valueJson.put(CRMViewPropertyKeys.X, displayValueResolver.resolveDisplayValue(
                        orgId, getUserValue(Objects.nonNull(secondaryPrimaryXAxisValue) ? secondaryPrimaryXAxisValue.getValue() : null, primaryXAxis, crmUserMap),
                        xReportFieldColumnId, i18nResolver));
                valueJson.put(CRMViewPropertyKeys.Y, NumericalConstant.ZERO);

                final AxisValue<Object> rAxisValue = rAxisValues.get(index);

                valueJson.put(CRMViewPropertyKeys.R, getValueOrZero(Objects.nonNull(rAxisValue) ? rAxisValue.getValue() : null));
                valueArray.put(valueJson);
                graphDataJson.put(CRMViewPropertyKeys.VALUES, valueArray);
                graphDataArray.put(graphDataJson);
            }
        }

        return buildFinalResponse(response, graphMeta, graphDataArray);
    }

    /**
     * Handles bubble chart data generation when the primary X-axis has a time period function.
     * Populates X-axis values with time-formatted data grouped by dataSet.
     *
     * @param context                     the common context for chart data generation
     * @param axisForDisplay              the axis used for display value resolution
     * @param primaryDataSetValues        the primary dataSet values
     * @param rAxisValues                 the R axis values
     * @param secondaryPrimaryXAxisValues the secondary primary X axis values
     * @param secondaryDataSetValues      the secondary dataSet values
     * @param crmUserMap                  mapping of CRM user IDs to users
     * @return a JsonObject containing the chart's meta and dataset values
     */
    private JsonObject getTwoMetricsAndTwoViewByWithXAxisTimePeriod(final BubbleChartContext context,
                                                                    final GraphAxis<Object> axisForDisplay,
                                                                    final List<AxisValue<Object>> primaryDataSetValues,
                                                                    final List<AxisValue<Object>> rAxisValues,
                                                                    final List<AxisValue<Object>> secondaryPrimaryXAxisValues,
                                                                    final List<AxisValue<Object>> secondaryDataSetValues,
                                                                    final Map<Long, CrmUser> crmUserMap) {
        final String functionName = context.functionName();
        final List<AxisValue<Object>> primaryXAxisValues = context.primaryXAxisValues();
        final List<AxisValue<Object>> yAxisValues = context.yAxisValues();
        final I18nResolver i18nResolver = context.i18nResolver();
        final JsonArray graphDataArray = context.graphDataArray();
        final TimePeriodFunction timePeriodFunction = TimePeriodFunction.get(functionName);
        final List<Object> xAxisList = new ArrayList<>();
        final Map<Object, Map<Integer, Object>> yXAxisDataMapByDataSet = new LinkedHashMap<>();
        final Map<Object, Map<Integer, Object>> rXAxisDataMapByDataSet = new LinkedHashMap<>();
        final Map<Object, List<AxisValue<Object>>> yAxisValuesByDataSet = new LinkedHashMap<>();
        final Map<Object, List<AxisValue<Object>>> rAxisValuesByDataSet = new LinkedHashMap<>();
        final Set<Object> allDataSets = new LinkedHashSet<>();
        final int primaryXAxisSize = primaryXAxisValues.size();

        populateAxisDataMapByKey(primaryDataSetValues, primaryXAxisValues, yAxisValues,
                allDataSets, yXAxisDataMapByDataSet, yAxisValuesByDataSet);
        populateAxisDataMapByKey(secondaryDataSetValues, secondaryPrimaryXAxisValues, rAxisValues,
                allDataSets, rXAxisDataMapByDataSet, rAxisValuesByDataSet);

        final Map<Integer, Object> combinedXAxisDataMap = new LinkedHashMap<>();
        final Counter counter = new Counter(NumericalConstant.ZERO);

        for (int index = NumericalConstant.ZERO; index < primaryXAxisSize; index++) {
            final AxisValue<Object> primaryXAxisValue = primaryXAxisValues.get(index);
            final Object xValue = Objects.nonNull(primaryXAxisValue) ? primaryXAxisValue.getValue() : null;

            if (Objects.nonNull(xValue)) {
                combinedXAxisDataMap.put(counter.getAndIncrement(), xValue);
            }
        }

        populateXAxisByLabel(functionName, yAxisValues, combinedXAxisDataMap, xAxisList, i18nResolver);

        for (final Object dataSetKey : allDataSets) {
            final JsonObject graphDataJson = Json.object();
            final JsonArray valueArray = Json.array();

            graphDataJson.put(CRMViewPropertyKeys.DATA_SET, displayValueResolver.resolveDisplayValue(
                    context.orgId(), getUserValue(findOriginalValue(dataSetKey, primaryDataSetValues, secondaryDataSetValues),
                            axisForDisplay, crmUserMap), context.columnId(), i18nResolver));

            final Map<Object, Object> yValueMap = new LinkedHashMap<>();
            final Map<Object, Object> rValueMap = new LinkedHashMap<>();

            if (Objects.nonNull(timePeriodFunction)) {
                timePeriodFunction.populate(yAxisValuesByDataSet.getOrDefault(dataSetKey, Collections.emptyList()),
                        yXAxisDataMapByDataSet.getOrDefault(dataSetKey, Collections.emptyMap()), yValueMap, i18nResolver);
                timePeriodFunction.populate(rAxisValuesByDataSet.getOrDefault(dataSetKey, Collections.emptyList()),
                        rXAxisDataMapByDataSet.getOrDefault(dataSetKey, Collections.emptyMap()), rValueMap, i18nResolver);
            }

            for (final Object xAxisValue : xAxisList) {
                final JsonObject valueJson = Json.object();
                final Object yValue = yValueMap.getOrDefault(xAxisValue, NumericalConstant.ZERO);
                final Object rValue = rValueMap.getOrDefault(xAxisValue, NumericalConstant.ZERO);

                valueJson.put(CRMViewPropertyKeys.X, xAxisValue);
                valueJson.put(CRMViewPropertyKeys.Y, getValueOrZero(yValue));
                valueJson.put(CRMViewPropertyKeys.R, getValueOrZero(rValue));
                valueArray.put(valueJson);
            }

            graphDataJson.put(CRMViewPropertyKeys.VALUES, valueArray);
            graphDataArray.put(graphDataJson);
        }

        return buildFinalResponse(context.response(), context.graphMeta(), graphDataArray);
    }

    /**
     * Handles bubble chart data generation when the dataSet X-axis has a time period function.
     * Populates dataSet values with time-formatted data grouped by primary X value.
     *
     * @param context                     the common context for chart data generation
     * @param axisForDisplay              the axis used for display value resolution
     * @param primaryDataSetValues        the primary dataSet values
     * @param rAxisValues                 the R axis values
     * @param secondaryPrimaryXAxisValues the secondary primary X axis values
     * @param secondaryDataSetValues      the secondary dataSet values
     * @param crmUserMap                  mapping of CRM user IDs to users
     * @return a JsonObject containing the chart's meta and dataset values
     */
    private JsonObject getTwoMetricsAndTwoViewByWithDataSetTimePeriod(final BubbleChartContext context,
                                                                      final GraphAxis<Object> axisForDisplay,
                                                                      final List<AxisValue<Object>> primaryDataSetValues,
                                                                      final List<AxisValue<Object>> rAxisValues,
                                                                      final List<AxisValue<Object>> secondaryPrimaryXAxisValues,
                                                                      final List<AxisValue<Object>> secondaryDataSetValues,
                                                                      final Map<Long, CrmUser> crmUserMap) {
        final String functionName = context.functionName();
        final List<AxisValue<Object>> primaryXAxisValues = context.primaryXAxisValues();
        final List<AxisValue<Object>> yAxisValues = context.yAxisValues();
        final I18nResolver i18nResolver = context.i18nResolver();
        final JsonArray graphDataArray = context.graphDataArray();
        final TimePeriodFunction timePeriodFunction = TimePeriodFunction.get(functionName);
        final List<Object> dataSetList = new ArrayList<>();
        final Map<Object, Map<Integer, Object>> yDataSetMapByX = new LinkedHashMap<>();
        final Map<Object, Map<Integer, Object>> rDataSetMapByX = new LinkedHashMap<>();
        final Map<Object, List<AxisValue<Object>>> yAxisValuesByX = new LinkedHashMap<>();
        final Map<Object, List<AxisValue<Object>>> rAxisValuesByX = new LinkedHashMap<>();
        final Set<Object> allXValues = new LinkedHashSet<>();
        final int primaryDataSetSize = primaryDataSetValues.size();

        populateAxisDataMapByKey(primaryXAxisValues, primaryDataSetValues, yAxisValues,
                allXValues, yDataSetMapByX, yAxisValuesByX);
        populateAxisDataMapByKey(secondaryPrimaryXAxisValues, secondaryDataSetValues, rAxisValues,
                allXValues, rDataSetMapByX, rAxisValuesByX);

        final Map<Integer, Object> combinedDataSetDataMap = new LinkedHashMap<>();
        final Counter counter = new Counter(NumericalConstant.ZERO);

        for (int index = NumericalConstant.ZERO; index < primaryDataSetSize; index++) {
            final AxisValue<Object> primaryDataSetAxisValue = primaryDataSetValues.get(index);
            final Object dataSetValue = Objects.nonNull(primaryDataSetAxisValue) ? primaryDataSetAxisValue.getValue() : null;

            if (Objects.nonNull(dataSetValue)) {
                combinedDataSetDataMap.put(counter.getAndIncrement(), dataSetValue);
            }
        }

        populateXAxisByLabel(functionName, yAxisValues, combinedDataSetDataMap, dataSetList, i18nResolver);

        for (final Object dataSetKey : dataSetList) {
            final JsonObject graphDataJson = Json.object();
            final JsonArray valueArray = Json.array();

            graphDataJson.put(CRMViewPropertyKeys.DATA_SET, dataSetKey);

            for (final Object xAxisValue: allXValues) {
                final JsonObject valueJson = Json.object();
                final Map<Object, Object> yValueMap = new LinkedHashMap<>();
                final Map<Object, Object> rValueMap = new LinkedHashMap<>();

                if (Objects.nonNull(timePeriodFunction)) {
                    timePeriodFunction.populate(yAxisValuesByX.getOrDefault(xAxisValue, Collections.emptyList()),
                            yDataSetMapByX.getOrDefault(xAxisValue, Collections.emptyMap()), yValueMap, i18nResolver);
                    timePeriodFunction.populate(rAxisValuesByX.getOrDefault(xAxisValue, Collections.emptyList()),
                            rDataSetMapByX.getOrDefault(xAxisValue, Collections.emptyMap()), rValueMap, i18nResolver);
                }

                final Object yValue = yValueMap.getOrDefault(dataSetKey, NumericalConstant.ZERO);
                final Object rValue = rValueMap.getOrDefault(dataSetKey, NumericalConstant.ZERO);

                valueJson.put(CRMViewPropertyKeys.X, displayValueResolver.resolveDisplayValue(
                        context.orgId(), getUserValue(findOriginalValue(xAxisValue, primaryXAxisValues, secondaryPrimaryXAxisValues),
                                axisForDisplay, crmUserMap), context.columnId(), i18nResolver));
                valueJson.put(CRMViewPropertyKeys.Y, getValueOrZero(yValue));
                valueJson.put(CRMViewPropertyKeys.R, getValueOrZero(rValue));
                valueArray.put(valueJson);
            }

            graphDataJson.put(CRMViewPropertyKeys.VALUES, valueArray);
            graphDataArray.put(graphDataJson);
        }

        return buildFinalResponse(context.response(), context.graphMeta(), graphDataArray);
    }

    /**
     * Finds the original value from axis value lists given a normalized key.
     * Searches the primary list first, then the secondary list if not found.
     *
     * @param normalizedKey   the normalized key to search for
     * @param primaryValues   the primary axis values to search
     * @param secondaryValues the secondary axis values to search (can be empty)
     * @return the original value, or the normalized key if not found
     */
    private Object findOriginalValue(final Object normalizedKey,
                                     final List<AxisValue<Object>> primaryValues,
                                     final List<AxisValue<Object>> secondaryValues) {
        for (final AxisValue<Object> axisValue : primaryValues) {
            if (Objects.isNull(axisValue)) {
                continue;
            }
            final Object primaryValue = axisValue.getValue();

            if (Objects.equals(normalizeKeyValue(primaryValue), normalizedKey)) {
                return primaryValue;
            }
        }

        for (final AxisValue<Object> axisValue : secondaryValues) {
            if (Objects.isNull(axisValue)) {
                continue;
            }
            final Object secondaryValue = axisValue.getValue();

            if (Objects.equals(normalizeKeyValue(secondaryValue), normalizedKey)) {
                return secondaryValue;
            }
        }

        return normalizedKey;
    }

    /**
     * Builds the final response JSON with graph meta and data.
     *
     * @param response       the response JSON object to populate
     * @param graphMeta      the graph metadata
     * @param graphDataArray the graph data array
     * @return the populated response object
     */
    private JsonObject buildFinalResponse(final JsonObject response,
                                          final JsonObject graphMeta,
                                          final JsonArray graphDataArray) {
        response.put(CRMViewPropertyKeys.GRAPH_META, graphMeta);
        response.put(CRMViewPropertyKeys.DATA, Json.object().put(CRMViewPropertyKeys.GRAPH_DATA, graphDataArray));
        return response;
    }

    /**
     * Returns the value or zero if null.
     * Handles both direct null values and AxisValue wrappers containing null values.
     *
     * @param value the value to check
     * @return the value if non-null, otherwise zero
     */
    private Object getValueOrZero(final Object value) {
        if (Objects.isNull(value)) {
            return NumericalConstant.ZERO;
        }

        if (value instanceof AxisValue<?> axisValue) {
            final Object innerValue = axisValue.getValue();
            return Objects.nonNull(innerValue) ? innerValue : NumericalConstant.ZERO;
        }

        return value;
    }

    /**
     * Builds bubble chart data for configurations containing one metric and two view-by fields.
     * <p>
     * Groups data by dataSet values and builds x/y pairs for each group.
     * If either the primary X-axis or the dataSet X-axis has a time period function,
     * the corresponding axis values will be populated with time-formatted data.
     * </p>
     *
     * @param orgId        the organization ID
     * @param collection   chart axis definitions
     * @param crmUserMap   mapping of CRM user IDs to {@link CrmUser}
     * @param i18nResolver resolver for localizing values
     * @return a {@link JsonObject} containing the chart's meta and dataset values
     */
    public JsonObject getOneMetricAndTwoViewBy(final Long orgId,
                                               final List<GraphAxis<Object>> collection,
                                               final Map<Long, CrmUser> crmUserMap,
                                               final I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonObject graphMeta = Json.object();
        final JsonArray graphDataArray = Json.array();
        final GraphAxis<Object> primaryXAxis = findAxis(NumericalConstant.ONE, collection, Axis.X_AXIS);
        final GraphAxis<Object> dataSetXAxis = findAxis(NumericalConstant.TWO, collection, Axis.X_AXIS);
        final GraphAxis<Object> yAxis = findAxis(NumericalConstant.ONE, collection, Axis.Y_AXIS);
        final ReportField primaryXReportField = primaryXAxis.getReportField();
        final ReportField dataSetReportField = dataSetXAxis.getReportField();
        final Long xReportFieldColumnId = primaryXReportField.getColumnId();
        final Long dataSetReportFieldColumnId = dataSetReportField.getColumnId();
        final String primaryXFunction = primaryXReportField.getFunctionName();
        final String dataSetFunction = dataSetReportField.getFunctionName();

        graphMeta.put(CRMViewPropertyKeys.Y, getMetaInfo(orgId, yAxis));
        graphMeta.put(CRMViewPropertyKeys.X, getMetaInfo(orgId, primaryXAxis));
        graphMeta.put(CRMViewPropertyKeys.DATA_SET, getMetaInfo(orgId, dataSetXAxis));

        final List<AxisValue<Object>> primaryXAxisValues = primaryXAxis.getAxisValues();
        final List<AxisValue<Object>> yAxisValues = yAxis.getAxisValues();
        final List<AxisValue<Object>> primaryDataSetValues = dataSetXAxis.getAxisValues();

        if (Objects.isNull(primaryXAxisValues) || primaryXAxisValues.isEmpty() && Objects.isNull(primaryDataSetValues) || primaryDataSetValues.isEmpty()) {
            return buildFinalResponse(response, graphMeta, graphDataArray);
        }

        if (Objects.nonNull(primaryXFunction) && Objects.nonNull(TimePeriodFunction.get(primaryXFunction))) {
            return getOneMetricAndTwoViewByWithXAxisTimePeriod(
                    new BubbleChartContext(orgId, graphMeta, graphDataArray, response, primaryXAxisValues, yAxisValues,
                            xReportFieldColumnId, primaryXFunction, i18nResolver),
                    dataSetXAxis, primaryDataSetValues, crmUserMap);
        }

        if (Objects.nonNull(dataSetFunction) && Objects.nonNull(TimePeriodFunction.get(dataSetFunction))) {
            return getOneMetricAndTwoViewByWithDataSetTimePeriod(
                    new BubbleChartContext(orgId, graphMeta, graphDataArray, response, primaryXAxisValues, yAxisValues,
                            xReportFieldColumnId, dataSetFunction, i18nResolver),
                    primaryXAxis, primaryDataSetValues, crmUserMap);
        }

        final Map<Object, List<Integer>> xDataSetMap = new LinkedHashMap<>();
        final int dataSetCount = primaryDataSetValues.size();

        for (int index = NumericalConstant.ZERO; index < dataSetCount; index++) {
            final AxisValue<Object> primaryDataSetAxisValue = primaryDataSetValues.get(index);
            xDataSetMap.computeIfAbsent(normalizeKeyValue(Objects.nonNull(primaryDataSetAxisValue) ? primaryDataSetAxisValue.getValue() : null),
                    valuesList -> new ArrayList<>()).add(index);
        }

        for (final Map.Entry<Object, List<Integer>> entry : xDataSetMap.entrySet()) {
            final List<Integer> xAxisIndexList = entry.getValue();
            final JsonObject graphDataJson = Json.object();
            final JsonArray valueArray = Json.array();

            final AxisValue<Object> dataSetAxisValue = primaryDataSetValues.get(xAxisIndexList.get(NumericalConstant.ZERO));
            graphDataJson.put(CRMViewPropertyKeys.DATA_SET, resolveAxisDisplayValue(orgId, dataSetAxisValue.getValue(),
                    dataSetXAxis, crmUserMap, dataSetReportFieldColumnId, displayValueResolver, i18nResolver));

            for (final Integer xAxisIndex : xAxisIndexList) {
                final JsonObject valueJson = Json.object();
                final AxisValue<Object> yAxisValue = yAxisValues.get(xAxisIndex);
                final Object yValue = Objects.nonNull(yAxisValue) ? yAxisValue.getValue() : NumericalConstant.ZERO;

                final AxisValue<Object> primaryXAxisValue = primaryXAxisValues.get(xAxisIndex);
                valueJson.put(CRMViewPropertyKeys.X, resolveAxisDisplayValue(orgId, primaryXAxisValue.getValue(), primaryXAxis, crmUserMap, xReportFieldColumnId, displayValueResolver, i18nResolver));
                valueJson.put(CRMViewPropertyKeys.Y, getValueOrZero(yValue));
                valueArray.put(valueJson);
            }

            graphDataJson.put(CRMViewPropertyKeys.VALUES, valueArray);
            graphDataArray.put(graphDataJson);
        }

        return buildFinalResponse(response, graphMeta, graphDataArray);
    }

    /**
     * Handles bubble chart data generation for one metric and two view-by when the primary X-axis has a time period function.
     * Populates X-axis values with time-formatted data grouped by dataSet.
     *
     * @param context              the common context for chart data generation
     * @param dataSetXAxis         the dataSet X axis graph definition
     * @param primaryDataSetValues the primary dataSet values
     * @param crmUserMap           mapping of CRM user IDs to users
     * @return a JsonObject containing the chart's meta and dataset values
     */
    private JsonObject getOneMetricAndTwoViewByWithXAxisTimePeriod(final BubbleChartContext context,
                                                                   final GraphAxis<Object> dataSetXAxis,
                                                                   final List<AxisValue<Object>> primaryDataSetValues,
                                                                   final Map<Long, CrmUser> crmUserMap) {
        final String functionName = context.functionName();
        final List<AxisValue<Object>> primaryXAxisValues = context.primaryXAxisValues();
        final List<AxisValue<Object>> yAxisValues = context.yAxisValues();
        final I18nResolver i18nResolver = context.i18nResolver();
        final JsonArray graphDataArray = context.graphDataArray();
        final TimePeriodFunction timePeriodFunction = TimePeriodFunction.get(functionName);
        final List<Object> xAxisList = new ArrayList<>();
        final Map<Object, Map<Integer, Object>> xAxisDataMapByDataSet = new LinkedHashMap<>();
        final Map<Object, List<AxisValue<Object>>> yAxisValuesByDataSet = new LinkedHashMap<>();
        final Set<Object> allDataSets = new LinkedHashSet<>();

        populateAxisDataMapByKeyWithOptionalMapping(primaryDataSetValues, primaryXAxisValues,
                yAxisValues, allDataSets, xAxisDataMapByDataSet, yAxisValuesByDataSet);

        final Map<Integer, Object> combinedXAxisDataMap = buildCombinedDataMap(primaryXAxisValues);

        populateXAxisByLabel(functionName, yAxisValues, combinedXAxisDataMap, xAxisList, i18nResolver);

        for (final Object dataSetKey : allDataSets) {
            final JsonObject graphDataJson = Json.object();
            final JsonArray valueArray = Json.array();

            graphDataJson.put(CRMViewPropertyKeys.DATA_SET, displayValueResolver.resolveDisplayValue(
                    context.orgId(), getUserValue(findOriginalValue(dataSetKey, primaryDataSetValues, Collections.emptyList()),
                            dataSetXAxis, crmUserMap), context.columnId(), i18nResolver));

            final Map<Object, Object> yValueMap = new LinkedHashMap<>();

            if (Objects.nonNull(timePeriodFunction)) {
                timePeriodFunction.populate(yAxisValuesByDataSet.getOrDefault(dataSetKey, Collections.emptyList()),
                        xAxisDataMapByDataSet.getOrDefault(dataSetKey, Collections.emptyMap()), yValueMap, i18nResolver);
            }

            for (final Object xAxisValue : xAxisList) {
                final JsonObject valueJson = Json.object();
                final Object yValue = yValueMap.getOrDefault(xAxisValue, NumericalConstant.ZERO);

                valueJson.put(CRMViewPropertyKeys.X, xAxisValue);
                valueJson.put(CRMViewPropertyKeys.Y, getValueOrZero(yValue));
                valueArray.put(valueJson);
            }

            graphDataJson.put(CRMViewPropertyKeys.VALUES, valueArray);
            graphDataArray.put(graphDataJson);
        }

        return buildFinalResponse(context.response(), context.graphMeta(), graphDataArray);
    }

    /**
     * Handles bubble chart data generation for one metric and two view-by when the dataSet X-axis has a time period function.
     * Populates dataSet values with time-formatted data grouped by primary X value.
     *
     * @param context              the common context for chart data generation
     * @param primaryXAxis         the primary X axis graph definition
     * @param primaryDataSetValues the primary dataSet values
     * @param crmUserMap           mapping of CRM user IDs to users
     * @return a JsonObject containing the chart's meta and dataset values
     */
    private JsonObject getOneMetricAndTwoViewByWithDataSetTimePeriod(final BubbleChartContext context,
                                                                     final GraphAxis<Object> primaryXAxis,
                                                                     final List<AxisValue<Object>> primaryDataSetValues,
                                                                     final Map<Long, CrmUser> crmUserMap) {
        final String functionName = context.functionName();
        final List<AxisValue<Object>> primaryXAxisValues = context.primaryXAxisValues();
        final List<AxisValue<Object>> yAxisValues = context.yAxisValues();
        final I18nResolver i18nResolver = context.i18nResolver();
        final JsonArray graphDataArray = context.graphDataArray();
        final TimePeriodFunction timePeriodFunction = TimePeriodFunction.get(functionName);
        final List<Object> dataSetList = new ArrayList<>();
        final Map<Object, Map<Integer, Object>> dataSetMapByX = new LinkedHashMap<>();
        final Map<Object, List<AxisValue<Object>>> yAxisValuesByX = new LinkedHashMap<>();
        final Set<Object> allXValues = new LinkedHashSet<>();

        populateAxisDataMapByKeyWithOptionalMapping(primaryXAxisValues, primaryDataSetValues,
                yAxisValues, allXValues, dataSetMapByX, yAxisValuesByX);

        final Map<Integer, Object> combinedDataSetDataMap = buildCombinedDataMap(primaryDataSetValues);

        populateXAxisByLabel(functionName, yAxisValues, combinedDataSetDataMap, dataSetList, i18nResolver);

        for (final Object dataSetKey : dataSetList) {
            final JsonObject graphDataJson = Json.object();
            final JsonArray valueArray = Json.array();

            graphDataJson.put(CRMViewPropertyKeys.DATA_SET, dataSetKey);

            for (final Object xAxisValue : allXValues) {
                final JsonObject valueJson = Json.object();
                final Map<Object, Object> yValueMap = new LinkedHashMap<>();

                if (Objects.nonNull(timePeriodFunction)) {
                    timePeriodFunction.populate(yAxisValuesByX.getOrDefault(xAxisValue, Collections.emptyList()),
                            dataSetMapByX.getOrDefault(xAxisValue, Collections.emptyMap()), yValueMap, i18nResolver);
                }

                final Object yValue = yValueMap.getOrDefault(dataSetKey, NumericalConstant.ZERO);

                valueJson.put(CRMViewPropertyKeys.X, displayValueResolver.resolveDisplayValue(
                        context.orgId(), getUserValue(findOriginalValue(xAxisValue, primaryXAxisValues, Collections.emptyList()),
                                primaryXAxis, crmUserMap), context.columnId(), i18nResolver));
                valueJson.put(CRMViewPropertyKeys.Y, getValueOrZero(yValue));
                valueArray.put(valueJson);
            }

            graphDataJson.put(CRMViewPropertyKeys.VALUES, valueArray);
            graphDataArray.put(graphDataJson);
        }

        return buildFinalResponse(context.response(), context.graphMeta(), graphDataArray);
    }

    /**
     * Builds bubble chart data for configurations containing two metrics but only one view-by field.
     * <p>
     * Cross-references the first metrics group's X-axis values with the second group's using
     * a pre-built index map for O(1) lookups, and directly processes unmatched entries without
     * building an intermediate index list.
     * </p>
     *
     * @param orgId        the organization ID
     * @param collection   chart axis definitions
     * @param crmUserMap   mapping of CRM user IDs to {@link CrmUser}
     * @param i18nResolver resolver for localizing values
     * @return a {@link JsonObject} containing the chart's meta and dataset values
     */
    public JsonObject getTwoMetricsAndOneViewBy(final Long orgId, final List<GraphAxis<Object>> collection,
                                                final Map<Long, CrmUser> crmUserMap, final I18nResolver i18nResolver) {
        final int collectionSize = collection.size();
        final List<GraphAxis<Object>> firstList = collection.subList(NumericalConstant.ZERO, Math.min(NumericalConstant.TWO, collectionSize));
        final List<GraphAxis<Object>> secondList = collectionSize > NumericalConstant.THREE
                ? collection.subList(NumericalConstant.TWO, NumericalConstant.FOUR)
                : Collections.emptyList();
        final JsonObject response = Json.object();
        final JsonObject graphMeta = Json.object();
        final JsonArray graphDataArray = Json.array();
        final GraphAxis<Object> yGraphAxis = firstList.get(NumericalConstant.ONE);
        final GraphAxis<Object> rGraphAxis = secondList.get(NumericalConstant.ONE);
        final GraphAxis<Object> firstXAxis = firstList.get(NumericalConstant.ZERO);
        final GraphAxis<Object> secondXAxis = secondList.get(NumericalConstant.ZERO);
        final List<AxisValue<Object>> firstMetricsXAxisValues = firstXAxis.getAxisValues();
        final List<AxisValue<Object>> secondMetricsXAxisValues = secondXAxis.getAxisValues();
        final List<AxisValue<Object>> yGraphAxisValues = yGraphAxis.getAxisValues();
        final List<AxisValue<Object>> rGraphAxisValues = rGraphAxis.getAxisValues();
        final boolean isFirstAxisValuesEmpty = Objects.isNull(firstMetricsXAxisValues) || firstMetricsXAxisValues.isEmpty()
                || firstMetricsXAxisValues.stream().allMatch(axisValue -> Objects.isNull(axisValue)
                || Objects.isNull(axisValue.getValue()) || Objects.equals(axisValue.getValue(), NumericalConstant.ZERO));
        final boolean isSecondAxisValuesEmpty = Objects.isNull(secondMetricsXAxisValues) || secondMetricsXAxisValues.isEmpty()
                || secondMetricsXAxisValues.stream().allMatch(axisValue -> Objects.isNull(axisValue)
                || Objects.isNull(axisValue.getValue()) || Objects.equals(axisValue.getValue(), NumericalConstant.ZERO));
        final ReportField firstXAxisReportField = firstXAxis.getReportField();
        final Long columnId = firstXAxisReportField.getColumnId();
        final String xAxisFunction = firstXAxisReportField.getFunctionName();
        final int firstMetricXAxisValuesSize = firstMetricsXAxisValues.size();
        final int secondMetricsXAxisValuesSize = secondMetricsXAxisValues.size();

        graphMeta.put(CRMViewPropertyKeys.Y, getMetaInfo(orgId, yGraphAxis));
        graphMeta.put(CRMViewPropertyKeys.R, getMetaInfo(orgId, rGraphAxis));
        graphMeta.put(CRMViewPropertyKeys.X, getMetaInfo(orgId, firstXAxis));

        if (isFirstAxisValuesEmpty && isSecondAxisValuesEmpty) {
            return buildFinalResponse(response, graphMeta, graphDataArray);
        }

        if (isPopulatable(xAxisFunction)) {
            return getTwoMetricsAndOneViewByWithFunction(
                    new BubbleChartContext(orgId, graphMeta, graphDataArray, response, firstMetricsXAxisValues, yGraphAxisValues,
                            columnId, xAxisFunction, i18nResolver),
                    secondMetricsXAxisValues, rGraphAxisValues);
        }

        final Map<Object, Integer> secondMetricsIndexMap = new HashMap<>(secondMetricsXAxisValuesSize);

        for (int index = NumericalConstant.ZERO; index < secondMetricsXAxisValuesSize; index++) {
            final AxisValue<Object> axisValue = secondMetricsXAxisValues.get(index);

            if (axisValue != null) {
                secondMetricsIndexMap.put(normalizeKeyValue(axisValue.getValue()), index);
            }
        }

        final Set<Object> matchedValues = new HashSet<>(firstMetricXAxisValuesSize);
        final Map<Object, JsonObject> graphDataMap = new HashMap<>();

        for (int index = NumericalConstant.ZERO; index < firstMetricXAxisValuesSize; index++) {
            final AxisValue<Object> firstMetricAxisValue = firstMetricsXAxisValues.get(index);
            final Object axisValue = Objects.nonNull(firstMetricAxisValue) ? firstMetricAxisValue.getValue() : null;
            final Object normalizedAxisValue = normalizeKeyValue(axisValue);
            final AxisValue<Object> yAxisValue = yGraphAxisValues.get(index);
            final Object yValue = Objects.nonNull(yAxisValue) ? yAxisValue.getValue() : NumericalConstant.ZERO;
            final Integer secondIndex = secondMetricsIndexMap.get(normalizedAxisValue);
            final Object rValue;

            if (Objects.nonNull(secondIndex)) {
                final AxisValue<Object> rAxisValue = rGraphAxisValues.get(secondIndex);
                rValue = Objects.nonNull(rAxisValue) ? rAxisValue.getValue() : NumericalConstant.ZERO;
            } else {
                rValue = NumericalConstant.ZERO;
            }

            final JsonObject graphDataJson = Json.object();

            matchedValues.add(normalizedAxisValue);

            if (axisValue instanceof Long && !crmUserMap.isEmpty()) {
                graphDataJson.put(CRMViewPropertyKeys.X, displayValueResolver.resolveDisplayValue(
                        orgId, getUserValue(axisValue, firstXAxis, crmUserMap), columnId, i18nResolver));
            } else {
                graphDataJson.put(CRMViewPropertyKeys.X, Objects.nonNull(axisValue) ? displayValueResolver.resolveDisplayValue(orgId, axisValue, columnId, i18nResolver) : null);
            }

            graphDataJson.put(CRMViewPropertyKeys.Y, getValueOrZero(yValue));
            graphDataJson.put(CRMViewPropertyKeys.R, getValueOrZero(rValue));
            graphDataMap.put(normalizedAxisValue, graphDataJson);
        }

        for (int i = NumericalConstant.ZERO; i < secondMetricsXAxisValuesSize; i++) {
            final AxisValue<Object> secondMetricsAxisValue = secondMetricsXAxisValues.get(i);
            final Object secondMetricsXAxisValue = Objects.nonNull(secondMetricsAxisValue) ? secondMetricsAxisValue.getValue() : null;
            final Object normalizedAxisValue = normalizeKeyValue(secondMetricsXAxisValue);

            if (!matchedValues.contains(normalizedAxisValue)) {
                final JsonObject graphDataJson = Json.object();
                final AxisValue<Object> rAxisValue = rGraphAxisValues.get(i);
                final Object rValue = Objects.nonNull(rAxisValue) ? rAxisValue.getValue() : NumericalConstant.ZERO;

                graphDataJson.put(CRMViewPropertyKeys.X, displayValueResolver.resolveDisplayValue(
                        orgId, getUserValue(secondMetricsXAxisValue, firstXAxis, crmUserMap), columnId, i18nResolver));
                graphDataJson.put(CRMViewPropertyKeys.Y, NumericalConstant.ZERO);
                graphDataJson.put(CRMViewPropertyKeys.R, getValueOrZero(rValue));
                graphDataMap.put(normalizedAxisValue, graphDataJson);
            }
        }

        if (CRMFunction.DATE.getFunctionName().equals(xAxisFunction)) {
            final Map<Object, JsonObject> sortedGraphDataMap = new TreeMap<>(Comparator.comparingLong(value ->
                    value instanceof Date date
                            ? date.getTime() : value instanceof Temporal temporal
                              ? temporal.getTimeStamp() : NumberUtils.parseNumber(value).longValue()));

            sortedGraphDataMap.putAll(graphDataMap);
            sortedGraphDataMap.values().forEach(graphDataArray::put);
        } else {
            graphDataMap.values().forEach(graphDataArray::put);
        }

        return buildFinalResponse(response, graphMeta, graphDataArray);
    }

    /**
     * Handles bubble chart data generation when the X axis has a time period function (e.g., BY_MONTH, BY_WEEK).
     * Aggregates Y and R metric values based on the transformed X-axis labels.
     *
     * @param context                  the common context for chart data generation
     * @param secondMetricsXAxisValues the second metric's X axis values
     * @param rGraphAxisValues         the R axis values (second metric)
     * @return a JsonObject containing the chart's meta and dataset values
     */
    private JsonObject getTwoMetricsAndOneViewByWithFunction(final BubbleChartContext context,
                                                             final List<AxisValue<Object>> secondMetricsXAxisValues,
                                                             final List<AxisValue<Object>> rGraphAxisValues) {
        final String functionName = context.functionName();
        final List<AxisValue<Object>> primaryXAxisValues = context.primaryXAxisValues();
        final I18nResolver i18nResolver = context.i18nResolver();
        final JsonArray graphDataArray = context.graphDataArray();
        final TimePeriodFunction timePeriodFunction = TimePeriodFunction.get(functionName);
        final Map<Integer, Object> yXAxisDataMap = new LinkedHashMap<>();
        final Map<Integer, Object> rXAxisDataMap = new LinkedHashMap<>();
        final List<Object> xAxisList = new ArrayList<>();
        final List<AxisValue<Object>> filteredYAxisValues = new ArrayList<>();
        final List<AxisValue<Object>> filteredRAxisValues = new ArrayList<>();
        final int axisValuesSize = primaryXAxisValues.size();

        for (int index = NumericalConstant.ZERO; index < axisValuesSize; index++) {
            final AxisValue<Object> axisValue = primaryXAxisValues.get(index);

            if (axisValue != null && axisValue.getValue() != null) {
                final AxisValue<Object> yAxisValue = context.yAxisValues().get(index);
                if (Objects.nonNull(yAxisValue)) {
                    yXAxisDataMap.put(filteredYAxisValues.size(), axisValue.getValue());
                    filteredYAxisValues.add(yAxisValue);
                }
            }
        }

        final int secondMetricsXAxisValuesSize = secondMetricsXAxisValues.size();

        for (int index = NumericalConstant.ZERO; index < secondMetricsXAxisValuesSize; index++) {
            final AxisValue<Object> axisValue = secondMetricsXAxisValues.get(index);

            if (axisValue != null && axisValue.getValue() != null) {
                final AxisValue<Object> rAxisValue = rGraphAxisValues.get(index);
                if (Objects.nonNull(rAxisValue)) {
                    rXAxisDataMap.put(filteredRAxisValues.size(), axisValue.getValue());
                    filteredRAxisValues.add(rAxisValue);
                }
            }
        }

        populateXAxisByLabel(functionName, filteredYAxisValues, yXAxisDataMap, xAxisList, i18nResolver);

        final Map<Object, Object> yValueMap = new LinkedHashMap<>();
        final Map<Object, Object> rValueMap = new LinkedHashMap<>();

        if (Objects.nonNull(timePeriodFunction)) {
            timePeriodFunction.populate(filteredYAxisValues, yXAxisDataMap, yValueMap, i18nResolver);
            timePeriodFunction.populate(filteredRAxisValues, rXAxisDataMap, rValueMap, i18nResolver);
        }

        for (final Object xAxisValue : xAxisList) {
            final JsonObject graphDataJson = Json.object();

            graphDataJson.put(CRMViewPropertyKeys.X, displayValueResolver.resolveDisplayValue(context.orgId(), xAxisValue, context.columnId(), i18nResolver));
            graphDataJson.put(CRMViewPropertyKeys.Y, getValueOrZero(yValueMap.get(xAxisValue)));
            graphDataJson.put(CRMViewPropertyKeys.R, getValueOrZero(rValueMap.get(xAxisValue)));
            graphDataArray.put(graphDataJson);
        }

        return buildFinalResponse(context.response(), context.graphMeta(), graphDataArray);
    }

    /**
     * Normalizes a value for use as a map key. Extracts timestamp from temporal objects
     * to ensure consistent HashMap lookups (since custom Date classes may not properly
     * implement hashCode).
     *
     * @param value the value to normalize
     * @return the normalized key value
     */
    private Object normalizeKeyValue(final Object value) {
        if (value instanceof com.tlc.l10n.temporal.model.Temporal temporal) {
            return temporal.getTimeStamp();
        }

        if (value instanceof java.util.Date date) {
            return date.getTime();
        }

        return value;
    }

    /**
     * Builds the metadata JSON object for a given graph axis.
     *
     * @param orgId the organization ID (must not be null)
     * @param axis  the graph axis containing the report field configuration (must not be null)
     * @return a {@link JsonObject} containing the axis metadata with label, measure, and data type
     */
    @Override
    public JsonObject getMetaInfo(final @NonNull Long orgId, final @NonNull GraphAxis<Object> axis) {
        final JsonObject metaJson = Json.object();
        final ReportField reportField = axis.getReportField();
        final OperationType operationType = reportField.getOperationType();

        metaJson.put(CRMViewPropertyKeys.LABEL, reportField.getLabel());
        metaJson.put(CRMViewPropertyKeys.MEASURE, I18nServiceProvider.get().getResolver(Locale.US)
                .get((Objects.equals(OperationType.COUNT, operationType) ? OperationType.TOTAL : operationType).getI18nKeyOfDisplayName()));
        metaJson.put(CRMViewPropertyKeys.DATA_TYPE, getDataType(reportField, columnDetailsService.get(reportField.getColumnId())));

        return metaJson;
    }

    /**
     * Determines whether a given function name supports time period population.
     *
     * @param function the function name to check (may be null)
     * @return {@code true} if the function is non-null and supports population;
     * {@code false} if the function is null or is DATE, BY_WEEK, or BY_YEAR
     */
    private boolean isPopulatable(final String function) {
        return Objects.nonNull(function) && (!Objects.equals(CRMFunction.DATE.getFunctionName(), function)) &&
                !Objects.equals(CRMFunction.BY_WEEK.getFunctionName(), function) && !(Objects.equals(CRMFunction.BY_YEAR.getFunctionName(), function));
    }

    /**
     * Populates axis data maps by grouping values based on a normalized key.
     * This is a common pattern used for building data structures for time period-based chart generation.
     *
     * @param keyAxisValues    the axis values to use as keys (will be normalized)
     * @param mappedValues     the values to map against each key
     * @param targetAxisValues the axis values to add to the values list
     * @param allKeys          the set to collect all unique normalized keys
     * @param dataMapByKey     the map to store mapped values indexed by position within each key group
     * @param axisValuesByKey  the map to store target axis values grouped by key
     */
    private void populateAxisDataMapByKey(final List<AxisValue<Object>> keyAxisValues,
                                          final List<AxisValue<Object>> mappedValues,
                                          final List<AxisValue<Object>> targetAxisValues,
                                          final Set<Object> allKeys,
                                          final Map<Object, Map<Integer, Object>> dataMapByKey,
                                          final Map<Object, List<AxisValue<Object>>> axisValuesByKey) {

        final int size = keyAxisValues.size();

        for (int index = NumericalConstant.ZERO; index < size; index++) {
            final AxisValue<Object> keyAxisValue = keyAxisValues.get(index);
            final AxisValue<Object> mappedAxisValue = mappedValues.get(index);
            final AxisValue<Object> targetAxisValue = targetAxisValues.get(index);
            final Object keyValue = Objects.nonNull(keyAxisValue) ? keyAxisValue.getValue() : null;
            final Object mappedValue = Objects.nonNull(mappedAxisValue) ? mappedAxisValue.getValue() : null;

            if (Objects.nonNull(keyValue) && Objects.nonNull(mappedValue) && Objects.nonNull(targetAxisValue)) {
                final Object normalizedKey = normalizeKeyValue(keyValue);

                allKeys.add(normalizedKey);
                dataMapByKey.computeIfAbsent(normalizedKey, valueMap -> new LinkedHashMap<>());
                axisValuesByKey.computeIfAbsent(normalizedKey, valuesList -> new ArrayList<>());

                final List<AxisValue<Object>> axisValues = axisValuesByKey.get(normalizedKey);

                dataMapByKey.get(normalizedKey).put(axisValues.size(), mappedValue);
                axisValues.add(targetAxisValue);
            }
        }
    }

    /**
     * Populates axis data maps with flexible null handling for one metric scenarios.
     * Groups values by a normalized key and optionally maps secondary values.
     * The target axis value is always added when the key is non-null.
     *
     * @param keyAxisValues    the axis values to use as keys (will be normalized)
     * @param mappedValues     the values to map against each key (can contain nulls)
     * @param targetAxisValues the axis values to add to the values list
     * @param allKeys          the set to collect all unique normalized keys
     * @param dataMapByKey     the map to store mapped values indexed by position within each key group
     * @param axisValuesByKey  the map to store target axis values grouped by key
     */
    private void populateAxisDataMapByKeyWithOptionalMapping(
            final List<AxisValue<Object>> keyAxisValues,
            final List<AxisValue<Object>> mappedValues,
            final List<AxisValue<Object>> targetAxisValues,
            final Set<Object> allKeys,
            final Map<Object, Map<Integer, Object>> dataMapByKey,
            final Map<Object, List<AxisValue<Object>>> axisValuesByKey) {

        final int size = keyAxisValues.size();

        for (int index = NumericalConstant.ZERO; index < size; index++) {
            final AxisValue<Object> keyAxisValue = keyAxisValues.get(index);
            final Object keyValue = Objects.nonNull(keyAxisValue) ? keyAxisValue.getValue() : null;
            final AxisValue<Object> targetAxisValue = targetAxisValues.get(index);

            if (Objects.nonNull(keyValue) && Objects.nonNull(targetAxisValue)) {
                final Object normalizedKey = normalizeKeyValue(keyValue);

                allKeys.add(normalizedKey);
                dataMapByKey.computeIfAbsent(normalizedKey, valueMap -> new LinkedHashMap<>());
                axisValuesByKey.computeIfAbsent(normalizedKey, valuesList -> new ArrayList<>());

                final List<AxisValue<Object>> axisValues = axisValuesByKey.get(normalizedKey);

                final AxisValue<Object> mappedAxisValue = mappedValues.get(index);
                final Object mappedValue = Objects.nonNull(mappedAxisValue) ? mappedAxisValue.getValue() : null;
                if (Objects.nonNull(mappedValue)) {
                    dataMapByKey.get(normalizedKey).put(axisValues.size(), mappedValue);
                }

                axisValues.add(targetAxisValue);
            }
        }
    }

    /**
     * Builds a combined data map from axis values, filtering out null values.
     *
     * @param axisValues the axis values to process
     * @return a map of sequential indices to non-null values
     */
    private Map<Integer, Object> buildCombinedDataMap(final List<AxisValue<Object>> axisValues) {
        final Map<Integer, Object> combinedMap = new LinkedHashMap<>();
        final Counter counter = new Counter(NumericalConstant.ZERO);

        for (final AxisValue<Object> axisValue : axisValues) {
            final Object value = Objects.nonNull(axisValue) ? axisValue.getValue() : null;

            if (Objects.nonNull(value)) {
                combinedMap.put(counter.getAndIncrement(), value);
            }
        }

        return combinedMap;
    }

}
