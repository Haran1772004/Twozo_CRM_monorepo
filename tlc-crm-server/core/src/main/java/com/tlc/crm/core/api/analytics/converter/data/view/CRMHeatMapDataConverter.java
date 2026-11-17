package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.analytics.converter.data.DisplayValueResolver;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Converter implementation for generating Heat Map data in CRM analytics views.
 *
 * @author Ryan Sam Daniel N
 * @version 1.0
 */
public class CRMHeatMapDataConverter extends AbstractViewDataConverter {
    private static final int ZERO_INDEX = 0;
    private static final int FIRST_INDEX = 1;
    private static final int SECOND_INDEX = 2;
    private static final int THIRD_INDEX = 3;
    private static final int FOURTH_INDEX = 4;
    private static final int FIFTH_INDEX = 5;
    private static final int SIXTH_INDEX = 6;
    private static final int SEVENTH_INDEX = 7;
    private static final int EIGHTH_INDEX = 8;
    private final DisplayValueResolver displayValueResolver;

    protected CRMHeatMapDataConverter() {
        this.displayValueResolver = DisplayValueResolver.INSTANCE();
    }

    private static class Instance {
        private static final CRMHeatMapDataConverter INSTANCE = new CRMHeatMapDataConverter();
    }

    /**
     * Gets the {@link CRMHeatMapDataConverter} instance
     *
     * @return {@link CRMHeatMapDataConverter}
     */
    public static CRMHeatMapDataConverter INSTANCE() {
        return CRMHeatMapDataConverter.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        the ID of the organization to which the axis belongs (must not be null)
     * @param graphAxis    the list of {@link GraphAxis} objects containing report field and axis details (must not be null)
     * @param crmUserMap   a map of CRM user IDs to their corresponding {@link CrmUser} objects (must not be null)
     * @param i18nResolver the internationalization resolver used for localizing output (must not be null)
     * @return a {@link JsonObject} containing the processed chart data
     */
    @Override
    protected JsonObject getData(final @NonNull Long orgId, final @NonNull List<GraphAxis<Object>> graphAxis,
                                 final @NonNull Map<Long, CrmUser> crmUserMap, final @NonNull I18nResolver i18nResolver) {
        int maxXValue = 0;
        int maxYValue = 0;

        for (final GraphAxis<Object> axis : graphAxis) {
            final ReportField field = axis.getReportField();
            final int order = Math.toIntExact(field.getOrder());

            if (Axis.X_AXIS == field.getAxis()) {
                maxXValue = Math.max(maxXValue, order);
            } else {
                maxYValue = Math.max(maxYValue, order);
            }
        }

        if (maxXValue == 2 && maxYValue == 1) {
            return getOneMetricsAndTwoViewBy(orgId, graphAxis, crmUserMap, i18nResolver);
        }
        if (maxXValue == 1 && maxYValue == 3) {
            return getThreeMetricsAndOneViewBy(orgId, graphAxis, crmUserMap, i18nResolver);
        }
        if (maxXValue == 1 && maxYValue == 4) {
            return getFourMetricsAndOneViewBy(orgId, graphAxis, crmUserMap, i18nResolver);
        }
        if (maxXValue == 1 && maxYValue == 2) {
            return getTwoMetricsAndOneViewBy(orgId, graphAxis, crmUserMap, i18nResolver);
        }

        return getOneMetricsAndOneViewBy(orgId, graphAxis, crmUserMap, i18nResolver);
    }

    /**
     * Builds heat map data for configurations containing a single metric and a single view-by field.
     *
     * @param orgId        the organization ID
     * @param collection   chart axis definitions
     * @param crmUserMap   mapping of CRM user IDs to {@link CrmUser}
     * @param i18nResolver resolver for localizing values
     * @return a {@link JsonObject} containing the chart's meta and dataset values
     */
    public JsonObject getOneMetricsAndOneViewBy(final @NonNull Long orgId,
                                                final @NonNull List<GraphAxis<Object>> collection,
                                                final @NonNull Map<Long, CrmUser> crmUserMap,
                                                final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonObject graphMeta = Json.object();
        final JsonArray graphDataArray = Json.array();
        final JsonArray normalizedYArray = Json.array();

        final Collection<Object> xAxisList = new ArrayList<>();
        final Map<Integer, Object> xAxisValueByFirstYAxis = new LinkedHashMap<>();
        final List<AxisValue<Object>> filteredYAxisValues = new ArrayList<>();

        final GraphAxis<Object> xAxis = collection.get(ZERO_INDEX);
        final GraphAxis<Object> yAxis = collection.get(FIRST_INDEX);

        final List<AxisValue<Object>> axisValues = xAxis.getAxisValues();
        final List<AxisValue<Object>> yAxisValues = yAxis.getAxisValues();

        final ReportField xAxisReportField = xAxis.getReportField();
        final ReportField yAxisReportField = yAxis.getReportField();

        final String xAxisFunction = xAxisReportField.getFunctionName();
        final String xAxisLabel = xAxisReportField.getLabel();
        final String yAxisLabel = yAxisReportField.getLabel();

        final Long xAxisColumnId = xAxisReportField.getColumnId();
        final Long yAxisColumnId = yAxisReportField.getColumnId();

        final int axisValuesSize = axisValues.size();

        graphMeta.put(CRMViewPropertyKeys.Y_AXIS_LABEL, getMetaInfo(orgId, yAxis));
        graphMeta.put(CRMViewPropertyKeys.X_AXIS_LABEL, getMetaInfo(orgId, xAxis));

        if (axisValues.isEmpty()) {
            return buildEmptyGraphResponse(graphMeta, false);
        }

        for (int index = ZERO_INDEX; index < axisValuesSize; index++) {
            final JsonArray toolTipArray = Json.array();
            final JsonObject toolTipXValues = Json.object();
            final JsonObject toolTipYValues = Json.object();
            final JsonObject graphDataJson = Json.object();

            final Object resolvedXValue = resolveAxisDisplayValue(orgId, axisValues.get(index).getValue(), xAxis,
                    crmUserMap, xAxisColumnId, displayValueResolver, i18nResolver);
            final Object resolvedYValue = resolveMetricDisplayValue(orgId, getNullableUserValue(yAxisValues.get(index).getValue(), yAxis, crmUserMap),
                    yAxisColumnId, displayValueResolver, i18nResolver);

            if (isBlankAxisValue(resolvedXValue)) {
                continue;
            }

            if (Objects.isNull(xAxisFunction)) {
                buildGraphDataJson(resolvedXValue, resolvedYValue, null, graphDataJson);

                toolTipXValues.put(CRMReportKeys.KEY, xAxisLabel);
                toolTipXValues.put(CRMReportKeys.VALUE, resolvedXValue);
                toolTipYValues.put(CRMReportKeys.KEY, yAxisLabel);
                toolTipYValues.put(CRMReportKeys.VALUE, resolvedYValue);
                toolTipArray.put(toolTipXValues);
                toolTipArray.put(toolTipYValues);

                graphDataJson.put(CRMReportKeys.TOOLTIP, toolTipArray);
                graphDataArray.put(graphDataJson);
            }

            if (Objects.nonNull(xAxisFunction)) {
                xAxisValueByFirstYAxis.put(filteredYAxisValues.size(), resolvedXValue);
                filteredYAxisValues.add(yAxisValues.get(index));
            }
        }

        if (Objects.nonNull(xAxisFunction)) {
            populateXAxisByLabel(xAxisFunction, filteredYAxisValues, xAxisValueByFirstYAxis, xAxisList, i18nResolver);
            populateYAxisByLabel(orgId, xAxisFunction, yAxis,
                    xAxisValueByFirstYAxis, normalizedYArray, displayValueResolver, i18nResolver);

            final JsonArray xValuesArray = Json.array(xAxisList);
            final int xAxisListSize = xValuesArray.size();

            for (int index = ZERO_INDEX; index < xAxisListSize; index++) {
                final JsonArray toolTipArray = Json.array();
                final JsonObject toolTipXValues = Json.object();
                final JsonObject toolTipYValues = Json.object();
                final JsonObject graphDataJson = Json.object();

                final Object resolvedXValue = resolveAxisDisplayValue(orgId, xValuesArray.get(index), xAxis,
                        crmUserMap, xAxisColumnId, displayValueResolver, i18nResolver);
                final Object resolvedYValue = resolveMetricDisplayValue(orgId, getNullableUserValue(normalizedYArray.get(index), yAxis, crmUserMap),
                        yAxisColumnId, displayValueResolver, i18nResolver);

                buildGraphDataJson(resolvedXValue, resolvedYValue, null, graphDataJson);

                toolTipXValues.put(CRMReportKeys.KEY, xAxisLabel);
                toolTipXValues.put(CRMReportKeys.VALUE, resolvedXValue);
                toolTipYValues.put(CRMReportKeys.KEY, yAxisLabel);
                toolTipYValues.put(CRMReportKeys.VALUE, resolvedYValue);
                toolTipArray.put(toolTipXValues);
                toolTipArray.put(toolTipYValues);

                graphDataJson.put(CRMReportKeys.TOOLTIP, toolTipArray);
                graphDataArray.put(graphDataJson);
            }
        }

        response.put(CRMViewPropertyKeys.GRAPH_META, graphMeta);
        response.put(CRMViewPropertyKeys.GRAPH_DATA, graphDataArray);
        return response;
    }

    /**
     * Builds heat map data for configurations containing a single metric and a two view-by field.
     *
     * @param orgId        the organization ID
     * @param collection   chart axis definitions
     * @param crmUserMap   mapping of CRM user IDs to {@link CrmUser}
     * @param i18nResolver resolver for localizing values
     * @return a {@link JsonObject} containing the chart's meta and dataset values
     */
    public JsonObject getOneMetricsAndTwoViewBy(final @NonNull Long orgId,
                                                final @NonNull List<GraphAxis<Object>> collection,
                                                final @NonNull Map<Long, CrmUser> crmUserMap,
                                                final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonObject graphMeta = Json.object();
        final JsonArray graphDataArray = Json.array();

        final Map<Integer, Object> xAxisValueByFirstYAxis = new LinkedHashMap<>();
        final Collection<Object> xAxisList = new ArrayList<>();

        GraphAxis<Object> yAxis = null;
        GraphAxis<Object> xAxisOne = null;
        GraphAxis<Object> xAxisTwo = null;

        for (final GraphAxis<Object> axis : collection) {
            final ReportField reportField = axis.getReportField();

            if (Objects.equals(Axis.Y_AXIS, reportField.getAxis())) {
                yAxis = axis;
            } else if (Objects.equals(FIRST_INDEX, Math.toIntExact(reportField.getOrder()))) {
                xAxisOne = axis;
            } else {
                xAxisTwo = axis;
            }
        }

        final List<AxisValue<Object>> xAxisOneValues = Objects.isNull(xAxisOne) ? Collections.emptyList() : xAxisOne.getAxisValues();
        final List<AxisValue<Object>> xAxisTwoValues = Objects.isNull(xAxisTwo) ? Collections.emptyList() : xAxisTwo.getAxisValues();
        final List<AxisValue<Object>> yAxisValues = Objects.isNull(yAxis) ? Collections.emptyList() : yAxis.getAxisValues();

        final int xAxisOneValuesSize = xAxisOneValues.size();
        final int xAxisTwoValuesSize = xAxisTwoValues.size();

        final ReportField xOneReportField = xAxisOne.getReportField();
        final ReportField xTwoReportField = xAxisTwo.getReportField();
        final ReportField yAxisReportField = yAxis.getReportField();

        final Long xOneColumnId = xOneReportField.getColumnId();
        final Long xTwoColumnId = xTwoReportField.getColumnId();
        final Long yColumnId = yAxisReportField.getColumnId();

        final String xOneFunction = xOneReportField.getFunctionName();
        final String xTwoFunction = xTwoReportField.getFunctionName();

        final String xAxisOneLabel = xOneReportField.getLabel();
        final String xAxisTwoLabel = xTwoReportField.getLabel();
        final String yAxisLabel = yAxisReportField.getLabel();

        graphMeta.put(CRMViewPropertyKeys.Y_AXIS_LABEL, getMetaInfo(orgId, xAxisTwo));
        graphMeta.put(CRMViewPropertyKeys.X_AXIS_LABEL, getMetaInfo(orgId, xAxisOne));

        if (xAxisOneValues.isEmpty() && xAxisTwoValues.isEmpty()) {
            return buildEmptyGraphResponse(graphMeta, true);
        }

        final int minAxisValueSize = Math.min(xAxisOneValuesSize, xAxisTwoValuesSize);

        for (int index = ZERO_INDEX; index < minAxisValueSize; index++) {
            final JsonObject graphDataJson = Json.object();
            final JsonArray toolTipArray = Json.array();
            final Object axisOneValue = xAxisOneValues.get(index).getValue();
            final Object axisTwoValue = xAxisTwoValues.get(index).getValue();
            final Object resolveAxisOneValue = resolveAxisDisplayValue(orgId, axisOneValue, xAxisOne, crmUserMap, xOneColumnId, displayValueResolver, i18nResolver);
            final Object resolveAxisTwoValue = resolveAxisDisplayValue(orgId, axisTwoValue, xAxisTwo, crmUserMap, xTwoColumnId, displayValueResolver, i18nResolver);
            final Object resolveMetricValue = resolveMetricDisplayValue(orgId,
                    getNullableUserValue(yAxisValues.get(index).getValue(), yAxis, crmUserMap), yColumnId, displayValueResolver, i18nResolver);

            if (Objects.isNull(xOneFunction) && Objects.isNull(xTwoFunction)) {
                buildGraphDataJson(resolveAxisOneValue, resolveAxisTwoValue, resolveMetricValue, graphDataJson);

                toolTipArray.put(Json.object().put(CRMReportKeys.KEY, displayValueResolver.resolveDisplayValue(orgId, xAxisOneLabel,
                                                                                                               xOneColumnId, i18nResolver))
                            .put(CRMReportKeys.VALUE, resolveAxisOneValue));
                toolTipArray.put(Json.object().put(CRMReportKeys.KEY, displayValueResolver.resolveDisplayValue(orgId, xAxisTwoLabel,
                                                                                                               xTwoColumnId, i18nResolver))
                            .put(CRMReportKeys.VALUE, resolveAxisTwoValue));
                toolTipArray.put(Json.object().put(CRMReportKeys.KEY, displayValueResolver.resolveDisplayValue(orgId, yAxisLabel,
                                                                                                               yColumnId, i18nResolver))
                            .put(CRMReportKeys.VALUE, resolveMetricValue));

                graphDataJson.put(CRMReportKeys.TOOLTIP, toolTipArray);
                graphDataArray.put(graphDataJson);
            }

            if (Objects.nonNull(xOneFunction)) {
                xAxisValueByFirstYAxis.put(index, axisOneValue);
            }

            if (Objects.nonNull(xTwoFunction)) {
                xAxisValueByFirstYAxis.put(index, axisTwoValue);
            }
        }

        if (Objects.nonNull(xOneFunction) || Objects.nonNull(xTwoFunction)) {
            final Map<Object, Map<String, Object>> axisValueMap = new HashMap<>();
            final String function = Objects.nonNull(xOneFunction) ? xOneFunction : xTwoFunction;
            final boolean isFirstAxis = Objects.nonNull(xOneFunction);

            populateXAxisByLabel(function, yAxisValues, xAxisValueByFirstYAxis, xAxisList, i18nResolver);
            populateYAxisByLabel(orgId, function, yAxis, xAxisValueByFirstYAxis,
                                 Json.array(), displayValueResolver, i18nResolver);

            final List<AxisValue<Object>> firstAxisValues = isFirstAxis ? xAxisOneValues : xAxisTwoValues;
            final List<AxisValue<Object>> secondAxisValues = isFirstAxis ? xAxisTwoValues : xAxisOneValues;
            final int axisValuesSize = firstAxisValues.size();

            for (int index = ZERO_INDEX; index < axisValuesSize; index++) {
                axisValueMap.computeIfAbsent(resolveNullableDisplayValue(orgId, firstAxisValues.get(index).getValue(),
                                        isFirstAxis ? xOneColumnId : xTwoColumnId, displayValueResolver, i18nResolver),
                                value -> new HashMap<>())
                        .put(String.valueOf(secondAxisValues.get(index).getValue()), yAxisValues.get(index).getValue());
            }

            for (final Object xAxisListValue : xAxisList) {
                final Map<String, Object> values = axisValueMap.get(resolveNullableDisplayValue(orgId, xAxisListValue,
                        isFirstAxis ? xOneColumnId : xTwoColumnId, displayValueResolver, i18nResolver));
                final List<AxisValue<Object>> loopValues = isFirstAxis ? xAxisTwoValues : xAxisOneValues;

                for (final AxisValue<Object> axisValueObject : loopValues) {
                    final Object axisValue = axisValueObject.getValue();

                    buildGraphData(orgId, crmUserMap,
                            isFirstAxis ? getUserValue(xAxisListValue, xAxisOne, crmUserMap) : getUserValue(axisValue, xAxisOne, crmUserMap),
                            isFirstAxis ? getUserValue(axisValue, xAxisTwo, crmUserMap) : getUserValue(xAxisListValue, xAxisTwo, crmUserMap),
                            Objects.nonNull(values) ? findYValue(values, axisValue) : null,
                            xAxisOne, xAxisTwo, yAxis, graphDataArray, displayValueResolver, i18nResolver);
                }
            }
        }

        response.put(CRMViewPropertyKeys.GRAPH_META, graphMeta);
        response.put(CRMViewPropertyKeys.GRAPH_DATA, graphDataArray);
        return response;
    }

    /**
     * Builds Heat map data for configurations containing two metrics and one view-by fields.
     *
     * @param orgId        the organization ID
     * @param collection   chart axis definitions
     * @param crmUserMap   mapping of CRM user IDs to {@link CrmUser}
     * @param i18nResolver resolver for localizing values
     * @return a {@link JsonObject} containing the chart's meta and dataset values
     */
    public JsonObject getTwoMetricsAndOneViewBy(final @NonNull Long orgId,
                                                final @NonNull List<GraphAxis<Object>> collection,
                                                final @NonNull Map<Long, CrmUser> crmUserMap,
                                                final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonObject graphMeta = Json.object();
        final JsonArray graphDataArray = Json.array();
        final JsonArray normalizedYArrayOne = Json.array();
        final JsonArray normalizedYArrayTwo = Json.array();

        final Map<Integer, Object> xAxisValueByFirstYAxis = new LinkedHashMap<>();
        final Map<Integer, Object> xAxisValueBySecondYAxis = new LinkedHashMap<>();
        final Collection<Object> xAxisList = new ArrayList<>();
        final int collectionSize = collection.size();

        final List<GraphAxis<Object>> firstList = collection.subList(ZERO_INDEX, Math.min(SECOND_INDEX, collectionSize));
        final List<GraphAxis<Object>> secondList = collectionSize > THIRD_INDEX ? collection.subList(SECOND_INDEX, FOURTH_INDEX)
                                                                                   : Collections.emptyList();
        final GraphAxis<Object> xAxis = firstList.get(ZERO_INDEX);
        final GraphAxis<Object> yAxisOne = firstList.get(FIRST_INDEX);
        final GraphAxis<Object> xAxisTwo = secondList.get(ZERO_INDEX);
        final GraphAxis<Object> yAxisTwo = secondList.get(FIRST_INDEX);

        final ReportAxisContext yAxisOneContext = new ReportAxisContext(FIRST_INDEX, orgId, xAxis, yAxisOne, crmUserMap, displayValueResolver, i18nResolver);
        final ReportAxisContext yAxisTwoContext = new ReportAxisContext(SECOND_INDEX, orgId, xAxisTwo, yAxisTwo, crmUserMap, displayValueResolver, i18nResolver);

        final List<AxisValue<Object>> xAxisValues = Objects.nonNull(xAxis) ? xAxis.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> xAxisTwoValues = Objects.nonNull(xAxisTwo) ? xAxisTwo.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> yAxisOneValues = Objects.nonNull(yAxisOne) ? yAxisOne.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> yAxisTwoValues = Objects.nonNull(yAxisTwo) ? yAxisTwo.getAxisValues() : Collections.emptyList();

        final String xAxisFunction = xAxis.getReportField().getFunctionName();
        final String dataLabel = Objects.nonNull(xAxisFunction) ? xAxisFunction : xAxis.getReportField().getLabel();

        graphMeta.put(CRMViewPropertyKeys.Y_AXIS_LABEL, getMetaInfo(orgId, yAxisTwo));
        graphMeta.put(CRMViewPropertyKeys.X_AXIS_LABEL, getMetaInfo(orgId, xAxis));

        if (xAxisValues.isEmpty() && xAxisTwoValues.isEmpty()) {
            return buildEmptyGraphResponse(graphMeta, true);
        }

        addYAxisGraphData(yAxisOneValues.size(), xAxisValueByFirstYAxis, graphDataArray, yAxisOneContext);
        addYAxisGraphData(yAxisTwoValues.size(), xAxisValueBySecondYAxis, graphDataArray, yAxisTwoContext);

        final Map<Integer, Object> axisDataMap = resolveXAxisValues(List.of(xAxisValueByFirstYAxis, xAxisValueBySecondYAxis));

        if (Objects.nonNull(xAxisFunction)) {
            populateXAxisByLabel(dataLabel, yAxisOneValues, axisDataMap, xAxisList, i18nResolver);
            populateYAxisByLabel(orgId, dataLabel, yAxisOne,
                    axisDataMap, normalizedYArrayOne, displayValueResolver, i18nResolver);
            populateYAxisByLabel(orgId, dataLabel, yAxisTwo,
                    axisDataMap, normalizedYArrayTwo, displayValueResolver, i18nResolver);
        }

        final JsonArray xValuesArray = Json.array(xAxisList);

        addGraphData(graphDataArray, xValuesArray, normalizedYArrayOne, yAxisOneContext);
        addGraphData(graphDataArray, xValuesArray, normalizedYArrayTwo, yAxisTwoContext);

        response.put(CRMViewPropertyKeys.GRAPH_META, graphMeta);
        response.put(CRMViewPropertyKeys.GRAPH_DATA, graphDataArray);
        return response;
    }

    /**
     * Resolves and merges X-axis values from multiple axis data maps into a single ordered map.
     * <p>
     * For each axis index, the method selects the first valid value found across the
     * provided maps. A value is considered valid if it is non-null and, in the case
     * of numeric values, not equal to zero.
     * </p>
     *
     * @param xAxisDataMaps list of X-axis data maps where the key represents the axis
     *                      index and the value represents the corresponding axis value
     * @return a {@link LinkedHashMap} containing the resolved X-axis values ordered by
     *         axis index; returns an empty map if the input list is null or empty
     */
    private Map<Integer, Object> resolveXAxisValues(final List<Map<Integer, Object>> xAxisDataMaps) {
        final Map<Integer, Object> result = new LinkedHashMap<>();
        final Set<Integer> allAxisValueIndices = new TreeSet<>();

        if (Objects.isNull(xAxisDataMaps) || xAxisDataMaps.isEmpty()) {
            return result;
        }

        for (final Map<Integer, Object> map : xAxisDataMaps) {

            if (Objects.nonNull(map)) {
                allAxisValueIndices.addAll(map.keySet());
            }
        }

        for (final Integer axisValueIndex : allAxisValueIndices) {

            for (final Map<Integer, Object> map : xAxisDataMaps) {

                if (Objects.nonNull(map) && map.containsKey(axisValueIndex)) {
                    final Object value = map.get(axisValueIndex);

                    if (isValidAxisValue(value)) {
                        result.put(axisValueIndex, value);
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Checks if a value is valid (not null and not zero).
     *
     * @param value the value to check
     * @return true if the value is valid, false otherwise
     */
    private boolean isValidAxisValue(final Object value) {
        if (Objects.isNull(value)) {
            return false;
        }

        if (value instanceof Number number) {
            return number.doubleValue() != ZERO_INDEX;
        }

        return true;
    }

    /**
     * Builds heat map data for configurations containing three metrics and one view-by fields.
     *
     * @param orgId        the organization ID
     * @param collection   chart axis definitions
     * @param crmUserMap   mapping of CRM user IDs to {@link CrmUser}
     * @param i18nResolver resolver for localizing values
     * @return a {@link JsonObject} containing the chart's meta and dataset values
     */
    public JsonObject getThreeMetricsAndOneViewBy(final @NonNull Long orgId,
                                                  final @NonNull List<GraphAxis<Object>> collection,
                                                  final @NonNull Map<Long, CrmUser> crmUserMap,
                                                  final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonObject graphMeta = Json.object();
        final JsonArray graphDataArray = Json.array();
        final JsonArray normalizedYArrayOne = Json.array();
        final JsonArray normalizedYArrayTwo = Json.array();
        final JsonArray normalizedYArrayThree = Json.array();

        final Map<Integer, Object> xAxisValueByFirstYAxis = new LinkedHashMap<>();
        final Map<Integer, Object> xAxisValueBySecondYAxis = new LinkedHashMap<>();
        final Map<Integer, Object> xAxisValueByThirdYAxis = new LinkedHashMap<>();
        final Collection<Object> xAxisList = new ArrayList<>();
        final int collectionSize = collection.size();

        final List<GraphAxis<Object>> firstList = collection.subList(ZERO_INDEX, Math.min(SECOND_INDEX, collectionSize));
        final List<GraphAxis<Object>> secondList = collectionSize > THIRD_INDEX ? collection.subList(SECOND_INDEX, FOURTH_INDEX)
                : Collections.emptyList();
        final List<GraphAxis<Object>> thirdList = collectionSize > FIFTH_INDEX ? collection.subList(FOURTH_INDEX, SIXTH_INDEX)
                : Collections.emptyList();

        final GraphAxis<Object> xAxis = firstList.get(ZERO_INDEX);
        final GraphAxis<Object> xAxisTwo = secondList.get(ZERO_INDEX);
        final GraphAxis<Object> xAxisThree = thirdList.get(ZERO_INDEX);
        final GraphAxis<Object> yAxisOne = firstList.get(FIRST_INDEX);
        final GraphAxis<Object> yAxisTwo = secondList.get(FIRST_INDEX);
        final GraphAxis<Object> yAxisThree = thirdList.get(FIRST_INDEX);

        final List<AxisValue<Object>> xAxisValues = Objects.nonNull(xAxis) ? xAxis.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> xAxisTwoValues = Objects.nonNull(xAxisTwo) ? xAxisTwo.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> xAxisThreeValues = Objects.nonNull(xAxisThree) ? xAxisThree.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> yAxisOneValues = Objects.nonNull(yAxisOne) ? yAxisOne.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> yAxisTwoValues = Objects.nonNull(yAxisTwo) ? yAxisTwo.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> yAxisThreeValues = Objects.nonNull(yAxisThree) ? yAxisThree.getAxisValues() : Collections.emptyList();

        final String xAxisFunction = xAxis.getReportField().getFunctionName();

        graphMeta.put(CRMViewPropertyKeys.Y_AXIS_LABEL, getMetaInfo(orgId, yAxisTwo));
        graphMeta.put(CRMViewPropertyKeys.X_AXIS_LABEL, getMetaInfo(orgId, xAxis));

        if (xAxisValues.isEmpty() && xAxisTwoValues.isEmpty() && xAxisThreeValues.isEmpty()) {
            return buildEmptyGraphResponse(graphMeta, true);
        }

        final ReportAxisContext contextOne = new ReportAxisContext(FIRST_INDEX, orgId, xAxis, yAxisOne, crmUserMap, displayValueResolver, i18nResolver);
        final ReportAxisContext contextTwo = new ReportAxisContext(SECOND_INDEX, orgId, xAxisTwo, yAxisTwo, crmUserMap, displayValueResolver, i18nResolver);
        final ReportAxisContext contextThree = new ReportAxisContext(THIRD_INDEX, orgId, xAxisThree, yAxisThree, crmUserMap, displayValueResolver, i18nResolver);

        addYAxisGraphData(yAxisOneValues.size(), xAxisValueByFirstYAxis, graphDataArray, contextOne);
        addYAxisGraphData(yAxisTwoValues.size(), xAxisValueBySecondYAxis, graphDataArray, contextTwo);
        addYAxisGraphData(yAxisThreeValues.size(), xAxisValueByThirdYAxis, graphDataArray, contextThree);

        final Map<Integer, Object> axisDataMap = resolveXAxisValues(List.of(xAxisValueByFirstYAxis, xAxisValueBySecondYAxis, xAxisValueByThirdYAxis));

        if (Objects.nonNull(xAxisFunction)) {
            populateXAxisByLabel(xAxisFunction, yAxisOneValues, axisDataMap, xAxisList, i18nResolver);
            populateYAxisByLabel(orgId, xAxisFunction, yAxisOne,
                    axisDataMap, normalizedYArrayOne, displayValueResolver, i18nResolver);
            populateYAxisByLabel(orgId, xAxisFunction,
                                 yAxisTwo, axisDataMap, normalizedYArrayTwo, displayValueResolver, i18nResolver);
            populateYAxisByLabel(orgId, xAxisFunction, yAxisThree,
                    axisDataMap, normalizedYArrayThree, displayValueResolver, i18nResolver);
        }

        final JsonArray xValuesArray = Json.array(xAxisList);

        addGraphData(graphDataArray, xValuesArray, normalizedYArrayOne, contextOne);
        addGraphData(graphDataArray, xValuesArray, normalizedYArrayTwo, contextTwo);
        addGraphData(graphDataArray, xValuesArray, normalizedYArrayThree, contextThree);

        response.put(CRMViewPropertyKeys.GRAPH_META, graphMeta);
        response.put(CRMViewPropertyKeys.GRAPH_DATA, graphDataArray);
        return response;
    }

    /**
     * Builds heat map data for configurations containing four metrics and one view-by fields.
     *
     * @param orgId        the organization ID
     * @param collection   chart axis definitions
     * @param crmUserMap   mapping of CRM user IDs to {@link CrmUser}
     * @param i18nResolver resolver for localizing values
     * @return a {@link JsonObject} containing the chart's meta and dataset values
     */
    public JsonObject getFourMetricsAndOneViewBy(final @NonNull Long orgId,
                                                 final @NonNull List<GraphAxis<Object>> collection,
                                                 final @NonNull Map<Long, CrmUser> crmUserMap,
                                                 final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonObject graphMeta = Json.object();

        final JsonArray graphDataArray = Json.array();
        final JsonArray normalizedYArrayOne = Json.array();
        final JsonArray normalizedYArrayTwo = Json.array();
        final JsonArray normalizedYArrayThree = Json.array();
        final JsonArray normalizedYArrayFour = Json.array();

        final Map<Integer, Object> xAxisValueByFirstYAxis = new LinkedHashMap<>();
        final Map<Integer, Object> xAxisValueBySecondYAxis = new LinkedHashMap<>();
        final Map<Integer, Object> xAxisValueByThirdYAxis = new LinkedHashMap<>();
        final Map<Integer, Object> xAxisValueByFourthYAxis = new LinkedHashMap<>();
        final Collection<Object> xAxisList = new ArrayList<>();
        final int collectionSize = collection.size();

        final List<GraphAxis<Object>> firstList = collection.subList(ZERO_INDEX, Math.min(SECOND_INDEX, collectionSize));
        final List<GraphAxis<Object>> secondList = collectionSize > THIRD_INDEX ? collection.subList(SECOND_INDEX, FOURTH_INDEX)
                : Collections.emptyList();
        final List<GraphAxis<Object>> thirdList = collectionSize > FIFTH_INDEX ? collection.subList(FOURTH_INDEX, SIXTH_INDEX)
                : Collections.emptyList();
        final List<GraphAxis<Object>> fourthList = collectionSize > SEVENTH_INDEX ? collection.subList(SIXTH_INDEX, EIGHTH_INDEX)
                : Collections.emptyList();

        final GraphAxis<Object> xAxis = firstList.get(ZERO_INDEX);
        final GraphAxis<Object> xAxisTwo = secondList.get(ZERO_INDEX);
        final GraphAxis<Object> xAxisThree = thirdList.get(ZERO_INDEX);
        final GraphAxis<Object> xAxisFour = fourthList.get(ZERO_INDEX);
        final GraphAxis<Object> yAxisOne = firstList.get(FIRST_INDEX);
        final GraphAxis<Object> yAxisTwo = secondList.get(FIRST_INDEX);
        final GraphAxis<Object> yAxisThree = thirdList.get(FIRST_INDEX);
        final GraphAxis<Object> yAxisFour = fourthList.get(FIRST_INDEX);
        final List<AxisValue<Object>> xAxisValues = Objects.nonNull(xAxis) ? xAxis.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> xAxisTwoValues = Objects.nonNull(xAxisTwo) ? xAxisTwo.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> xAxisThreeValues = Objects.nonNull(xAxisThree) ? xAxisThree.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> xAxisFourValues = Objects.nonNull(xAxisFour) ? xAxisFour.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> yAxisOneValues = Objects.nonNull(yAxisOne) ? yAxisOne.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> yAxisTwoValues = Objects.nonNull(yAxisTwo) ? yAxisTwo.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> yAxisThreeValues = Objects.nonNull(yAxisThree) ? yAxisThree.getAxisValues() : Collections.emptyList();
        final List<AxisValue<Object>> yAxisFourValues = Objects.nonNull(yAxisFour) ? yAxisFour.getAxisValues() : Collections.emptyList();

        final String xAxisFunction = xAxis.getReportField().getFunctionName();
        final String dataLabel = Objects.nonNull(xAxisFunction) ? xAxisFunction : xAxis.getReportField().getLabel();

        graphMeta.put(CRMViewPropertyKeys.Y_AXIS_LABEL, getMetaInfo(orgId, yAxisTwo));
        graphMeta.put(CRMViewPropertyKeys.X_AXIS_LABEL, getMetaInfo(orgId, xAxis));

        if (xAxisValues.isEmpty() && xAxisTwoValues.isEmpty()
            && xAxisThreeValues.isEmpty() && xAxisFourValues.isEmpty()) {
            return buildEmptyGraphResponse(graphMeta, true);
        }

        final ReportAxisContext contextOne = new ReportAxisContext(FIRST_INDEX, orgId, xAxis, yAxisOne, crmUserMap, displayValueResolver, i18nResolver);
        final ReportAxisContext contextTwo = new ReportAxisContext(SECOND_INDEX, orgId, xAxisTwo, yAxisTwo, crmUserMap, displayValueResolver, i18nResolver);
        final ReportAxisContext contextThree = new ReportAxisContext(THIRD_INDEX, orgId, xAxisThree, yAxisThree, crmUserMap, displayValueResolver, i18nResolver);
        final ReportAxisContext contextFour = new ReportAxisContext(FOURTH_INDEX, orgId, xAxisFour, yAxisFour, crmUserMap, displayValueResolver, i18nResolver);

        addYAxisGraphData(yAxisOneValues.size(),  xAxisValueByFirstYAxis, graphDataArray, contextOne);
        addYAxisGraphData(yAxisTwoValues.size(), xAxisValueBySecondYAxis, graphDataArray, contextTwo);
        addYAxisGraphData(yAxisThreeValues.size(), xAxisValueByThirdYAxis, graphDataArray, contextThree);
        addYAxisGraphData(yAxisFourValues.size(), xAxisValueByFourthYAxis, graphDataArray, contextFour);

        final Map<Integer, Object> axisDataMap = resolveXAxisValues(List.of(xAxisValueByFirstYAxis, xAxisValueBySecondYAxis, xAxisValueByThirdYAxis, xAxisValueByFourthYAxis));

        if (Objects.nonNull(xAxisFunction)) {
            populateXAxisByLabel(dataLabel, yAxisOneValues, axisDataMap, xAxisList, i18nResolver);
            populateYAxisByLabel(orgId, dataLabel, yAxisOne,
                    axisDataMap, normalizedYArrayOne, displayValueResolver, i18nResolver);
            populateYAxisByLabel(orgId, dataLabel, yAxisTwo,
                    axisDataMap, normalizedYArrayTwo, displayValueResolver, i18nResolver);
            populateYAxisByLabel(orgId, dataLabel, yAxisThree,
                    axisDataMap, normalizedYArrayThree, displayValueResolver, i18nResolver);
            populateYAxisByLabel(orgId, dataLabel, yAxisFour,
                    axisDataMap, normalizedYArrayFour, displayValueResolver, i18nResolver);
        }

        final JsonArray xValuesArray = Json.array(xAxisList);

        addGraphData(graphDataArray, xValuesArray, normalizedYArrayOne, contextOne);
        addGraphData(graphDataArray, xValuesArray, normalizedYArrayTwo, contextTwo);
        addGraphData(graphDataArray, xValuesArray, normalizedYArrayThree, contextThree);
        addGraphData(graphDataArray, xValuesArray, normalizedYArrayFour, contextFour);

        response.put(CRMViewPropertyKeys.GRAPH_META, graphMeta);
        response.put(CRMViewPropertyKeys.GRAPH_DATA, graphDataArray);
        return response;
    }

    /**
     * <p>
     * Builds and appends graph data points to the provided {@link JsonArray}
     * </p>
     *
     * @param graphDataArray        the {@link JsonArray} to which constructed graph data will be appended
     * @param xValuesArray          the {@link JsonArray} containing raw X-axis values to be resolved
     * @param normalizedYArray      the {@link JsonArray} containing normalized Y-axis values aligned
     * @param context               the {@link ReportAxisContext} containing all axis-related configuration
     *
     */
    private void addGraphData(final @NonNull JsonArray graphDataArray,
                              final @NonNull JsonArray xValuesArray,
                              final @NonNull JsonArray normalizedYArray,
                              final @NonNull ReportAxisContext context) {
        final int xValueSize = xValuesArray.size();
        final int normalizedYArraySize = normalizedYArray.size();

        final Long xAxisColumnId = context.getXAxisColumnId();
        final Long yAxisColumnId = context.getYAxisColumnId();

        final Long orgId = context.orgId();
        final Map<Long, CrmUser> crmUserMap = context.crmUserMap();
        final I18nResolver i18nResolver = context.i18nResolver();
        final DisplayValueResolver resolver = context.displayValueResolver();

        final GraphAxis<Object> xAxis = context.xAxis();
        final GraphAxis<Object> yAxis = context.yAxis();

        final String xAxisLabel = context.getXAxisLabel();
        final String yAxisLabel = context.getYAxisLabel();

        final Object resolvedYValue = resolveNullableDisplayValue(orgId, getNullableUserValue(yAxisLabel, yAxis, crmUserMap),
                yAxisColumnId, resolver, i18nResolver);
        final Object toolTipXKey = resolveNullableDisplayValue(orgId, xAxisLabel, xAxisColumnId, resolver, i18nResolver);

        for (int index = ZERO_INDEX; index < xValueSize; index++) {
            final JsonObject graphDataJson = Json.object();
            final JsonArray toolTipArray = Json.array();

            final Object resolvedXValue = resolveAxisDisplayValue(orgId, xValuesArray.get(index), xAxis, crmUserMap,
                    xAxisColumnId, resolver, i18nResolver);
            final Object resolvedHValue = resolveMetricDisplayValue(orgId,
                    getNullableUserValue(normalizedYArraySize > index ? normalizedYArray.get(index) : null, yAxis, crmUserMap),
                    yAxisColumnId, resolver, i18nResolver);

            buildGraphDataJson(resolvedXValue, resolvedYValue, resolvedHValue, graphDataJson);

            toolTipArray.put(Json.object()
                    .put(CRMReportKeys.KEY, toolTipXKey)
                    .put(CRMReportKeys.VALUE, resolvedXValue));
            toolTipArray.put(Json.object()
                    .put(CRMReportKeys.KEY, resolvedYValue)
                    .put(CRMReportKeys.VALUE, resolvedHValue));

            graphDataJson.put(CRMReportKeys.TOOLTIP, toolTipArray);

            if (context.metricOrder() > ZERO_INDEX) {
                graphDataJson.put(CRMViewPropertyKeys.UNIQUE_ID, context.metricOrder());
            }

            graphDataArray.put(graphDataJson);
        }
    }


    /**
     * <p>
     * Adds Y-axis based graph data points to the given {@link JsonArray}.
     * </p>
     *
     * @param yAxisValuesSize       number of Y-axis values to process
     * @param xAxisDataMap          optional map for storing raw X-axis values by index (populated if non-null)
     * @param graphDataArray        the {@link JsonArray} to which constructed graph data will be appended
     * @param context               the {@link ReportAxisContext} containing all axis-related configuration
     */
    private void addYAxisGraphData(final int yAxisValuesSize,
                                   final Map<Integer, Object> xAxisDataMap,
                                   final @NonNull JsonArray graphDataArray,
                                   final @NonNull ReportAxisContext context) {
        final Long orgId = context.orgId();
        final Map<Long, CrmUser> crmUserMap = context.crmUserMap();
        final I18nResolver i18nResolver = context.i18nResolver();

        final List<AxisValue<Object>> xAxisValues = context.getXAxisValues();
        final List<AxisValue<Object>> yAxisValues = context.getYAxisValues();

        final Long xAxisColumnId = context.getXAxisColumnId();
        final Long yAxisColumnId = context.getYAxisColumnId();

        final GraphAxis<Object> xAxis = context.xAxis();
        final GraphAxis<Object> yAxis = context.yAxis();

        final String xAxisFunction = context.getXAxisFunction();
        final String xAxisLabel = context.getXAxisLabel();
        final String yAxisLabel = context.getYAxisLabel();
        final DisplayValueResolver resolver = context.displayValueResolver();

        final Object resolvedYValue = resolveNullableDisplayValue(orgId, yAxisLabel,
                yAxisColumnId, resolver, i18nResolver);

        for (int index = ZERO_INDEX; index < yAxisValuesSize; index++) {
            final JsonObject graphDataJson = Json.object();
            final JsonArray toolTipArray = Json.array();
            final JsonObject toolTipXValues = Json.object();
            final JsonObject toolTipYValues = Json.object();

            final Object xValue;

            if (FIRST_INDEX == yAxisValuesSize && xAxisValues.isEmpty() && Objects.isNull(xAxisFunction)) {
                break;
            }

            xValue = (FIRST_INDEX == yAxisValuesSize && xAxisValues.isEmpty())
                    ? ZERO_INDEX
                    : xAxisValues.get(index).getValue();

            final Object resolvedXValue = resolveAxisDisplayValue(orgId, xValue, xAxis, crmUserMap, xAxisColumnId,
                    resolver, i18nResolver);

            final Object resolvedHValue = resolveMetricDisplayValue(orgId, getNullableUserValue(yAxisValues.get(index).getValue(), yAxis, crmUserMap),
                    yAxisColumnId, resolver, i18nResolver);

            if (Objects.isNull(xAxisFunction)) {
                buildGraphDataJson(resolvedXValue, resolvedYValue, resolvedHValue, graphDataJson);

                toolTipXValues.put(CRMReportKeys.KEY, xAxisLabel);
                toolTipXValues.put(CRMReportKeys.VALUE, resolvedXValue);
                toolTipYValues.put(CRMReportKeys.KEY, yAxisLabel);
                toolTipYValues.put(CRMReportKeys.VALUE, resolvedHValue);
                toolTipArray.put(toolTipXValues);
                toolTipArray.put(toolTipYValues);

                graphDataJson.put(CRMReportKeys.TOOLTIP, toolTipArray);

                if (context.metricOrder() > ZERO_INDEX) {
                    graphDataJson.put(CRMViewPropertyKeys.UNIQUE_ID, context.metricOrder());
                }

                graphDataArray.put(graphDataJson);
            }

            xAxisDataMap.put(index, xValue);
        }
    }


    /**
     * Populates the given {@link JsonObject} with graph data values for the X, Y, and H axes.
     *
     * @param xValue         the resolved value to be used for the X-axis
     * @param yValue         the resolved value to be used for the Y-axis
     * @param hValue         the resolved value to be used for the metric or height
     * @param graphDataJson  the {@link JsonObject} to which the axis values will be added
     */
    private void buildGraphDataJson(final Object xValue,
                                    final Object yValue,
                                    final Object hValue,
                                    final @NonNull JsonObject graphDataJson) {
        graphDataJson.put(CRMViewPropertyKeys.X, xValue);
        graphDataJson.put(CRMViewPropertyKeys.Y, yValue);

        if (Objects.nonNull(hValue)){
            graphDataJson.put(CRMViewPropertyKeys.H, hValue);
        }
    }

    /**
     * <p>
     * Builds and appends a single graph data point to the provided {@link JsonArray}.
     * </p>
     *
     * @param orgId                 the organization ID used for display value resolution
     * @param crmUserMap            map of CRM user IDs to {@link CrmUser} objects for user resolution
     * @param resolvedX1Value       the resolved value for the first X-axis
     * @param resolvedX2Value       the resolved value for the second X-axis
     * @param yValue                the metric (H-axis) value
     * @param xOneAxis              the {@link GraphAxis} object representing the X-axis one
     * @param xTwoAxis              the {@link GraphAxis} object representing the X-axis two
     * @param yAxis                 the {@link GraphAxis} object representing the Y-axis
     * @param graphDataArray        the {@link JsonArray} to which the constructed graph data will be appended
     * @param displayValueResolver  resolver used to transform raw values into display values
     * @param i18nResolver          internationalization resolver for localized display values
     */
    private void buildGraphData(final @NonNull Long orgId,
                                final @NonNull Map<Long, CrmUser> crmUserMap,
                                final Object resolvedX1Value,
                                final Object resolvedX2Value,
                                final Object yValue,
                                final @NonNull GraphAxis<Object> xOneAxis,
                                final @NonNull GraphAxis<Object> xTwoAxis,
                                final @NonNull GraphAxis<Object> yAxis,
                                final @NonNull JsonArray graphDataArray,
                                final @NonNull DisplayValueResolver displayValueResolver,
                                final @NonNull I18nResolver i18nResolver) {
        final Long xOneColumnId = xOneAxis.getReportField().getColumnId();
        final Long xTwoColumnId = xTwoAxis.getReportField().getColumnId();
        final Long yColumnId = yAxis.getReportField().getColumnId();

        final JsonObject graphDataJson = Json.object()
                .put(CRMViewPropertyKeys.X, resolveNullableDisplayValue(orgId, resolvedX1Value, xOneColumnId,
                        displayValueResolver, i18nResolver))
                .put(CRMViewPropertyKeys.Y, resolveNullableDisplayValue(orgId, resolvedX2Value,
                        xTwoColumnId, displayValueResolver, i18nResolver))
                .put(CRMViewPropertyKeys.H, Objects.isNull(yValue) ? ZERO_INDEX : resolveMetricDisplayValue(orgId,
                        yValue, yColumnId, displayValueResolver, i18nResolver));
        final JsonArray toolTip = Json.array()
                .put(Json.object().put(CRMReportKeys.KEY, xOneAxis.getReportField().getLabel())
                        .put(CRMReportKeys.VALUE, resolveNullableDisplayValue(orgId, resolvedX1Value, xOneColumnId,
                                displayValueResolver, i18nResolver)))
                .put(Json.object().put(CRMReportKeys.KEY, xTwoAxis.getReportField().getLabel())
                        .put(CRMReportKeys.VALUE, resolveNullableDisplayValue(orgId, resolvedX2Value,
                                xTwoColumnId, displayValueResolver, i18nResolver)))
                .put(Json.object().put(CRMReportKeys.KEY, yAxis.getReportField().getLabel())
                        .put(CRMReportKeys.VALUE, Objects.isNull(yValue) ? ZERO_INDEX : resolveMetricDisplayValue(orgId,
                                getNullableUserValue(yValue, yAxis, crmUserMap), yColumnId, displayValueResolver, i18nResolver)));

        graphDataJson.put(CRMReportKeys.TOOLTIP, toolTip);
        graphDataArray.put(graphDataJson);
    }


    /**
     * <p>
     * Finds the corresponding Y-axis value in a list of key-value pairs based on a matching key.
     * </p>
     *
     * @param valueMap the map of key-value pairs to search
     * @param matchKey the key to look for in the map
     * @return the value associated with the matching key, or null if not found
     */
    private Object findYValue(final @NonNull Map<String, Object> valueMap, final @NonNull Object matchKey) {
        return valueMap.get(String.valueOf(matchKey));
    }

    /**
     * Builds a standard empty graph response with X/Y (and optional H).
     *
     * @param graphMeta      the graph metadata
     * @param includesHValue  whether to include H in the structure
     * @return a {@link JsonObject} containing the empty graph response
     */
    private JsonObject buildEmptyGraphResponse(final @NonNull JsonObject graphMeta, final boolean includesHValue) {
        final JsonObject response = Json.object();
        final JsonObject graphDataJson = Json.object();

        graphDataJson.put(CRMViewPropertyKeys.X, Json.array());
        graphDataJson.put(CRMViewPropertyKeys.Y, Json.array());

        if (includesHValue) {
            graphDataJson.put(CRMViewPropertyKeys.H, Json.array());
        }

        response.put(CRMViewPropertyKeys.GRAPH_META, graphMeta);
        response.put(CRMViewPropertyKeys.GRAPH_DATA, Json.array().put(graphDataJson));
        return response;
    }

    /**
     * Holds axis-related configuration for report data conversion, reducing parameter passing.
     *
     * @param orgId                the organization ID
     * @param xAxis                the X-axis graph axis
     * @param yAxis                the Y-axis graph axis
     * @param crmUserMap           mapping of CRM user IDs to {@link CrmUser}
     * @param displayValueResolver resolver for formatting display values
     * @param i18nResolver         resolver for localized display values
     */
    private record ReportAxisContext(int metricOrder,
                                     Long orgId,
                                     GraphAxis<Object> xAxis,
                                     GraphAxis<Object> yAxis,
                                     Map<Long, CrmUser> crmUserMap,
                                     DisplayValueResolver displayValueResolver,
                                     I18nResolver i18nResolver) {

        Long getXAxisColumnId() {
            return xAxis.getReportField().getColumnId();
        }

        Long getYAxisColumnId() {
            return yAxis.getReportField().getColumnId();
        }

        String getXAxisLabel() {
            return xAxis.getReportField().getLabel();
        }

        String getYAxisLabel() {
            return yAxis.getReportField().getLabel();
        }

        String getXAxisFunction() {
            return xAxis.getReportField().getFunctionName();
        }

        List<AxisValue<Object>> getXAxisValues() {
            return Objects.nonNull(xAxis) ? xAxis.getAxisValues() : Collections.emptyList();
        }

        List<AxisValue<Object>> getYAxisValues() {
            return Objects.nonNull(yAxis) ? yAxis.getAxisValues() : Collections.emptyList();
        }
    }

    private boolean isBlankAxisValue(final Object value) {
        return Objects.isNull(value) || value instanceof String && ((String) value).trim().isEmpty();
    }

}
