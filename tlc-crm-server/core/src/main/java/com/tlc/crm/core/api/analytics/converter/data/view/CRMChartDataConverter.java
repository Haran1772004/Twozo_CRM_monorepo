package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.Counter;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.analytics.converter.data.DisplayValueResolver;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * <p>
 * Provides different ways to convert the {@link com.tlc.analytics.config.model.view.report.ChartView} details into {@link JsonObject}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class CRMChartDataConverter extends AbstractViewDataConverter
{
    private static final String DEFAULT_VALUE = "Null";
    private final DisplayValueResolver displayValueResolver;
    private final ColumnDetailsService columnDetailsService;

    protected CRMChartDataConverter()
    {
        this.displayValueResolver = DisplayValueResolver.INSTANCE();
        this.columnDetailsService = ColumnDetailsService.getInstance();
    }

    private static class Instance
    {
        private static final CRMChartDataConverter INSTANCE = new CRMChartDataConverter();
    }

    /**
     * Gets the {@link CRMChartDataConverter} instance
     *
     * @return {@link CRMChartDataConverter}
     */
    public static CRMChartDataConverter INSTANCE()
    {
        return CRMChartDataConverter.Instance.INSTANCE;
    }

    /**
     * Builds the chart response JSON object for the given organization and chart configuration.
     * <p>
     * Depending on the number of X-axis fields configured in {@code graphAxis}, this method decides
     * whether to build a single "view by" chart response or a multi "view by" chart response.
     * </p>
     *
     * @param orgId        the ID of the organization
     * @param graphAxis    the list of {@link GraphAxis} objects containing report field and axis data
     * @param crmUserMap   the map of user IDs to {@link CrmUser} objects, used for resolving user display names
     * @param i18nResolver the resolver for localizing display values
     * @return the chart view {@link JsonObject} response
     */
    @Override
    public JsonObject getData(final @NonNull Long orgId,
                              final @NonNull List<GraphAxis<Object>> graphAxis,
                              final @NonNull Map<Long, CrmUser> crmUserMap,
                              final @NonNull I18nResolver i18nResolver) {
        final int xAxisCount = Math.toIntExact(graphAxis.stream()
                .filter(axis -> Objects.equals(Axis.X_AXIS, axis.getReportField().getAxis()))
                .count());

        return Objects.equals(NumericalConstant.ONE, xAxisCount) ? buildSingleViewByChartResponse(orgId, graphAxis, crmUserMap, i18nResolver)
                : buildMultiViewByChartResponse(orgId, graphAxis, crmUserMap, i18nResolver);
    }

    /**
     * Builds a chart response when the chart has a single X-axis ("view by").
     * <p>
     * This method prepares the X-axis metadata, resolves display values (including user names if applicable),
     * and constructs the Y-axis series data aligned to the X-axis values.
     * </p>
     *
     * @param orgId        the ID of the organization
     * @param graphAxis    the list of {@link GraphAxis} objects containing X and Y axis data
     * @param crmUserMap   the map of user IDs to {@link CrmUser} objects, used for resolving user display names
     * @param i18nResolver the resolver for localizing display values
     * @return the chart view {@link JsonObject} response for a single view-by chart
     */
    private JsonObject buildSingleViewByChartResponse(final @NonNull Long orgId,
                                                      final @NonNull List<GraphAxis<Object>> graphAxis,
                                                      final @NonNull Map<Long, CrmUser> crmUserMap,
                                                      final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonObject dataGraphJson = Json.object();
        final JsonObject graphMetaJson = Json.object();
        final JsonArray yJsonArray = Json.array();
        final JsonArray yMetaJsonArray = Json.array();

        final Collection<Object> xAxisList = new ArrayList<>();
        final Set<Integer> missingValueIndices = new HashSet<>();
        final Map<Integer, Object> xAxisDataMap = new LinkedHashMap<>();

        final Counter yAxisCounter = new Counter();

        final GraphAxis<Object> xAxis = findAxis(NumericalConstant.ONE, graphAxis, Axis.X_AXIS);
        final GraphAxis<Object> firstYAxis = findAxis(NumericalConstant.ONE, graphAxis, Axis.Y_AXIS);
        final GraphAxis<Object> secondYAxis = findAxis(NumericalConstant.TWO, graphAxis, Axis.Y_AXIS);
        final GraphAxis<Object> thirdYAxis = findAxis(NumericalConstant.THREE, graphAxis, Axis.Y_AXIS);
        final GraphAxis<Object> fourthYAxis = findAxis(NumericalConstant.FOUR, graphAxis, Axis.Y_AXIS);

        final ReportField xAxisReportField = xAxis.getReportField();
        final String functionName = xAxisReportField.getFunctionName();
        final String xAxisLabel = Objects.nonNull(functionName) ? functionName : xAxisReportField.getLabel();
        final String xAxisDisplayName = Axis.X_AXIS.getDisplayName();
        final String yAxisDisplayName = Axis.Y_AXIS.getDisplayName();

        for ( final GraphAxis<Object> axisObject : graphAxis )
        {
            final ReportField reportField = axisObject.getReportField();
            final Axis axis = reportField.getAxis();
            final JsonArray dataArray = Json.array();

            if ( Objects.equals( Axis.X_AXIS, axis) )
            {
                final List<AxisValue<Object>> axisValues = axisObject.getAxisValues();
                final Long columnId = reportField.getColumnId();
                final Object rowValue = axisObject.getColumnMap().get( columnId );

                if ( rowValue instanceof Long && !crmUserMap.isEmpty() )
                {

                    for ( final AxisValue<Object> axisValue : axisValues )
                    {
                        final Object axisVal = axisValue.getValue();
                        final CrmUser crmUser = crmUserMap.get( NumberUtils.parseNumber( axisVal ).longValue() );
                        final Object name = crmUser != null ? crmUser.getFullName() : axisVal;

                        xAxisDataMap.put( xAxisDataMap.size(), name );
                    }
                }
                else
                {
                    if ( Objects.isNull( axisValues ) )
                    {
                        xAxisDataMap.put( NumericalConstant.ZERO, DEFAULT_VALUE );
                        missingValueIndices.add( NumericalConstant.ZERO );
                    }
                    else
                    {
                        int axisIndex = NumericalConstant.ZERO;

                        for ( final AxisValue<Object> axisValue : axisValues )
                        {
                            final Object objectValue = axisValue.getValue();

                            if ( Objects.nonNull(objectValue) )
                            {
                                final Object display = displayValueResolver.resolveDisplayValue( orgId, objectValue, columnId, i18nResolver );

                                xAxisDataMap.put( axisIndex, display );
                            }
                            else
                            {
                                missingValueIndices.add( axisIndex );
                            }

                            axisIndex++;
                        }
                    }
                }
            }

            if ( Objects.equals( Axis.Y_AXIS, axis ) )
            {
                yAxisCounter.increment();
            }

            final int count = yAxisCounter.getCount();

            if (Objects.equals(Axis.X_AXIS, axis)) {
                populateXAxisByLabel(xAxisLabel, firstYAxis.getAxisValues(), xAxisDataMap, xAxisList, i18nResolver);
                dataGraphJson.put(xAxisDisplayName, Json.array(xAxisList));
            }

            if (Objects.equals(Axis.Y_AXIS, axis)) {
                final List<GraphAxis<Object>> yAxes = new ArrayList<>();

                if (Objects.nonNull(firstYAxis)) {
                    yAxes.add(firstYAxis);
                }

                if (Objects.nonNull(secondYAxis)) {
                    yAxes.add(secondYAxis);
                }

                if (Objects.nonNull(thirdYAxis)) {
                    yAxes.add(thirdYAxis);
                }

                if (Objects.nonNull(fourthYAxis)) {
                    yAxes.add(fourthYAxis);
                }

                if (count > NumericalConstant.ZERO && count <= yAxes.size()) {
                    populateYAxisByLabel(orgId, xAxisLabel, yAxes.get(count - NumericalConstant.ONE), xAxisDataMap, dataArray, displayValueResolver, i18nResolver);
                    yJsonArray.put(dataArray);
                }
            }
        }

        yMetaJsonArray.put(getMetaInfo(orgId,firstYAxis));

        if (Objects.nonNull(secondYAxis)) {
            yMetaJsonArray.put(getMetaInfo(orgId, secondYAxis));
        }

        if (Objects.nonNull(thirdYAxis)) {
            yMetaJsonArray.put(getMetaInfo(orgId, thirdYAxis));
        }

        if (Objects.nonNull(fourthYAxis)) {
            yMetaJsonArray.put(getMetaInfo(orgId, fourthYAxis));
        }

        graphMetaJson.put(xAxisDisplayName, getMetaInfo(orgId, xAxis));
        graphMetaJson.put(yAxisDisplayName, yMetaJsonArray);

        dataGraphJson.put(yAxisDisplayName, yJsonArray);

        response.put( CRMViewPropertyKeys.GRAPH_META, graphMetaJson );
        response.put( CRMViewPropertyKeys.GRAPH_DATA, dataGraphJson );
        return response;
    }

    /**
     * Builds the JSON response for a chart that uses two X-axes (multi-view by) and a single Y-axis.
     *
     * @param orgId        the organization ID
     * @param graphAxes    list of graph axes (X1, X2, Y)
     * @param crmUserMap   mapping of CRM user IDs to user details
     * @param i18nResolver resolver for localized display values
     * @return JSON response containing metadata and grouped chart data
     */
    private JsonObject buildMultiViewByChartResponse(final Long orgId,
                                                     final List<GraphAxis<Object>> graphAxes,
                                                     final Map<Long, CrmUser> crmUserMap,
                                                     final I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonObject dataGraphJson = Json.object();
        final JsonObject graphData = Json.object();

        GraphAxis<Object> yAxis = null;
        GraphAxis<Object> xAxisPrimary = null;
        GraphAxis<Object> xAxisSecondary = null;

        for (final GraphAxis<Object> axis : graphAxes) {
            final ReportField reportField = axis.getReportField();

            if (Objects.equals(Axis.Y_AXIS, reportField.getAxis())) {
                yAxis = axis;
            } else if (Objects.equals(NumericalConstant.ONE, Math.toIntExact(reportField.getOrder()))) {
                xAxisPrimary = axis;
            } else {
                xAxisSecondary = axis;
            }
        }

        final List<AxisValue<Object>> yAxisValues = Objects.isNull(yAxis) ? Collections.emptyList() : yAxis.getAxisValues();
        final List<AxisValue<Object>> primaryXValues = Objects.isNull(xAxisPrimary) ? Collections.emptyList() : xAxisPrimary.getAxisValues();
        final List<AxisValue<Object>> secondaryXValues = Objects.isNull(xAxisSecondary) ? Collections.emptyList() : xAxisSecondary.getAxisValues();

        final Collection<Object> uniquePrimaryXValues = primaryXValues.stream()
                .map(AxisValue::getValue)
                .distinct()
                .toList();
        final Collection<Object> uniqueSecondaryXValues = secondaryXValues.stream()
                .map(AxisValue::getValue)
                .distinct()
                .toList();

        final Map<Object, Map<Object, Object>> yValueLookup = new HashMap<>();
        final Map<Integer, Object> xAxisDataMap = new HashMap<>();
        final List<Object> xAxisList = new ArrayList<>();
        final ReportField xAxisPrimaryReportField = xAxisPrimary.getReportField();
        final String xAxisPrimaryFunction = xAxisPrimaryReportField.getFunctionName();
        final String xAxisSecondaryFunction = xAxisSecondary.getReportField().getFunctionName();
        final String functionName = Objects.nonNull(xAxisPrimaryFunction) ? xAxisPrimaryFunction : xAxisSecondaryFunction;
        final String xAxisDisplayName = Axis.X_AXIS.getDisplayName();
        final String yAxisDisplayName = Axis.Y_AXIS.getDisplayName();
        final int yAxisSize = yAxisValues.size();
        final boolean areXaxisValuesEmpty = primaryXValues.isEmpty() && secondaryXValues.isEmpty();

        if (areXaxisValuesEmpty) {
            graphData.put(xAxisDisplayName, Json.array());
            graphData.put(yAxisDisplayName, Json.array());
            dataGraphJson.put(CRMViewPropertyKeys.GRAPH_DATA, graphData);
            dataGraphJson.put(CRMViewPropertyKeys.GRAPH_META, buildMultiViewByMeta(orgId, Collections.emptyList(),
                    areXaxisValuesEmpty, crmUserMap, xAxisPrimary, xAxisSecondary, yAxis, i18nResolver));

            return response.put(CRMViewPropertyKeys.DATA, dataGraphJson);
        }

        for (int index = NumericalConstant.ZERO; index < yAxisSize; index++) {
            yValueLookup.computeIfAbsent(primaryXValues.get(index).getValue(), key -> new HashMap<>())
                    .put(secondaryXValues.get(index).getValue(), yAxisValues.get(index).getValue());
        }

        if (Objects.isNull(functionName)) {
            final JsonArray yAxisArray = Json.array();

            for (final Object secondaryXValue : uniqueSecondaryXValues) {
                final JsonArray yRow = Json.array();

                for (final Object primaryXValue : uniquePrimaryXValues) {
                    yRow.put(yValueLookup.getOrDefault(primaryXValue, Collections.emptyMap())
                            .getOrDefault(secondaryXValue, NumericalConstant.ZERO));
                }
                yAxisArray.put(yRow);
            }

            final JsonArray xAxisArray = Json.array();
            final Long columnId = xAxisPrimaryReportField.getColumnId();

            for (final Object primaryXValue : uniquePrimaryXValues) {
                xAxisArray.put(displayValueResolver.resolveDisplayValue(orgId,
                        getUserValue(primaryXValue, xAxisPrimary, crmUserMap),
                        columnId, i18nResolver));
            }

            graphData.put(yAxisDisplayName, yAxisArray);
            graphData.put(xAxisDisplayName, xAxisArray);

            dataGraphJson.put(CRMViewPropertyKeys.GRAPH_META,
                    buildMultiViewByMeta(orgId, List.of(), areXaxisValuesEmpty, crmUserMap, xAxisPrimary, xAxisSecondary, yAxis, i18nResolver));
            dataGraphJson.put(CRMViewPropertyKeys.GRAPH_DATA, graphData);

            return response.put(CRMViewPropertyKeys.DATA, dataGraphJson);
        }

        final List<AxisValue<Object>> functionAxisValues = Objects.nonNull(xAxisPrimaryFunction) ? primaryXValues : secondaryXValues;
        final int functionXAxisValueSize = functionAxisValues.size();
        final Long columnId = (Objects.nonNull(xAxisPrimaryFunction) ? xAxisPrimaryReportField
                : xAxisSecondary.getReportField()).getColumnId();

        for (int index = NumericalConstant.ZERO; index < functionXAxisValueSize; index++) {
            final Object functionAxisValue = functionAxisValues.get(index).getValue();

            if (Objects.nonNull(functionAxisValue)) {
                xAxisDataMap.put(index, displayValueResolver.resolveDisplayValue(orgId, functionAxisValue, columnId, i18nResolver));
            }
        }

        populateXAxisByLabel(functionName, yAxisValues, xAxisDataMap, xAxisList, i18nResolver);

        if (Objects.nonNull(xAxisPrimaryFunction)) {
            final JsonArray yValuesArray = Json.array();
            final JsonArray xValuesArray = Json.array();

            for (final Object secondaryXAxisValue : uniqueSecondaryXValues) {
                final JsonArray innerYValues = Json.array();

                for (final Object primaryXAxisValue : uniquePrimaryXValues) {
                    innerYValues.put(yValueLookup.getOrDefault(primaryXAxisValue, Collections.emptyMap())
                            .getOrDefault(secondaryXAxisValue, NumericalConstant.ZERO));
                }

                yValuesArray.put(innerYValues);
            }

            xAxisList.forEach(xValuesArray::put);

            graphData.put(yAxisDisplayName, yValuesArray);
            graphData.put(xAxisDisplayName, xValuesArray);

            dataGraphJson.put(CRMViewPropertyKeys.GRAPH_META,
                    buildMultiViewByMeta(orgId, List.of(), areXaxisValuesEmpty, crmUserMap, xAxisPrimary, xAxisSecondary, yAxis, i18nResolver));
            dataGraphJson.put(CRMViewPropertyKeys.GRAPH_DATA, graphData);
        }

        if (Objects.nonNull(xAxisSecondaryFunction)) {
            final JsonArray yValuesArray = Json.array();
            final JsonArray xValuesArray = Json.array();

            for (final Object secondaryXAxisValue : xAxisList) {
                final JsonArray innerYValues = Json.array();

                for (final Object primaryXAxisValue : uniquePrimaryXValues) {
                    final Map<Object, Object> secondaryXAxisMap = yValueLookup.get(primaryXAxisValue);

                    innerYValues.put(Objects.nonNull(secondaryXAxisMap) && secondaryXAxisMap.containsKey(secondaryXAxisValue)
                            ? secondaryXAxisMap.get(secondaryXAxisValue)
                            : NumericalConstant.ZERO);
                }

                yValuesArray.put(innerYValues);
            }

            final Long primaryColumnId = xAxisPrimaryReportField.getColumnId();

            for (final Object primaryXValue : uniquePrimaryXValues) {
                xValuesArray.put(displayValueResolver.resolveDisplayValue(orgId,
                        getUserValue(primaryXValue, xAxisPrimary, crmUserMap),
                        primaryColumnId, i18nResolver));
            }

            graphData.put(yAxisDisplayName, yValuesArray);
            graphData.put(xAxisDisplayName, xValuesArray);

            dataGraphJson.put(CRMViewPropertyKeys.GRAPH_META,
                    buildMultiViewByMeta(orgId, xAxisList, areXaxisValuesEmpty, crmUserMap, xAxisPrimary, xAxisSecondary, yAxis, i18nResolver));
            dataGraphJson.put(CRMViewPropertyKeys.GRAPH_DATA, graphData);
        }

        return response.put(CRMViewPropertyKeys.DATA, dataGraphJson);
    }

    /**
     * Builds the chart meta information for a multi-view-by chart.
     *
     * <p>Generates axis labels and metadata for the primary X-Axis, secondary X-Axis, and Y-Axis.</p>
     *
     * @param orgId          the organization ID
     * @param crmUserMap     map of CRM users for resolving display values
     * @param xAxisPrimary   the primary X-Axis
     * @param xAxisSecondary the secondary X-Axis
     * @param yAxis          the Y-Axis
     * @param i18nResolver   resolver for internationalization of labels
     * @return the JSON object containing chart meta info
     */
    private JsonObject buildMultiViewByMeta(final Long orgId,
                                            final List<Object> functionFieldsList,
                                            final boolean xAxisValuesIsEmpty,
                                            final Map<Long, CrmUser> crmUserMap,
                                            final GraphAxis<Object> xAxisPrimary,
                                            final GraphAxis<Object> xAxisSecondary,
                                            final GraphAxis<Object> yAxis,
                                            final I18nResolver i18nResolver) {
        final JsonObject graphMeta = Json.object();
        final ReportField yAxisReportField = yAxis.getReportField();
        final ReportField primaryReportField = xAxisPrimary.getReportField();
        final String xAxisLabel = primaryReportField.getLabel();
        final OperationType operationType = primaryReportField.getOperationType();
        final JsonObject primaryXAxisMeta = Json.object();

        primaryXAxisMeta.put(CRMViewPropertyKeys.MEASURE, Objects.equals(OperationType.COUNT, operationType)
                ? OperationType.TOTAL
                : operationType);
        primaryXAxisMeta.put(CRMViewPropertyKeys.DATA_TYPE, getDataType(primaryReportField, columnDetailsService.get(primaryReportField.getColumnId())));
        primaryXAxisMeta.put(CRMViewPropertyKeys.LABEL, xAxisLabel);

        final ReportField secondaryReportField = xAxisSecondary.getReportField();
        final Long secondaryColumnId = secondaryReportField.getColumnId();
        final OperationType secondaryOperationType = yAxisReportField.getOperationType();
        final OperationType secondaryMeasure = Objects.equals(OperationType.COUNT, secondaryOperationType)
                ? OperationType.TOTAL
                : secondaryOperationType;
        final String secondaryDataType = getDataType(secondaryReportField, columnDetailsService.get(secondaryColumnId));
        final JsonArray secondaryXAxisMeta = Json.array();
        final Set<Object> processedValues = new HashSet<>();

        if (!xAxisValuesIsEmpty) {
            if (Objects.isNull(functionFieldsList) || functionFieldsList.isEmpty()) {

                for (final AxisValue<Object> axisValue : xAxisSecondary.getAxisValues()) {
                    final Object value = axisValue.getValue();

                    if (Objects.nonNull(value) && processedValues.add(value)) {
                        final JsonObject secondaryMetaEntry = Json.object();

                        secondaryMetaEntry.put(CRMViewPropertyKeys.MEASURE, secondaryMeasure);
                        secondaryMetaEntry.put(CRMViewPropertyKeys.DATA_TYPE, secondaryDataType);
                        secondaryMetaEntry.put(CRMViewPropertyKeys.LABEL, displayValueResolver.resolveDisplayValue(orgId,
                                getUserValue(value, xAxisSecondary, crmUserMap),
                                secondaryColumnId, i18nResolver));

                        secondaryXAxisMeta.put(secondaryMetaEntry);
                    }
                }

            } else {

                for (final Object axisValue : functionFieldsList) {

                    if (Objects.nonNull(axisValue) && processedValues.add(axisValue)) {
                        final JsonObject secondaryMetaEntry = Json.object();

                        secondaryMetaEntry.put(CRMViewPropertyKeys.MEASURE, secondaryMeasure);
                        secondaryMetaEntry.put(CRMViewPropertyKeys.DATA_TYPE, secondaryDataType);
                        secondaryMetaEntry.put(CRMViewPropertyKeys.LABEL, displayValueResolver.resolveDisplayValue(orgId,
                                getUserValue(axisValue, xAxisSecondary, crmUserMap),
                                secondaryColumnId, i18nResolver));

                        secondaryXAxisMeta.put(secondaryMetaEntry);
                    }
                }
            }
        }

        graphMeta.put(CRMViewPropertyKeys.Y_AXIS_LABEL, yAxisReportField.getLabel());
        graphMeta.put(CRMViewPropertyKeys.X_AXIS_LABEL, xAxisLabel);
        graphMeta.put(Axis.Y_AXIS.getDisplayName(), secondaryXAxisMeta);
        graphMeta.put(Axis.X_AXIS.getDisplayName(), primaryXAxisMeta);

        return graphMeta;
    }

}
