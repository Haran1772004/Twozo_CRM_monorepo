package com.tlc.analytics.core.api.query.filter;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.view.filter.Filter;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.FilterFunctionMapper;
import com.tlc.analytics.config.model.view.filter.OperatorType;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.olap.api.dml.function.OlapSqlFunctionBuilderFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.util.Objects;
import java.util.function.Function;

/**
 * Implementation of {@link FilterApplier} for date-based filters
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class DateFilter extends AbstractFilterApplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateFilter.class);
    private static final String INVALID_OPERATOR_KEY = "i18n.analytics.invalid.criteria.operator";
    private static final String DATE_DATA_TYPE = "DATE";
    private static final String NULLABLE_DATE_DATA_TYPE = "NULLABLE(DATE)";

    private static class Instance {
        private static final DateFilter INSTANCE = new DateFilter();
    }

    /**
     * Returns singleton instance.
     *
     * @return {@link DateFilter} singleton
     */
    public static DateFilter INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param selectQuery           The {@link SelectQuery} to which the criteria is applied.
     * @param column                The {@link Column} that the criteria is applied to.
     * @param filterField           The {@link FilterField} containing filter details.
     * @param columnDetailsFunction A function to retrieve {@link ColumnDetail} based on column ID.
     * @param timezoneInfo          Represents the {@link TimezoneInfo} details
     * @return The criteria to be applied to the select query. Returns null if the criteria cannot be determined.
     */
    @Override
    protected @NonNull Criteria handleAndGetCriteria(final @NonNull SelectQuery selectQuery,
                                                     final @NonNull Column column,
                                                     final @NonNull FilterField filterField,
                                                     final @NonNull Function<Long, ColumnDetail> columnDetailsFunction,
                                                     final @NonNull TimezoneInfo timezoneInfo) {
        Criteria criteria = null;

        if (filterField.getFilterType() == Filter.RANGE) {
            criteria = handleRangeFilter(column, filterField);
        } else if (filterField.getFilterType() == Filter.NUMERIC) {
            criteria = handleNumericFilter(selectQuery, column, filterField, timezoneInfo);
        } else {
            LOGGER.error("Analytics: Date Filter - Invalid criteria for column: {}, operatorType: {}", column.getColumnName(), filterField.getOperatorType());
            throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE, INVALID_OPERATOR_KEY);
        }

        return criteria;
    }

    /**
     * Handles RANGE filter type (TO, FROM, BETWEEN).
     *
     * @param column      the column to filter on
     * @param filterField the filter field containing range values
     * @return the constructed criteria
     */
    private Criteria handleRangeFilter(final Column column, final FilterField filterField) {
        return switch (filterField.getOperatorType()) {
            case TO -> Criteria.le(column, filterField.getTo());
            case FROM -> Criteria.ge(column, filterField.getFrom());
            case BETWEEN -> Criteria.between(column,
                    OlapSqlFunctionBuilderFactory.fromUnixTimestamp64Milli(filterField.getFrom()),
                    OlapSqlFunctionBuilderFactory.fromUnixTimestamp64Milli(filterField.getTo()));
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE, INVALID_OPERATOR_KEY);
        };
    }

    /**
     * Handles NUMERIC filter type with various operator types (IS, ON, BEFORE, AFTER, SINCE, etc.).
     *
     * @param selectQuery  The query to directly append criteria to when using half-open intervals
     * @param column       the column to filter on
     * @param filterField  the filter field containing operator and value
     * @param timezoneInfo the user's timezone info
     * @return the constructed criteria
     */
    private Criteria handleNumericFilter(final SelectQuery selectQuery, final Column column, final FilterField filterField, final TimezoneInfo timezoneInfo) {
        final Object filterFieldValue = filterField.getValue();
        final Number value = filterFieldValue == null ? null : getNum(filterFieldValue);
        final OperatorType operatorType = filterField.getOperatorType();

        return switch (operatorType) {
            case IS, EQUAL -> Criteria.eq(column, value);
            case LESS_THAN -> Criteria.lt(column, value);
            case GREATER_THAN -> Criteria.gt(column, value);
            case IS_NOT, NOT_EQUAL_TO -> Criteria.notEq(column, value);
            case GREATER_THAN_OR_EQUAL_TO -> Criteria.ge(column, value);
            case LESS_THAN_OR_EQUAL_TO -> Criteria.le(column, value);
            case ON -> handleOnFilter(selectQuery, column, filterField);
            case DAY_IS -> handleDayIsFilter(column, filterField);
            case BEFORE, AFTER -> handleBeforeAfterFilter(column, filterField, operatorType, value, timezoneInfo);
            case SINCE -> handleSinceFilter(column, filterField, value, timezoneInfo);
            case IN_THE_LAST, NOT_IN_THE_LAST -> handleInTheLastFilter(column, filterField, operatorType, value, timezoneInfo);
            case IS_EMPTY -> Criteria.eq(column, null);
            case NOT_EMPTY -> Criteria.notEq(column, null);
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE, INVALID_OPERATOR_KEY);
        };
    }

    /**
     * Handles the ON operator — builds a BETWEEN criteria for the matched time period for dates,
     * or a half-open interval for datetimes.
     *
     * @param selectQuery The query being built
     * @param column      the column to filter on
     * @param filterField the filter field containing function mapper
     * @return the constructed criteria, or null if no function mapper
     */
    private Criteria handleOnFilter(final SelectQuery selectQuery, final Column column, final FilterField filterField) {
        final FilterFunctionMapper filterFunctionMapper = filterField.getFilterFunctionMapper();

        if (filterFunctionMapper == null) {
            return null;
        }

        final boolean isDateType = isDateOnlyColumn(column);
        final String functionName = filterFunctionMapper.getFunctionName();

        if (isDateType) {
            return switch (functionName) {
                case "today" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceToday(), OlapSqlFunctionBuilderFactory.endOfToday());
                case "yesterday" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceYesterday(), OlapSqlFunctionBuilderFactory.endOfYesterday());
                case "this_week" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceThisWeek(), OlapSqlFunctionBuilderFactory.endOfThisWeek());
                case "last_week" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceLastWeek(), OlapSqlFunctionBuilderFactory.endOfLastWeek());
                case "this_month" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceThisMonth(), OlapSqlFunctionBuilderFactory.endOfThisMonth());
                case "last_month" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceLastMonth(), OlapSqlFunctionBuilderFactory.endOfLastMonth());
                case "this_quarter" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceThisQuarter(), OlapSqlFunctionBuilderFactory.endOfThisQuarter());
                case "last_quarter" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceLastQuarter(), OlapSqlFunctionBuilderFactory.endOfLastQuarter());
                case "this_year" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceThisYear(), OlapSqlFunctionBuilderFactory.endOfThisYear());
                case "last_year" -> buildBetween(column, OlapSqlFunctionBuilderFactory.sinceLastYear(), OlapSqlFunctionBuilderFactory.endOfLastYear());
                default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE, INVALID_OPERATOR_KEY);
            };
        } else {
            final SqlFunction.Builder lowerBound;
            final SqlFunction.Builder upperBound;
            switch (functionName) {
                case "today" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceToday(); upperBound = OlapSqlFunctionBuilderFactory.afterToday(); }
                case "yesterday" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceYesterday(); upperBound = OlapSqlFunctionBuilderFactory.afterYesterday(); }
                case "this_week" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceThisWeek(); upperBound = OlapSqlFunctionBuilderFactory.afterThisWeek(); }
                case "last_week" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceLastWeek(); upperBound = OlapSqlFunctionBuilderFactory.afterLastWeek(); }
                case "this_month" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceThisMonth(); upperBound = OlapSqlFunctionBuilderFactory.afterThisMonth(); }
                case "last_month" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceLastMonth(); upperBound = OlapSqlFunctionBuilderFactory.afterLastMonth(); }
                case "this_quarter" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceThisQuarter(); upperBound = OlapSqlFunctionBuilderFactory.afterThisQuarter(); }
                case "last_quarter" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceLastQuarter(); upperBound = OlapSqlFunctionBuilderFactory.afterLastQuarter(); }
                case "this_year" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceThisYear(); upperBound = OlapSqlFunctionBuilderFactory.afterThisYear(); }
                case "last_year" -> { lowerBound = OlapSqlFunctionBuilderFactory.sinceLastYear(); upperBound = OlapSqlFunctionBuilderFactory.afterLastYear(); }
                default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE, INVALID_OPERATOR_KEY);
            }
            addToWhereClause(selectQuery, Criteria.ge(column, lowerBound));
            return Criteria.lt(column, upperBound);
        }
    }

    /**
     * Handles the DAY_IS operator — builds an equality criteria on day-of-week.
     *
     * @param column      the column to filter on
     * @param filterField the filter field containing function mapper with day name
     * @return the constructed criteria, or null if no function mapper
     */
    private Criteria handleDayIsFilter(final Column column, final FilterField filterField) {
        final FilterFunctionMapper filterFunctionMapper = filterField.getFilterFunctionMapper();

        if (filterFunctionMapper == null) {
            return null;
        }

        return Criteria.eq(column.function(OlapSqlFunctionBuilderFactory.toDayOfFWeek(), column.getColumnAlias(), NumericalConstant.ZERO),
                DayOfWeek.valueOf(filterFunctionMapper.getFunctionName().toUpperCase()).getValue());
    }

    /**
     * Handles BEFORE and AFTER operators — builds lt/gt criteria based on time period.
     *
     * @param column       the column to filter on
     * @param filterField  the filter field containing function mapper
     * @param operatorType BEFORE or AFTER
     * @param value        the filter value (used for custom)
     * @param timezoneInfo the user's timezone info
     * @return the constructed criteria, or null if no function mapper
     */
    private Criteria handleBeforeAfterFilter(final Column column, final FilterField filterField,
                                             final OperatorType operatorType, final Number value,
                                             final TimezoneInfo timezoneInfo) {
        final FilterFunctionMapper filterFunctionMapper = filterField.getFilterFunctionMapper();

        if (filterFunctionMapper == null) {
            return null;
        }

        final String functionName = filterFunctionMapper.getFunctionName();
        final boolean isBefore = operatorType == OperatorType.BEFORE;

        if ("custom".equals(functionName)) {
            final long actualValue = value.longValue();

            return isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.fromUnixTimestamp64MilliWithTimezone(actualValue,
                    timezoneInfo.getTimeZone().getID())) : Criteria.gt(column, OlapSqlFunctionBuilderFactory.afterCustom(actualValue));
        }

        final boolean isDateType = isDateOnlyColumn(column);

        return switch (functionName) {
            case "today" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeToday())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfToday()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterToday());
            case "yesterday" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeYesterday())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfYesterday()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterYesterday());
            case "this_week" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeThisWeek())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfThisWeek()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterThisWeek());
            case "last_week" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeLastWeek())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfLastWeek()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterLastWeek());
            case "this_month" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeThisMonth())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfThisMonth()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterThisMonth());
            case "last_month" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeLastMonth())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfLastMonth()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterLastMonth());
            case "this_quarter" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeThisQuarter())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfThisQuarter()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterThisQuarter());
            case "last_quarter" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeLastQuarter())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfLastQuarter()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterLastQuarter());
            case "this_year" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeThisYear())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfThisYear()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterThisYear());
            case "last_year" -> isBefore ? Criteria.lt(column, OlapSqlFunctionBuilderFactory.beforeLastYear())
                    : isDateType ? Criteria.gt(column, OlapSqlFunctionBuilderFactory.endOfLastYear()) : Criteria.ge(column, OlapSqlFunctionBuilderFactory.afterLastYear());
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE);
        };
    }

    /**
     * Handles the SINCE operator — builds a ge criteria from the specified time period.
     *
     * @param column       the column to filter on
     * @param filterField  the filter field containing function mapper
     * @param value        the filter value (used for custom)
     * @param timezoneInfo the user's timezone info
     * @return the constructed criteria, or null if no function mapper
     */
    private Criteria handleSinceFilter(final Column column, final FilterField filterField,
                                       final Number value, final TimezoneInfo timezoneInfo) {
        final FilterFunctionMapper filterFunctionMapper = filterField.getFilterFunctionMapper();

        if (filterFunctionMapper == null) {
            return null;
        }

        return switch (filterFunctionMapper.getFunctionName()) {
            case "custom" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.fromUnixTimestamp64MilliWithTimezone(value.longValue(), timezoneInfo.getTimeZone().getID()));
            case "today" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceToday());
            case "yesterday" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceYesterday());
            case "this_week" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceThisWeek());
            case "last_week" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceLastWeek());
            case "this_month" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceThisMonth());
            case "last_month" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceLastMonth());
            case "this_quarter" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceThisQuarter());
            case "last_quarter" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceLastQuarter());
            case "this_year" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceThisYear());
            case "last_year" -> Criteria.ge(column, OlapSqlFunctionBuilderFactory.sinceLastYear());
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE);
        };
    }

    /**
     * Handles IN_THE_LAST and NOT_IN_THE_LAST operators.
     *
     * @param column       the column to filter on
     * @param filterField  the filter field containing function mapper with time unit
     * @param operatorType IN_THE_LAST or NOT_IN_THE_LAST
     * @param value        the number of time units
     * @param timezoneInfo the user's timezone info
     * @return the constructed criteria, or null if no function mapper
     */
    private Criteria handleInTheLastFilter(final Column column, final FilterField filterField,
                                           final OperatorType operatorType, final Number value,
                                           final TimezoneInfo timezoneInfo) {
        final FilterFunctionMapper filterFunctionMapper = filterField.getFilterFunctionMapper();

        if (filterFunctionMapper == null) {
            return null;
        }

        final String unit = filterFunctionMapper.getFunctionName();
        final long unitValue = value.longValue();

        return operatorType == OperatorType.IN_THE_LAST ? buildInTheLastCriteria(column, unit, unitValue, timezoneInfo)
                : buildNotInTheLastCriteria(column, unit, unitValue, timezoneInfo);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Builds a BETWEEN criteria using two SQL function builders.
     *
     * @param column the column to apply the criteria on
     * @param from   the start bound builder
     * @param to     the end bound builder
     * @return the BETWEEN criteria
     */
    private static Criteria buildBetween(final Column column, final SqlFunction.Builder from, final SqlFunction.Builder to) {
        return Criteria.between(column, from, to);
    }

    /**
     * Converts an input value to a numeric format.
     *
     * @param value The input object to be converted to a {@link Number}
     * @return A {@link Number} representing the parsed value
     */
    private Number getNum(final Object value) {
        final String strVal = String.valueOf(value);

        return strVal.contains(".") ? Double.parseDouble(strVal) : Long.parseLong(strVal);
    }

    /**
     * DATE / NULLABLE(DATE) columns require day-end bounds for inclusive BETWEEN clauses.
     */
    private boolean isDateOnlyColumn(final Column column) {
        final String dataType = column.getColumnDefinition().getDataType().getDataTypeStr();

        return DATE_DATA_TYPE.equalsIgnoreCase(dataType) || NULLABLE_DATE_DATA_TYPE.equalsIgnoreCase(dataType);
    }

    /**
     * Adds a lower-bound criteria directly to the select query's WHERE clause so that the caller
     * can return only the upper-bound criteria. This lets the ON operator produce a half-open
     * interval [start, nextPeriodStart) for DATETIME columns without requiring two returned criteria.
     */
    private void addToWhereClause(final SelectQuery selectQuery, final Criteria criteria) {
        final WhereClause existing = selectQuery.getWhereClause();

        if (Objects.isNull(existing)) {
            selectQuery.setWhereClause(new WhereClause(criteria));
        } else {
            selectQuery.setWhereClause(existing.and(criteria));
        }
    }

    /**
     * Builds criteria for the "IN THE LAST" date filter.
     *
     * @param column    The column to apply the filter on
     * @param unit      The time unit (seconds, minutes, hours, days, weeks, months, years)
     * @param unitValue The number of time units
     * @param timezoneInfo The user's timezone info
     * @return The BETWEEN criteria for the IN THE LAST filter
     */
    private Criteria buildInTheLastCriteria(final Column column, final String unit, final long unitValue, final TimezoneInfo timezoneInfo) {
        final String timezoneId = timezoneInfo.getTimeZone().getID();
        final SqlFunction.Builder lowerBound;
        final SqlFunction.Builder upperBound;

        switch (unit) {
            case "seconds" -> {
                lowerBound = OlapSqlFunctionBuilderFactory.inTheLastSecondsLower(unitValue);
                upperBound = OlapSqlFunctionBuilderFactory.inTheLastSecondsUpper();
            }
            case "minutes" -> {
                lowerBound = OlapSqlFunctionBuilderFactory.inTheLastMinutesLower(unitValue);
                upperBound = OlapSqlFunctionBuilderFactory.inTheLastMinutesUpper();
            }
            case "hours" -> {
                lowerBound = OlapSqlFunctionBuilderFactory.inTheLastHoursLower(unitValue);
                upperBound = OlapSqlFunctionBuilderFactory.inTheLastHoursUpper();
            }
            case "days" -> {
                lowerBound = OlapSqlFunctionBuilderFactory.inTheLastDaysLower(unitValue);
                upperBound = OlapSqlFunctionBuilderFactory.inTheLastDaysUpper();
            }
            case "weeks" -> {
                lowerBound = OlapSqlFunctionBuilderFactory.inTheLastWeeksLower(unitValue, timezoneId);
                upperBound = OlapSqlFunctionBuilderFactory.inTheLastWeeksUpper(timezoneId);
            }
            case "months" -> {
                lowerBound = OlapSqlFunctionBuilderFactory.inTheLastMonthsLower(unitValue, timezoneId);
                upperBound = OlapSqlFunctionBuilderFactory.inTheLastMonthsUpper(timezoneId);
            }
            case "years" -> {
                lowerBound = OlapSqlFunctionBuilderFactory.inTheLastYearsLower(unitValue, timezoneId);
                upperBound = OlapSqlFunctionBuilderFactory.inTheLastYearsUpper(timezoneId);
            }
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE, INVALID_OPERATOR_KEY);
        }

        return Criteria.between(column, lowerBound, upperBound);
    }

    /**
     * Builds criteria for the "NOT IN THE LAST" date filter.
     *
     * @param column    The column to apply the filter on
     * @param unit      The time unit (seconds, minutes, hours, days, weeks, months, years)
     * @param unitValue The number of time units
     * @param timezoneInfo The user's timezone info
     * @return The lt criteria for the NOT IN THE LAST filter
     */
    private Criteria buildNotInTheLastCriteria(final Column column, final String unit, final long unitValue, final TimezoneInfo timezoneInfo) {
        final String timezoneId = timezoneInfo.getTimeZone().getID();

        final SqlFunction.Builder bound = switch (unit) {
            case "seconds" -> OlapSqlFunctionBuilderFactory.notInTheLastSeconds(unitValue);
            case "minutes" -> OlapSqlFunctionBuilderFactory.notInTheLastMinutes(unitValue);
            case "hours" -> OlapSqlFunctionBuilderFactory.notInTheLastHours(unitValue);
            case "days" -> OlapSqlFunctionBuilderFactory.notInTheLastDays(unitValue);
            case "weeks" -> OlapSqlFunctionBuilderFactory.notInTheLastWeeks(unitValue, timezoneId);
            case "months" -> OlapSqlFunctionBuilderFactory.notInTheLastMonths(unitValue, timezoneId);
            case "years" -> OlapSqlFunctionBuilderFactory.notInTheLastYears(unitValue, timezoneId);
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE, INVALID_OPERATOR_KEY);
        };

        return Criteria.lt(column, bound);
    }
}
