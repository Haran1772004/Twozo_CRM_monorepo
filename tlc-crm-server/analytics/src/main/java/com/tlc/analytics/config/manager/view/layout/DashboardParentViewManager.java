package com.tlc.analytics.config.manager.view.layout;

import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.config.model.view.layout.DashboardParentView;
import com.tlc.analytics.sql.resource.ANADASHBOARDPARENTVIEW;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * The DashboardParentViewManager class is responsible for managing the configuration and persistence of {@link DashboardParentView} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class DashboardParentViewManager extends AbstractAnalyticsConfigManager<DashboardParentView>
{
    private static class Instance
    {
        private static final DashboardParentViewManager INSTANCE = new DashboardParentViewManager();
    }

    public static DashboardParentViewManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link DashboardParentView} model class
     * </p>
     *
     * @return The {@link DashboardParentView}
     */
    @Override
    protected @NonNull Class<DashboardParentView> getModelClass()
    {
        return DashboardParentView.class;
    }

    /**
     * <p>
     * Gets the {@link DashboardParentView} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANADASHBOARDPARENTVIEW.TABLE );
    }
}
