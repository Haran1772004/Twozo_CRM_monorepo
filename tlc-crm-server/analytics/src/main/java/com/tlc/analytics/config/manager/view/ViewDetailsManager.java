package com.tlc.analytics.config.manager.view;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.sql.resource.ANAVIEWDETAILS;

import com.tlc.sql.common.api.dml.Table;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * <p>
 * The ViewDetailsManager class is responsible for managing the configuration and persistence of {@link ViewDetails} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors( fluent = true )
@RequiredArgsConstructor
class ViewDetailsManager extends AbstractAnalyticsConfigManager<ViewDetails>
{

    private static class Instance
    {
        private static final ViewDetailsManager INSTANCE = new ViewDetailsManager();
    }

    public static ViewDetailsManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link ViewDetails} model class
     * </p>
     *
     * @return The {@link ViewDetails}
     */
    @Override
    protected @NonNull Class<ViewDetails> getModelClass()
    {
        return ViewDetails.class;
    }

    /**
     * <p>
     * Gets the {@link ViewDetails} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANAVIEWDETAILS.TABLE );
    }
}
