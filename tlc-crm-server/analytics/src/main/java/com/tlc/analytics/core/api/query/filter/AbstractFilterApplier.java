package com.tlc.analytics.core.api.query.filter;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.HavingClause;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of {@link FilterApplier} that provides a framework for applying filters to a {@link SelectQuery}.
 *
 * @author Aswath
 * @version 1.0
 */
abstract class AbstractFilterApplier implements FilterApplier
{
    private static final Logger LOGGER = LoggerFactory.getLogger( AbstractFilterApplier.class );

    /**
     * {@inheritDoc}
     *
     * @param selectQuery           The {@link SelectQuery} to which the filter is applied.
     * @param column                The {@link Column} that the filter is applied to.
     * @param filterField           The {@link FilterField} containing filter details.
     * @param timezoneInfo          Represents the user {@link TimezoneInfo} details
     * @param columnDetailsFunction A function to retrieve {@link ColumnDetail} based on column ID.
     */
    @Override
    public final void apply( final @NonNull SelectQuery selectQuery,
                             final @NonNull Column column,
                             final @NonNull FilterField filterField,
                             final @NonNull Function<Long, ColumnDetail> columnDetailsFunction,
                             final @NonNull TimezoneInfo timezoneInfo )
    {
        final String columnName = column.getColumnName();
        LOGGER.info( "Analytics: Adding the filter field for column: {}", columnName );

        final Object value = filterField.getValue();
        Criteria criteria;

        switch (filterField.getOperatorType()) {
            case IS_ANY_OF -> criteria = Criteria.in(column, List.of(String.valueOf(value).split(",")));
            case IS_NOT_ANY_OF -> criteria = Criteria.notIn(column, List.of(String.valueOf(value).split(",")));
            default -> criteria = handleAndGetCriteria(selectQuery, column, filterField, columnDetailsFunction, timezoneInfo);
        }

        if (Objects.isNull(filterField.getHavingOperationType())) {
            addCriteria(selectQuery, criteria);
        } else {
            addHavingClause(selectQuery, criteria);
        }
        LOGGER.info("Analytics: Criteria added for filter field in column: {}", columnName);

    }

    /**
     * Adds the given {@link Criteria} to the {@link SelectQuery}.
     * If a having clause already exists, the criteria is added to it using a logical AND.
     * If no having clause exists, a new having clause is created with the given criteria.
     *
     * @param selectQuery The {@link SelectQuery} to which the criteria is added.
     * @param criteria    The {@link Criteria} to be added.
     */
    private void addHavingClause(final SelectQuery selectQuery, final Criteria criteria) {
        Optional.ofNullable(selectQuery.getHavingClause())
                .ifPresentOrElse(havingClause -> selectQuery.addHavingClause(havingClause.and(criteria)), () ->
                {
                    final HavingClause havingClause = new HavingClause(criteria);

                    selectQuery.addHavingClause(havingClause);
                });
    }

    /**
     * Adds the given {@link Criteria} to the {@link SelectQuery}.
     * If a where clause already exists, the criteria is added to it using a logical AND.
     * If no where clause exists, a new where clause is created with the given criteria.
     *
     * @param selectQuery The {@link SelectQuery} to which the criteria is added.
     * @param criteria    The {@link Criteria} to be added.
     */
    private void addCriteria( final SelectQuery selectQuery, final Criteria criteria )
    {
        Optional.ofNullable( selectQuery.getWhereClause() )
                .ifPresentOrElse( wClause -> selectQuery.setWhereClause( wClause.and( criteria ) ), () ->
                                                                     {
                                                                         final WhereClause wClause = new WhereClause( criteria );
                                                                         selectQuery.setWhereClause( wClause );
                                                                     } );
    }

    /**
     * Handling custom criteria based on the filter field
     *
     * @param selectQuery           The {@link SelectQuery} to which the criteria is applied.
     * @param column                The {@link Column} that the criteria is applied to.
     * @param filterField           The {@link FilterField} containing filter details.
     * @param columnDetailsFunction A function to retrieve {@link ColumnDetail} based on column ID.
     * @param timezoneInfo          Represents the user {@link TimezoneInfo} details
     * @return The {@link Criteria} object representing the custom filter criteria.
     */
    @NonNull
    abstract protected Criteria handleAndGetCriteria( final @NonNull SelectQuery selectQuery,
                                                      final @NonNull Column column,
                                                      final @NonNull FilterField filterField,
                                                      final @NonNull Function<Long, ColumnDetail> columnDetailsFunction,
                                                      final @NonNull TimezoneInfo timezoneInfo );
}
