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
import com.tlc.sql.olap.api.dml.OlapCriteria;
import com.tlc.sql.common.api.dml.SelectQuery;

import com.tlc.sql.common.api.meta.CommonDataType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Collections;
import java.util.function.Function;

/**
 * Implementation of {@link FilterApplier} for numeric-based filters
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class NumericFilter extends AbstractFilterApplier
{
    private static final Logger LOGGER = LoggerFactory.getLogger( NumericFilter.class );

    private static class Instance
    {
        private static final NumericFilter INSTANCE = new NumericFilter();
    }

    public static NumericFilter INSTANCE()
    {
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
    protected @NonNull Criteria handleAndGetCriteria( final @NonNull SelectQuery selectQuery,
                                                      final @NonNull Column column,
                                                      final @NonNull FilterField filterField,
                                                      final @NonNull Function<Long, ColumnDetail> columnDetailsFunction,
                                                      final @NonNull TimezoneInfo timezoneInfo )
    {
        Criteria criteria;
        final OperatorType operatorType = filterField.getOperatorType();
        Object rawValue = filterField.getValue();

        if( filterField.getFilterType() == Filter.NUMERIC )
        {
            if ( ( operatorType == OperatorType.IS || operatorType == OperatorType.IS_NOT ) )
            {
                final Collection<?> collection = ValueNormalizer.normalizeToCollection( rawValue );
                final List<Number> numberList = collection.stream()
                                                          .map( this::getNum )
                                                          .toList();

                if (Objects.equals(column.getColumnDefinition().getDataType(), CommonDataType.ARRAY)) {
                    criteria = ( operatorType == OperatorType.IS ) ? OlapCriteria.hasAny( column, numberList )
                            : OlapCriteria.notHasAny( column, numberList);
                } else {
                    criteria = ( operatorType == OperatorType.IS ) ? Criteria.in( column, numberList )
                            : Criteria.notIn( column, numberList );
                }
            }

            else
            {
                final Number value = Optional.ofNullable(ValueNormalizer.normalizeToCollection(filterField.getValue()))
                        .orElse(Collections.emptyList()).stream().findFirst().map(this::getNum).orElse(null);

                switch( filterField.getOperatorType() )
                {
                    case IS, EQUAL, IS_EMPTY -> criteria = Criteria.eq( column, value );
                    case LESS_THAN -> criteria = Criteria.lt( column, value );
                    case GREATER_THAN -> criteria = Criteria.gt( column, value );
                    case IS_NOT, NOT_EQUAL_TO, NOT_EMPTY -> criteria = Criteria.notEq( column, value );
                    case GREATER_THAN_OR_EQUAL_TO -> criteria = Criteria.ge( column, value );
                    case LESS_THAN_OR_EQUAL_TO ->  criteria = Criteria.le( column, value );

                    default ->  throw ErrorCode.get( ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE,
                            "i18n.analytics.invalid.criteria.operator" );
                }
            }
        }
        else if( filterField.getFilterType() == Filter.RANGE )
        {
            final Number from = getNum( filterField.getFrom() );
            final Number to = getNum( filterField.getTo() );
            switch( filterField.getOperatorType() )
            {
                case FROM -> criteria = Criteria.ge( column, from );
                case TO -> criteria = Criteria.le( column, to );
                case BETWEEN, BETWEEN_NUMBER -> criteria = Criteria.between( column, from, to );
                default ->  throw ErrorCode.get( ConfigErrorCodes.INVALID_CRITERIA_OPERATOR_TYPE,
                                                 "i18n.analytics.invalid.criteria.operator" );
            }
        }
        else
        {
            LOGGER.error( "Analytics: Invalid criteria for numeric filter column name: {}, operator type: {}", column.getColumnName(), filterField.getOperatorType() );
            throw ErrorCode.get( ConfigErrorCodes.INVALID_CRITERIA_FILTER_TYPE,
                                 "i18n.analytics.invalid.criteria.filter" );
        }
        return criteria;
    }

    private Number getNum( final Object value )
    {
        if( Objects.isNull( value ) )
        {
            return null;
        }
        final String strVal = String.valueOf( value );

        if( strVal.contains( "." ) )
        {
            return Double.parseDouble( strVal );
        }
        else
        {
            return Long.parseLong( strVal );
        }
    }

}
