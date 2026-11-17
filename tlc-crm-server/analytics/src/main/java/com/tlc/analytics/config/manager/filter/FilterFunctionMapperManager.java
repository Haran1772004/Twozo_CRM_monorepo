package com.tlc.analytics.config.manager.filter;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.FilterFunctionMapper;
import com.tlc.analytics.sql.resource.ANAFILTERFUNCTIONMAPPER;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * <p>
 * A provides an abstract way to manage the configuration and persistence of {@link FilterFunctionMapper} entities
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see AbstractAnalyticsConfigManager
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class FilterFunctionMapperManager extends AbstractAnalyticsConfigManager<FilterFunctionMapper>
{
    private static class Instance
    {
        private static final FilterFunctionMapperManager INSTANCE = new FilterFunctionMapperManager();
    }

    public static FilterFunctionMapperManager INSTANCE()
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
    protected @NonNull Class<FilterFunctionMapper> getModelClass()
    {
        return FilterFunctionMapper.class;
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
        return Table.get( ANAFILTERFUNCTIONMAPPER.TABLE );
    }
}
