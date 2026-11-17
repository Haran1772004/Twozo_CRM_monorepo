package com.tlc.analytics.config.manager.view;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.sql.resource.ANAREPORT;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * <p>
 * The ChartViewManager class is responsible for managing the configuration and persistence of {@link ChartView} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ChartViewManager extends AbstractAnalyticsConfigManager<ChartView>
{
    private static class Instance
    {
        private static final ChartViewManager INSTANCE = new ChartViewManager();
    }

    public static ChartViewManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link ChartView} model class
     * </p>
     *
     * @return The {@link ChartView}
     */
    @Override
    protected @NonNull Class<ChartView> getModelClass()
    {
        return ChartView.class;
    }

    /**
     * <p>
     * Gets the {@link ChartView} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANAREPORT.TABLE );
    }
}
