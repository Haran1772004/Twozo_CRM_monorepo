package com.tlc.analytics.core.api.query.filter;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.view.filter.Filter;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.OperatorType;

import com.tlc.analytics.general.utils.ValueNormalizer;
import com.tlc.commons.code.ErrorCode;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JsonColumn;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.meta.CommonDataType;

import com.tlc.sql.olap.api.dml.OlapCriteria;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Function;

/**
 * Implementation of {@link FilterApplier} for text-based filters
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors(fluent = true)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class TextFilter extends AbstractFilterApplier {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextFilter.class);

    private static class Instance {
        private static final TextFilter INSTANCE = new TextFilter();
    }

    public static TextFilter INSTANCE() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param selectQuery           The {@link SelectQuery} to which the criteria is applied.
     * @param column                The {@link Column} that the criteria is applied to.
     * @param filterField           The {@link FilterField} containing filter details.
     * @param columnDetailsFunction A function to retrieve {@link ColumnDetail} based on column ID.
     * @param timezoneInfo          Represents the user {@link TimezoneInfo} details
     * @return The criteria to be applied to the select query. Returns null if the criteria cannot be determined.
     */
    @Override
    protected @NonNull Criteria handleAndGetCriteria(final @NonNull SelectQuery selectQuery,
                                                     final @NonNull Column column,
                                                     final @NonNull FilterField filterField,
                                                     final @NonNull Function<Long, ColumnDetail> columnDetailsFunction,
                                                     final @NonNull TimezoneInfo timezoneInfo) {
        final String value = String.valueOf(filterField.getValue());
        final OperatorType operatorType = filterField.getOperatorType();
        Criteria criteria;

        if (filterField.getFilterType() == Filter.TEXT) {

            if (CommonDataType.BOOLEAN == column.getColumnDefinition().getDataType()) {
                final boolean filterValue = Boolean.parseBoolean(value);

                switch (operatorType) {
                    case IS -> criteria = Criteria.eq(column, filterValue);
                    case IS_NOT -> criteria = Criteria.notEq(column, filterValue);
                    default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE,
                            "i18n.analytics.invalid.criteria.operator");
                }
            } else {
                JsonColumn jsonColumn = null;

                if (column instanceof JsonColumn) {
                    jsonColumn = (JsonColumn) column;
                }

                if (Objects.equals(CommonDataType.ARRAY, column.getColumnDefinition().getDataType()) ||
                        (jsonColumn != null && Objects.equals(jsonColumn.getReturnType(), CommonDataType.ARRAY))) {
                    switch (operatorType) {
                        case IS -> criteria = OlapCriteria.hasAny(column, ValueNormalizer.normalizeToCollection(value));
                        case IS_NOT -> criteria = OlapCriteria.notHasAny(column, ValueNormalizer.normalizeToCollection(value));
                        case IS_EMPTY -> criteria = Criteria.eq( column, null );
                        case NOT_EMPTY -> criteria = Criteria.notEq( column, null );
                        default ->  throw ErrorCode.get( ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE,
                                "i18n.analytics.invalid.criteria.operator" );
                    }
                } else if (Objects.nonNull(jsonColumn) && Objects.equals(jsonColumn.getReturnType(), CommonDataType.BIGINT)) {
                    final Number numericValue = Long.parseLong(value);

                    switch (operatorType) {
                        case IS, EQUAL, IS_EMPTY -> criteria = Criteria.eq(column, numericValue);
                        case LESS_THAN -> criteria = Criteria.lt(column, numericValue);
                        case GREATER_THAN -> criteria = Criteria.gt(column, numericValue);
                        case IS_NOT, NOT_EQUAL_TO, NOT_EMPTY -> criteria = Criteria.notEq(column, numericValue);
                        case GREATER_THAN_OR_EQUAL_TO -> criteria = Criteria.ge(column, numericValue);
                        case LESS_THAN_OR_EQUAL_TO -> criteria = Criteria.le(column, numericValue);
                        default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE,
                                "i18n.analytics.invalid.criteria.operator");
                    }
                } else {
                    switch (operatorType) {
                        case CONTAINS -> criteria = Criteria.contains(column, ValueNormalizer.normalizeToString(value));
                        case DOES_NOT_CONTAINS -> criteria = Criteria.notContains(column, ValueNormalizer.normalizeToString(value));
                        case IS_EMPTY -> criteria = Criteria.eq(column, null);
                        case NOT_EMPTY -> criteria = Criteria.notEq(column, null);
                        case STARTS_WITH -> criteria = Criteria.startsWith(column, ValueNormalizer.normalizeToString(value));
                        case ENDS_WITH -> criteria = Criteria.endsWith(column, ValueNormalizer.normalizeToString(value));
                        case IS -> criteria = Criteria.eq(column, ValueNormalizer.normalizeToString(value));
                        case IS_NOT -> criteria = Criteria.notEq(column, ValueNormalizer.normalizeToString(value));
                        case INCLUDES -> criteria = Criteria.in(column, ValueNormalizer.normalizeToCollection(value));
                        case DOES_NOT_INCLUDE -> criteria = Criteria.notIn(column, ValueNormalizer.normalizeToCollection(value));
                        default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE,
                                "i18n.analytics.invalid.criteria.operator");
                    }
                }
            }
        } else {
            LOGGER.error("Analytics: Invalid criteria for text filter column name: {}, operator type: {}", column.getColumnName(), operatorType);
            throw ErrorCode.get(ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE,
                    "i18n.analytics.invalid.criteria.operator");
        }

        return criteria;
    }

}
