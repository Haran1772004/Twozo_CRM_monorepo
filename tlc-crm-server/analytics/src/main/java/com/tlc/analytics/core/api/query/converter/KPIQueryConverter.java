package com.tlc.analytics.core.api.query.converter;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.view.OperationType;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparator;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorSide;
import com.tlc.analytics.core.api.internal.DataViewRequest;
import com.tlc.analytics.core.api.KPIRequest;

import com.tlc.commons.code.ErrorCode;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.util.WhereClauseUtil;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.olap.api.dml.function.OlapSqlFunctionBuilderFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Implementation of {@link KPIQueryConverter} for converting a {@link DataViewRequest} into a {@link SelectQuery}.
 *
 * @param <V>       The type of report view associated with the request, e.g., {@link ChartView}.
 * @param <Request> The type of data view request, e.g., {@link KPIRequest}.
 * @author Jamesh
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class KPIQueryConverter<V extends AnalyticsReport, Request extends DataViewRequest<V>>
        extends AbstractReportSelectQueryConverter<V, Request> {

    private static class Instance {
        private static final KPIQueryConverter INSTANCE = new KPIQueryConverter();
    }

    public static KPIQueryConverter INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Holds the start and end time as fromUnixTimestamp64Milli expression strings
     * </p>
     */
    private record FunctionInterval(SqlFunction.Builder start, SqlFunction.Builder end) {
    }

    /**
     * {@inheritDoc}
     *
     * @param column        Represents the {@link Column}
     * @param columnDetail Represents the {@link ColumnDetail}
     * @param viewRequest   Represents the {@link DataViewRequest}
     * @return The function column
     */
    @Override
    void applyFunctionInterval(final @NonNull Column column, final @NonNull ColumnDetail columnDetail,
                               final @NonNull Request viewRequest, final @NonNull SelectQuery selectQuery) {
        final KPIRequest kpiRequest = (KPIRequest) viewRequest;
        final FunctionInterval interval = getIntervalTime(kpiRequest.getOperationType(), kpiRequest.getKpiComparator());

        selectQuery.setWhereClause(WhereClauseUtil.connect(selectQuery.getWhereClause(),
                new WhereClause(Criteria.between(column, interval.start(), interval.end())),
                Connector.AND));
    }

    /**
     * <p>
     * Gets the interval time based on the {@link KPIComparator}
     * </p>
     *
     * @param operationType Represents the {@link OperationType}
     * @param kpiComparator Represents the {@link KPIComparator}
     * @return The {@link FunctionInterval}
     */
    private FunctionInterval getIntervalTime(final OperationType operationType, final KPIComparator kpiComparator) {
        if (OperationType.CUSTOM == operationType) {
            return getCustomInterval(kpiComparator);
        }

        if (KPIComparatorSide.LEFT == kpiComparator.getComparatorSide()) {
            return getBaseInterval(operationType);
        }

        return getComparatorInterval(operationType, kpiComparator);
    }

    /**
     * <p>
     * Gets the start and end time for the base KPI interval.
     * </p>
     *
     * @param operationType Represents the {@link OperationType} details
     * @return The {@link FunctionInterval} where the key represents the start time and value represents the end time
     */
    private FunctionInterval getBaseInterval(final OperationType operationType) {
        return switch (operationType) {
            case TODAY -> buildInterval(OlapSqlFunctionBuilderFactory.sinceToday(), OlapSqlFunctionBuilderFactory.afterToday());
            case THIS_WEEK -> buildInterval(OlapSqlFunctionBuilderFactory.sinceThisWeek(), OlapSqlFunctionBuilderFactory.afterThisWeek());
            case THIS_MONTH -> buildInterval(OlapSqlFunctionBuilderFactory.sinceThisMonth(), OlapSqlFunctionBuilderFactory.afterThisMonth());
            case THIS_QUARTER -> buildInterval(OlapSqlFunctionBuilderFactory.sinceThisQuarter(), OlapSqlFunctionBuilderFactory.afterThisQuarter());
            case THIS_YEAR -> buildInterval(OlapSqlFunctionBuilderFactory.sinceThisYear(), OlapSqlFunctionBuilderFactory.afterThisYear());
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_OPERATION_TYPE);
        };
    }

    /**
     * <p>
     * Gets the comparator interval based on the {@link KPIComparator#getComparatorType()}.
     * Delegates to the appropriate interval method depending on the comparator type.
     * </p>
     *
     * @param operationType Represents the {@link OperationType} details
     * @param kpiComparator Represents the {@link KPIComparator} containing comparator type and range info
     * @return The {@link FunctionInterval} where the key represents the start time and value represents the end time
     */
    private FunctionInterval getComparatorInterval(final OperationType operationType, final KPIComparator kpiComparator) {
        return switch (kpiComparator.getComparatorType()) {
            case PREVIOUS -> getPreviousInterval(operationType);
            case PREVIOUS_RELATIVE -> getPreviousRelativeInterval(operationType);
            case SAME_DAY_PREVIOUS_YEAR -> getSameDayPreviousYearInterval(operationType);
            case CUSTOM -> getCustomInterval(kpiComparator);
        };
    }

    /**
     * <p>
     * Gets the start and end time for a custom KPI interval using the from/to values
     * defined in the {@link KPIComparator}.
     * </p>
     *
     * @param kpiComparator Represents the {@link KPIComparator} containing the custom date range
     * @return The {@link FunctionInterval} where the key represents the start time and value represents the end time
     */
    private FunctionInterval getCustomInterval(final KPIComparator kpiComparator) {
        return new FunctionInterval(OlapSqlFunctionBuilderFactory.fromUnixTimestamp64Milli(kpiComparator.getFrom()),
                OlapSqlFunctionBuilderFactory.afterCustom(kpiComparator.getTo()));
    }

    /**
     * <p>
     * Gets the start and end time for the previous period KPI interval based on the {@link OperationType}.
     * For example, if the operation type is {@code TODAY}, this returns yesterday's interval.
     * </p>
     *
     * @param operationType Represents the {@link OperationType} details
     * @return The {@link FunctionInterval} where the key represents the start time and value represents the end time
     */
    private FunctionInterval getPreviousInterval(final OperationType operationType) {
        return switch (operationType) {
            case TODAY -> buildInterval(OlapSqlFunctionBuilderFactory.sinceYesterday(), OlapSqlFunctionBuilderFactory.subtractSeconds(OlapSqlFunctionBuilderFactory.afterYesterday()));
            case THIS_WEEK -> buildInterval(OlapSqlFunctionBuilderFactory.sinceLastWeek(), OlapSqlFunctionBuilderFactory.subtractSeconds(OlapSqlFunctionBuilderFactory.afterLastWeek()));
            case THIS_MONTH -> buildInterval(OlapSqlFunctionBuilderFactory.sinceLastMonth(), OlapSqlFunctionBuilderFactory.subtractSeconds(OlapSqlFunctionBuilderFactory.afterLastMonth()));
            case THIS_QUARTER -> buildInterval(OlapSqlFunctionBuilderFactory.sinceLastQuarter(), OlapSqlFunctionBuilderFactory.subtractSeconds(OlapSqlFunctionBuilderFactory.afterLastQuarter()));
            case THIS_YEAR -> buildInterval(OlapSqlFunctionBuilderFactory.sinceLastYear(), OlapSqlFunctionBuilderFactory.subtractSeconds(OlapSqlFunctionBuilderFactory.afterLastYear()));
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_OPERATION_TYPE);
        };
    }

    /**
     * <p>
     * Gets the start and end time for the previous relative period KPI interval based on the {@link OperationType}.
     * The previous relative interval is proportional to how much of the current period has elapsed.
     * For example, if today is Wednesday and the operation type is {@code THIS_WEEK},
     * the interval covers the equivalent elapsed portion of the previous week.
     * </p>
     *
     * @param operationType Represents the {@link OperationType} details
     * @return The {@link FunctionInterval} where the key represents the start time and value represents the end time
     */
    private FunctionInterval getPreviousRelativeInterval(final OperationType operationType) {
        return switch (operationType) {
            case TODAY -> buildInterval(OlapSqlFunctionBuilderFactory.sincePreviousRelativeToday(), OlapSqlFunctionBuilderFactory.afterPreviousRelativeToday());
            case THIS_WEEK -> buildInterval(OlapSqlFunctionBuilderFactory.sincePreviousRelativeThisWeek(), OlapSqlFunctionBuilderFactory.afterPreviousRelativeThisWeek());
            case THIS_MONTH -> buildInterval(OlapSqlFunctionBuilderFactory.sincePreviousRelativeThisMonth(), OlapSqlFunctionBuilderFactory.afterPreviousRelativeThisMonth());
            case THIS_QUARTER -> buildInterval(OlapSqlFunctionBuilderFactory.sincePreviousRelativeThisQuarter(), OlapSqlFunctionBuilderFactory.afterPreviousRelativeThisQuarter());
            case THIS_YEAR -> buildInterval(OlapSqlFunctionBuilderFactory.sincePreviousRelativeThisYear(), OlapSqlFunctionBuilderFactory.afterPreviousRelativeThisYear());
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_OPERATION_TYPE);
        };
    }

    /**
     * <p>
     * Gets the start and end time for the same day of the previous year KPI interval based on the {@link OperationType}.
     * For example, if the operation type is {@code THIS_MONTH}, this returns the interval for the same
     * month in the previous year, up to the same day as today.
     * </p>
     *
     * @param operationType Represents the {@link OperationType} details
     * @return The {@link FunctionInterval} where the key represents the start time and value represents the end time
     */
    private FunctionInterval getSameDayPreviousYearInterval(final OperationType operationType) {
        return switch (operationType) {
            case TODAY -> buildInterval(OlapSqlFunctionBuilderFactory.sinceSameDayPreviousYearToday(), OlapSqlFunctionBuilderFactory.afterSameDayPreviousYearToday());
            case THIS_WEEK -> buildInterval(OlapSqlFunctionBuilderFactory.sinceSameDayPreviousYearThisWeek(), OlapSqlFunctionBuilderFactory.afterSameDayPreviousYearThisWeek());
            case THIS_MONTH -> buildInterval(OlapSqlFunctionBuilderFactory.sinceSameDayPreviousYearThisMonth(), OlapSqlFunctionBuilderFactory.afterSameDayPreviousYearThisMonth());
            case THIS_QUARTER -> buildInterval(OlapSqlFunctionBuilderFactory.sinceSameDayPreviousYearThisQuarter(), OlapSqlFunctionBuilderFactory.afterSameDayPreviousYearThisQuarter());
            case THIS_YEAR -> buildInterval(OlapSqlFunctionBuilderFactory.sinceLastYear(), OlapSqlFunctionBuilderFactory.afterLastYear());
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_OPERATION_TYPE);
        };
    }

    /**
     * <p>
     * Builds a {@link FunctionInterval} from two {@link SqlFunction.Builder} instances
     * representing the start and end time expressions.
     * </p>
     *
     * @param sinceBuilder the SQL function builder for the start time
     * @param afterBuilder the SQL function builder for the end time
     * @return the constructed {@link FunctionInterval}
     */
    private FunctionInterval buildInterval(final SqlFunction.Builder sinceBuilder, final SqlFunction.Builder afterBuilder) {
        return new FunctionInterval(sinceBuilder, afterBuilder);
    }
}
