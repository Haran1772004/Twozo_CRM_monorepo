package com.tlc.analytics.config.manager.filter;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.sql.resource.ANAFILTERFIELD;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * <p>
 * The FilterFieldManager class is responsible for managing the configuration and persistence of {@link FilterField}
 * entities
 * </p>
 *
 * @author Aswath
 * @version 1.0
 * @see AbstractAnalyticsConfigManager
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FilterFieldManager extends AbstractAnalyticsConfigManager<FilterField>
{
    private static class Instance
    {
        private static final FilterFieldManager INSTANCE = new FilterFieldManager();
    }

    public static FilterFieldManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link FilterField} model class
     * </p>
     *
     * @return The {@link FilterField}
     */
    @Override
    protected @NonNull Class<FilterField> getModelClass()
    {
        return FilterField.class;
    }

    /**
     * <p>
     * Gets the {@link FilterField} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANAFILTERFIELD.TABLE );
    }
}
