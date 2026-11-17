package com.tlc.analytics.config.manager.view.layout;

import com.tlc.analytics.config.model.view.layout.DashboardLayout;
import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.sql.resource.ANADASHBOARDLAYOUT;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * The DashboardLayoutManager class is responsible for managing the configuration and persistence of {@link DashboardLayout} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class DashboardLayoutManager extends AbstractAnalyticsConfigManager<DashboardLayout>
{
    private static class Instance
    {
        private static final DashboardLayoutManager INSTANCE = new DashboardLayoutManager();
    }

    public static DashboardLayoutManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link DashboardLayout} model class
     * </p>
     *
     * @return The {@link DashboardLayout}
     */
    @Override
    protected @NonNull Class<DashboardLayout> getModelClass()
    {
        return DashboardLayout.class;
    }

    /**
     * <p>
     * Gets the {@link DashboardLayout} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANADASHBOARDLAYOUT.TABLE );
    }
}
