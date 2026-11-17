package com.tlc.analytics.config.manager.view;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.sql.resource.ANAREPORT;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * <p>
 * The WidgetViewManager class is responsible for managing the configuration and persistence of {@link KPIView} entities.
 * </p>
 *
 * @author James
 * @version 1.0
 */
@Accessors( fluent = true )
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class KPIViewManager extends AbstractAnalyticsConfigManager<KPIView>
{
    private static class Instance
    {
        private static final KPIViewManager INSTANCE = new KPIViewManager();
    }

    public static KPIViewManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link KPIView} model class
     * </p>
     *
     * @return The {@link KPIView}
     */
    @NonNull
    @Override
    protected  Class<KPIView> getModelClass()
    {
        return KPIView.class;
    }

    /**
     * <p>
     * Gets the {@link ANAREPORT} table
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
