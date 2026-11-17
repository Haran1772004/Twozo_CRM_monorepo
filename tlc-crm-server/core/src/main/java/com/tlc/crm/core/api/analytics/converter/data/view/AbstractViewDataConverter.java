package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.service.column.ColumnDetailsService;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.chart.Axis;
import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.api.analytics.converter.data.DisplayValueResolver;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunction;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMViewPropertyKeys;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.l10n.temporal.model.Temporal;
import lombok.NonNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.Date;

/**
 * <p>
 * Provides an abstract way to convert the view data
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
abstract class AbstractViewDataConverter {
    private static final Long SECOND_INDEX = 2L;
    private final ColumnDetailsService columnDetailsService = ColumnDetailsService.getInstance();

    /**
     * <p>
     * Retrieves the metadata information for a given axis and organization id
     * </p>
     *
     * @param orgId the ID of the organization to which the axis belongs
     * @param axis  the GraphAxis object containing report field information
     * @return a JsonObject containing the metadata details for the specified axis
     */
    public JsonObject getMetaInfo(final @NonNull Long orgId, final @NonNull GraphAxis<Object> axis )
    {
        final JsonObject metaJson = Json.object();
        final ReportField reportField = axis.getReportField();
        final OperationType operationType = reportField.getOperationType();
        final OperationType measure = Objects.equals(OperationType.COUNT, operationType) ? OperationType.TOTAL : operationType;
        final ColumnDetail columnDetail = columnDetailsService.get(reportField.getColumnId());
        final String dataTypeString = getDataType(reportField, columnDetail);

        metaJson.put(CRMViewPropertyKeys.LABEL, (Objects.equals(SECOND_INDEX , reportField.getOrder())
                && Objects.equals( Axis.X_AXIS, reportField.getAxis())) ? StringUtil.EMPTY_STRING : reportField.getLabel());
        metaJson.put( CRMViewPropertyKeys.MEASURE, I18nServiceProvider.get().getResolver( Locale.US )
                .get( measure.getI18nKeyOfDisplayName() ) );
        metaJson.put( CRMViewPropertyKeys.DATA_TYPE, dataTypeString );

        return metaJson;
    }

    /**
     * Determines the data type (String, Numeric, or Date) for a given report field,
     * based on the function applied (if any) and the column's underlying data type.
     *
     * @param reportField    the field being analyzed, including axis and function details
     * @param columnDetail  metadata about the field's actual data type
     * @return               "String", "Numeric", or "Date" depending on the context
     */
    protected static String getDataType(final ReportField reportField, final ColumnDetail columnDetail) {
        final String functionName = reportField.getFunctionName();

        if (Objects.equals(Axis.X_AXIS, reportField.getAxis()) && Objects.nonNull(functionName)) {
            return getFunctionBasedDataType(functionName.toLowerCase());
        }

        return switch (columnDetail.getDataType()) {
            case STRING    -> "String";
            case NUMERICAL -> "Numeric";
            default        -> "Date";
        };
    }

    /**
     * Resolves the display value for a given axis value, applying CRM username resolution when the value corresponds to a user ID.
     *
     * @param axisVal    the raw axis value
     * @param axisObject the {@link GraphAxis} containing the column mapping
     * @param crmUserMap mapping of CRM user IDs to {@link CrmUser}
     * @return the resolved display value (either the original value or a localized username)
     */
    protected Object getUserValue(final Object axisVal,
                                  final GraphAxis<Object> axisObject,
                                  final Map<Long, CrmUser> crmUserMap) {
        if (crmUserMap.isEmpty()) {
            return axisVal;
        }

        if (axisObject.getColumnMap().get(axisObject.getReportField().getColumnId()) instanceof Long) {
            final CrmUser crmUser = crmUserMap.get(NumberUtils.parseNumber(axisVal).longValue());

            return Objects.nonNull(crmUser) ? crmUser.getFullName() : axisVal;
        }

        return axisVal;
    }

    /**
     * Gets the data type based on the given function name applied to a field.
     *
     * @param function  the lowercase name of the function
     * @return          "Numeric", "String", or "Date" based on the function category
     */
    private static String getFunctionBasedDataType(final String function) {
        return switch (function) {
            case "hour_of_the_day", "day_of_the_month", "quarter_of_the_year" -> "Numeric";
            case "by_month", "day_of_the_week", "month_of_the_year", "by_week", "by_year" -> "String";
            default -> "Date";
        };
    }

    /**
     * <p>
     * Constructs a response object containing metadata and data values for a given list of graph axes
     * </p>
     *
     * @param orgId     the ID of the organization to which the axis belongs
     * @param graphAxis the GraphAxis object containing report field information
     * @param crmUserMap The user id as a key and {@link CrmUser} as value
     * @param i18nResolver resolver for localizing axis labels
     * @return a JsonObject containing the graph data details for the specified axis
     */
    protected abstract JsonObject getData( final @NonNull Long orgId,
                                           final @NonNull List<GraphAxis<Object>> graphAxis,
                                           final @NonNull Map<Long, CrmUser> crmUserMap,
                                           final @NonNull I18nResolver i18nResolver );

    /**
     * <p>
     * Populates the X-axis values based on the given label and track missing labels
     * </p>
     *
     * @param xAxisLabel   the label or function name used to determine transformation
     * @param yAxisValues  list of Y-axis values aligned with the X-axis
     * @param xAxisDataMap map of raw X-axis values indexed by position
     * @param xAxisList    collection to store the transformed X-axis values
     * @param i18nResolver resolver for localizing axis labels
     */
    public void populateXAxisByLabel(final String xAxisLabel,
                                     final List<AxisValue<Object>> yAxisValues,
                                     final Map<Integer, Object> xAxisDataMap,
                                     final Collection<Object> xAxisList,
                                     final @NonNull I18nResolver i18nResolver) {
        if (Objects.isNull(xAxisLabel)) {
            xAxisList.addAll(xAxisDataMap.values());
            return;
        }

        if (CRMFunction.DATE.getFunctionName().equals(xAxisLabel)) {
            final Set<Object> sortedXAxisDateKeys = buildSortedXAxisDateKeys(xAxisDataMap);

            xAxisList.addAll(sortedXAxisDateKeys);
            return;
        }

        final TimePeriodFunction timePeriodFunction = TimePeriodFunction.get(xAxisLabel);

        if (Objects.nonNull(timePeriodFunction)) {
            final Map<Object, Object> valueMap = new LinkedHashMap<>();

            timePeriodFunction.populate(yAxisValues, xAxisDataMap, valueMap, i18nResolver);
            xAxisList.addAll(TimePeriodFunction.BY_MONTH == timePeriodFunction ? valueMap.keySet() : timePeriodFunction.buildKeys(i18nResolver));
        } else {
            xAxisList.addAll(xAxisDataMap.values());
        }

    }


    /**
     * <p>
     * Populates the Y-axis values based on the given X-axis label.
     * </p>
     *
     * @param orgId                the organization identifier
     * @param xAxisLabel           the label or function name used to guide transformation
     * @param axisObject           the Y-axis graph axis definition
     * @param xAxisDataMap         map of raw X-axis values indexed by position
     * @param dataArray            array to store the transformed Y-axis values
     * @param displayValueResolver resolver for formatting display values
     * @param i18nResolver         resolver for localizing labels
     */
    public void populateYAxisByLabel(final @NonNull Long orgId,
                                     final String xAxisLabel,
                                     final @NonNull GraphAxis<Object> axisObject,
                                     final Map<Integer, Object> xAxisDataMap,
                                     final @NonNull JsonArray dataArray,
                                     final @NonNull DisplayValueResolver displayValueResolver,
                                     final @NonNull I18nResolver i18nResolver) {
        final Long columnId = axisObject.getReportField().getColumnId();
        final List<AxisValue<Object>> axisValues = axisObject.getAxisValues();

        if (Objects.isNull(xAxisLabel)) {
            addValuesToDataArray(orgId, columnId, axisValues,
                    AxisValue::getValue, displayValueResolver, dataArray, i18nResolver);
            return;
        }

        if (CRMFunction.DATE.getFunctionName().equals(xAxisLabel)) {
            final Map<Object, Object> groupedYAxisValueMap = new LinkedHashMap<>();
            final int rowCount = Math.min(xAxisDataMap.size(), axisValues.size());
            final Set<Object> sortedXAxisDateKeys = buildSortedXAxisDateKeys(xAxisDataMap);

            sortedXAxisDateKeys.forEach(dateKey -> groupedYAxisValueMap.put(dateKey, NumericalConstant.ZERO));

            for (int rowIndex = NumericalConstant.ZERO; rowIndex < rowCount; rowIndex++) {
                final Object dateKey = xAxisDataMap.get(rowIndex);
                final Object yAxisValue = axisValues.get(rowIndex).getValue();

                if (Objects.nonNull(yAxisValue) && !Objects.equals(yAxisValue, NumericalConstant.ZERO)) {
                    groupedYAxisValueMap.put(dateKey, yAxisValue);
                }
            }

            addValuesToDataArray(orgId, columnId, sortedXAxisDateKeys, groupedYAxisValueMap::get, displayValueResolver, dataArray, i18nResolver);
            return;
        }

        final TimePeriodFunction timePeriodFunction = TimePeriodFunction.get(xAxisLabel);

        if (Objects.nonNull(timePeriodFunction)) {
            final Map<Object, Object> valueMap = new LinkedHashMap<>();

            timePeriodFunction.populate(axisValues, xAxisDataMap, valueMap, i18nResolver);

            if (TimePeriodFunction.BY_MONTH == timePeriodFunction) {
                addValuesToDataArray(orgId, columnId, valueMap.keySet(), valueMap::get, displayValueResolver, dataArray, i18nResolver);
            } else {
                addValuesToDataArray(orgId, columnId, timePeriodFunction.buildKeys(i18nResolver), valueMap::get,
                        displayValueResolver, dataArray, i18nResolver);
            }
        } else {
            addValuesToDataArray(orgId, columnId, axisValues, AxisValue::getValue, displayValueResolver, dataArray, i18nResolver);
        }

    }

    /**
     * <p>
     * Populates a JSON array with display values resolved from a set of keys.
     * </p>
     *
     * @param orgId                the ID of the organization to which the values belong
     * @param columnId             the column identifier associated with the values
     * @param <T>                  the type of the keys being iterated
     * @param keys                 the iterable collection of keys to process
     * @param valueProvider        the function that maps a key to its corresponding value
     * @param displayValueResolver the resolver used to transform raw values into display values
     * @param dataArray            the JSON array to be populated with resolved display values
     * @param i18nResolver         the resolver for internationalization of display values
     */
    private static <T> void addValuesToDataArray(final long orgId,
                                                 final long columnId,
                                                 final Iterable<T> keys,
                                                 final Function<T, Object> valueProvider,
                                                 final DisplayValueResolver displayValueResolver,
                                                 final JsonArray dataArray,
                                                 final @NonNull I18nResolver i18nResolver) {
        for (final T key : keys) {
            final Object value = valueProvider.apply(key);

            dataArray.put(Objects.nonNull(value) ? displayValueResolver.resolveDisplayValue(orgId, value, columnId, i18nResolver)
                    : NumericalConstant.ZERO);
        }
    }

    /**
     * Finds a specific {@link GraphAxis} from the provided list that matches the given axis type and order.
     *
     * @param order     the order/index to match (e.g. FIRST_INDEX or SECOND_INDEX)
     * @param axes      the list of available graph axes
     * @param axisType  the axis type to look for
     * @return the matching {@link GraphAxis}
     */
    public GraphAxis<Object> findAxis(final int order, final List<GraphAxis<Object>> axes, final Axis axisType) {
        for (final GraphAxis<Object> axis : axes) {
            final ReportField reportField = axis.getReportField();

            if (Objects.equals(axisType, reportField.getAxis()) && order == reportField.getOrder()) {
                return axis;
            }
        }

        return null;
    }

    /**
     * Builds a sorted set of X-axis date keys from the given data map,
     * ordered by their numeric (long) value in ascending order.
     *
     * @param xAxisDataMap map of raw X-axis values indexed by position
     * @return a {@link TreeSet} of X-axis date keys sorted by long value
     */
    private static Set<Object> buildSortedXAxisDateKeys(final Map<Integer, Object> xAxisDataMap) {
        final Set<Object> sortedXAxisDateKeys = new TreeSet<>(Comparator.comparingLong(value ->
            value instanceof Date date? date.getTime()
                    : value instanceof Temporal temporal
                      ? temporal.getTimeStamp()
                      : NumberUtils.parseNumber(value).longValue()));

        sortedXAxisDateKeys.addAll(xAxisDataMap.values());
        return sortedXAxisDateKeys;
    }

    /**
     * Safely resolves axis display value with explicit resolver, handling null values.
     *
     * @param orgId        the organization ID
     * @param value        the raw value to resolve
     * @param axis         the GraphAxis for user resolution
     * @param crmUserMap   map of CRM users
     * @param columnId     the column ID
     * @param resolver     the DisplayValueResolver to use
     * @param i18nResolver resolver for localization
     * @return the resolved display value or null if input is null
     */
    protected Object resolveAxisDisplayValue(final @NonNull Long orgId,
                                           final Object value,
                                           final @NonNull GraphAxis<Object> axis,
                                           final @NonNull Map<Long, CrmUser> crmUserMap,
                                           final Long columnId,
                                           final @NonNull DisplayValueResolver resolver,
                                           final @NonNull I18nResolver i18nResolver) {
        return resolveNullableDisplayValue(orgId,
                getNullableUserValue(value, axis, crmUserMap),
                columnId,
                resolver,
                i18nResolver);
    }

    /**
     * Safely resolves metric display value with explicit resolver, returning ZERO_INDEX for null.
     *
     * @param orgId        the organization ID
     * @param value        the metric value to resolve
     * @param columnId     the column ID
     * @param resolver     the DisplayValueResolver to use
     * @param i18nResolver resolver for localization
     * @return the resolved display value or ZERO_INDEX if null
     */
    protected Object resolveMetricDisplayValue(final @NonNull Long orgId,
                                             final Object value,
                                             final Long columnId,
                                             final @NonNull DisplayValueResolver resolver,
                                             final @NonNull I18nResolver i18nResolver) {
        return Objects.nonNull(value) ? resolver.resolveDisplayValue(orgId, value, columnId, i18nResolver) : NumericalConstant.ZERO;
    }

    /**
     * Safely resolves a nullable display value with explicit resolver, returning null for null input.
     *
     * @param orgId        the organization ID
     * @param value        the value to resolve
     * @param columnId     the column ID
     * @param resolver     the DisplayValueResolver to use
     * @param i18nResolver resolver for localization
     * @return the resolved display value or null if input is null
     */
    protected Object resolveNullableDisplayValue(final @NonNull Long orgId,
                                               final Object value,
                                               final Long columnId,
                                               final @NonNull DisplayValueResolver resolver,
                                               final @NonNull I18nResolver i18nResolver) {
        return Objects.nonNull(value) ? resolver.resolveDisplayValue(orgId, value, columnId, i18nResolver) : null;
    }

    /**
     * Safely gets user value, returning null if input is null before delegating to getUserValue.
     *
     * @param value        the value to resolve
     * @param axis         the GraphAxis for user resolution
     * @param crmUserMap   map of CRM users
     * @return the resolved user value or null if input is null
     */
    protected Object getNullableUserValue(final Object value,
                                        final @NonNull GraphAxis<Object> axis,
                                        final @NonNull Map<Long, CrmUser> crmUserMap) {
        return Objects.nonNull(value) ? getUserValue(value, axis, crmUserMap) : null;
    }
}
