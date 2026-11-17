package com.tlc.analytics.core.api.query.filter;

import com.tlc.analytics.config.model.table.ColumnDetail;
import com.tlc.analytics.config.model.view.filter.FilterField;

import com.tlc.l10n.temporal.model.TimezoneInfo;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.SelectQuery;
import lombok.NonNull;

import java.util.function.Function;

/**
 * Interface for applying filters to a {@link SelectQuery}.
 *
 * @author Aswath
 * @version 1.0
 **/
public interface FilterApplier
{
    /**
     * Applies the specified filter to the given column in the {@link SelectQuery}.
     * The filter is based on the details provided in the {@link FilterField}.
     *
     * @param selectQuery         The {@link SelectQuery} to which the filter is applied.
     * @param column              The {@link Column} that the filter is applied to.
     * @param filterField         The {@link FilterField} containing filter details.
     * @param columnDetailsGetter A function to retrieve {@link ColumnDetail} based on column ID.
     * @param timezoneInfo        Represents the user {@link TimezoneInfo} details
     */
    void apply( final @NonNull SelectQuery selectQuery,
                final @NonNull Column column,
                final @NonNull FilterField filterField,
                final @NonNull Function<Long, ColumnDetail> columnDetailsGetter,
                final @NonNull TimezoneInfo timezoneInfo );

    /**
     * Gets the instance of {@link FilterApplier} for text-based filters.
     *
     * @return The instance of {@link FilterApplier} for text-based filters.
     */
    static FilterApplier getTxtFilter()
    {
        return TextFilter.INSTANCE();
    }

    /**
     * Gets the instance of {@link FilterApplier} for numeric filters.
     *
     * @return The instance of {@link FilterApplier} for numeric filters.
     */
    static FilterApplier getNumericFilter()
    {
        return NumericFilter.INSTANCE();
    }

    /**
     * Gets the instance of {@link FilterApplier} for date-based filters.
     *
     * @return The instance of {@link FilterApplier} for date-based filters.
     */
    static FilterApplier getDateFilter()
    {
        return DateFilter.INSTANCE();
    }
}
