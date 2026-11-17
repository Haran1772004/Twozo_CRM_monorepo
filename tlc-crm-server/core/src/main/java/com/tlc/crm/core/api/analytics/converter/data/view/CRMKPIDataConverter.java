package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorSide;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorTimeUnit;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorType;
import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.api.analytics.general.keys.layout.CRMKPIKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

import java.util.List;
import java.util.Map;

/**
 * Converts {@link com.tlc.analytics.config.model.view.report.KPIView} graph axis data
 * into JSON responses containing current value, previous value, percentage indicator,
 * and axis metadata for KPI chart rendering.
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class CRMKPIDataConverter extends AbstractViewDataConverter {
    private static final double ZERO_DOUBLE = 0.0;
    private static final double FULL_PERCENT = 100.0;
    private static final String Y_AXIS_DISPLAY_NAME = Axis.Y_AXIS.getDisplayName();
    private static final String INDICATOR_FORMAT = "%.2f%%";

    /**
     * Builds the KPI view JSON response from the graph axis data.
     * <p>
     * Iterates over the graph axes to extract the current (LEFT) and previous (RIGHT)
     * comparator values, computes the percentage change indicator, and assembles the
     * metadata and data sections of the response.
     * </p>
     *
     * @param orgId        the organization ID
     * @param graphAxis    the list of {@link GraphAxis} containing KPI comparator data
     * @param crmUserMap   the map of user IDs to {@link CrmUser} (unused in KPI, required by contract)
     * @param i18nResolver the resolver for localizing time unit labels
     * @return the KPI view {@link JsonObject} response
     */
    @Override
    public JsonObject getData(final @NonNull Long orgId,
                              final @NonNull List<GraphAxis<Object>> graphAxis,
                              final @NonNull Map<Long, CrmUser> crmUserMap,
                              final @NonNull I18nResolver i18nResolver) {
        final JsonObject response = Json.object();
        final JsonArray yAxisArray = Json.array();
        final JsonObject kpiJson = Json.object();
        double currentValue = ZERO_DOUBLE;
        double previousValue = ZERO_DOUBLE;
        boolean metaPopulated = false;

        for (final GraphAxis<Object> axisObject : graphAxis) {
            if (!metaPopulated && NumericalConstant.ZERO < axisObject.getTotalRecords()) {
                yAxisArray.put(getMetaInfo(orgId, axisObject));
                metaPopulated = true;
            }

            final KPIComparatorSide comparatorSide = axisObject.getComparatorSide();
            final List<AxisValue<Object>> axisValues = axisObject.getAxisValues();

            if (axisValues.isEmpty() && KPIComparatorSide.RIGHT == comparatorSide) {
                previousValue = ZERO_DOUBLE;

                kpiJson.put(CRMKPIKeys.PREVIOUS_VALUE,
                        buildPreviousValueJson(NumericalConstant.ZERO, axisObject.getOperationType(), axisObject.getComparatorType(), i18nResolver));
                continue;
            }

            if (KPIComparatorSide.RIGHT == comparatorSide) {
                final String timeUnitLabel = resolveTimeUnitLabel(axisObject.getOperationType(), axisObject.getComparatorType(), i18nResolver);

                for (final AxisValue<Object> axisValue : axisValues) {
                    final Object value = resolveValue(axisValue);

                    previousValue = NumberUtils.parseNumber(value).doubleValue();

                    final JsonObject jsonObject = Json.object();

                    jsonObject.put(CRMKPIKeys.VALUE, value);
                    jsonObject.put(CRMKPIKeys.TIME_UNIT, timeUnitLabel);
                    kpiJson.put(CRMKPIKeys.PREVIOUS_VALUE, jsonObject);
                }
            } else if (KPIComparatorSide.LEFT == comparatorSide) {
                for (final AxisValue<Object> axisValue : axisValues) {
                    final Object value = resolveValue(axisValue);

                    currentValue = NumberUtils.parseNumber(value).doubleValue();
                    kpiJson.put(CRMKPIKeys.CURRENT_VALUE, value);
                }
            }
        }

        if (ZERO_DOUBLE == previousValue && ZERO_DOUBLE == currentValue) {
            response.put(CRMViewPropertyKeys.GRAPH_META, Json.emptyObject());
            response.put(CRMViewPropertyKeys.GRAPH_DATA, Json.emptyObject());

            return response;
        }

        final JsonObject yAxisJson = Json.object();
        final JsonObject dataJson = Json.object();
        final JsonArray dataArray = Json.array();

        yAxisJson.put(Y_AXIS_DISPLAY_NAME, yAxisArray);
        kpiJson.put(CRMKPIKeys.INDICATOR, buildIndicatorJson(currentValue, previousValue));
        dataArray.put(kpiJson);
        dataJson.put(Y_AXIS_DISPLAY_NAME, dataArray);

        response.put(CRMViewPropertyKeys.GRAPH_META, yAxisJson);
        response.put(CRMViewPropertyKeys.GRAPH_DATA, dataJson);

        return response;
    }

    /**
     * Resolves the axis value, returning 0 if the value is null.
     *
     * @param axisValue the axis value to resolve
     * @return the resolved value (original or 0 if null)
     */
    private Object resolveValue(final AxisValue<Object> axisValue) {
        final Object kpiValue = axisValue.getValue();

        return kpiValue != null ? kpiValue : NumericalConstant.ZERO;
    }

    /**
     * Builds the "previous value" JSON object containing the value and its
     * localized time unit label.
     *
     * @param value         the previous value
     * @param operationType the operation type used to determine the time unit
     * @param i18nResolver  the internationalization resolver
     * @return the populated {@link JsonObject}
     */
    private JsonObject buildPreviousValueJson(final Object value,
                                              final OperationType operationType,
                                              final KPIComparatorType comparatorType,
                                              final I18nResolver i18nResolver) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(CRMKPIKeys.VALUE, value);
        jsonObject.put(CRMKPIKeys.TIME_UNIT, resolveTimeUnitLabel(operationType, comparatorType, i18nResolver));

        return jsonObject;
    }

    /**
     * Resolves the localized time unit label for the given operation type's
     * previous period.
     *
     * @param operationType the current operation type
     * @param i18nResolver  the internationalization resolver
     * @return the localized time unit display string
     */
    private String resolveTimeUnitLabel(final OperationType operationType,
                                        final KPIComparatorType comparatorType,
                                        final I18nResolver i18nResolver) {
        return i18nResolver.get(getPreviousTimeUnit(operationType, comparatorType).getI18nKeyOfDisplayName());
    }

    /**
     * Builds the percentage change indicator JSON containing the formatted value
     * and an optional "up"/"down" symbol.
     *
     * @param currentValue  the current KPI value
     * @param previousValue the previous KPI value
     * @return the indicator {@link JsonObject}
     */
    private JsonObject buildIndicatorJson(final double currentValue, final double previousValue) {
        final JsonObject indicatorJson = Json.object();
        final double indicatorValue;

        if (ZERO_DOUBLE == previousValue) {
            indicatorValue = ZERO_DOUBLE == currentValue ? ZERO_DOUBLE : FULL_PERCENT;
        } else {
            indicatorValue = ((currentValue - previousValue) / previousValue) * FULL_PERCENT;
        }

        if (ZERO_DOUBLE != indicatorValue) {
            indicatorJson.put(CRMKPIKeys.SYMBOL, indicatorValue > ZERO_DOUBLE ? "up" : "down");
        }

        indicatorJson.put(CRMKPIKeys.VALUE, String.format(INDICATOR_FORMAT, indicatorValue));

        return indicatorJson;
    }

    /**
     * Maps a current time-period operation type to its corresponding previous
     * time-period operation type.
     *
     * @param operationType the current {@link OperationType}
     * @return the corresponding previous period {@link OperationType}
     * @throws com.tlc.commons.code.ErrorCode if the operation type is unsupported
     */
    private KPIComparatorTimeUnit getPreviousTimeUnit(final OperationType operationType, final KPIComparatorType comparatorType) {
        return switch (operationType) {
            case TODAY -> switch (comparatorType) {
                case PREVIOUS -> KPIComparatorTimeUnit.YESTERDAY;
                case PREVIOUS_RELATIVE -> KPIComparatorTimeUnit.PREVIOUS_DAY_LAST_YEAR;
                case SAME_DAY_PREVIOUS_YEAR -> KPIComparatorTimeUnit.SAME_PERIOD_LAST_YEAR;
                case CUSTOM -> KPIComparatorTimeUnit.CUSTOM;
            };
            case THIS_WEEK -> switch (comparatorType) {
                case PREVIOUS -> KPIComparatorTimeUnit.LAST_WEEK;
                case PREVIOUS_RELATIVE -> KPIComparatorTimeUnit.PREVIOUS_WEEK_LAST_YEAR;
                case SAME_DAY_PREVIOUS_YEAR -> KPIComparatorTimeUnit.SAME_PERIOD_LAST_YEAR;
                case CUSTOM -> KPIComparatorTimeUnit.CUSTOM;
            };
            case THIS_MONTH -> switch (comparatorType) {
                case PREVIOUS -> KPIComparatorTimeUnit.LAST_MONTH;
                case PREVIOUS_RELATIVE -> KPIComparatorTimeUnit.PREVIOUS_MONTH_LAST_YEAR;
                case SAME_DAY_PREVIOUS_YEAR -> KPIComparatorTimeUnit.SAME_PERIOD_LAST_YEAR;
                case CUSTOM -> KPIComparatorTimeUnit.CUSTOM;
            };
            case THIS_YEAR -> switch (comparatorType) {
                case PREVIOUS, PREVIOUS_RELATIVE -> KPIComparatorTimeUnit.LAST_YEAR;
                case SAME_DAY_PREVIOUS_YEAR -> KPIComparatorTimeUnit.SAME_PERIOD_LAST_YEAR;
                case CUSTOM -> KPIComparatorTimeUnit.CUSTOM;
            };
            case THIS_QUARTER -> switch (comparatorType) {
                case PREVIOUS -> KPIComparatorTimeUnit.LAST_QUARTER;
                case PREVIOUS_RELATIVE -> KPIComparatorTimeUnit.PREVIOUS_QUARTER_LAST_YEAR;
                case SAME_DAY_PREVIOUS_YEAR -> KPIComparatorTimeUnit.SAME_PERIOD_LAST_YEAR;
                case CUSTOM -> KPIComparatorTimeUnit.CUSTOM;
            };
            case CUSTOM -> KPIComparatorTimeUnit.CUSTOM;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_OPERATION_TYPE,
                    "i18n.analytics.invalid.operation.type");
        };
    }
}