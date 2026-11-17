package com.tlc.analytics.config.manager.view.permission;

import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.share.ViewPermission;
import com.tlc.analytics.config.manager.AbstractAnalyticsConfigManager;
import com.tlc.analytics.sql.resource.ANAVIEWACCESSPERMISSION;

import com.tlc.sql.common.api.dml.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * The ChartViewManager class is responsible for managing the configuration and persistence of {@link ChartView} entities.
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class ViewPermissionManager extends AbstractAnalyticsConfigManager<ViewPermission>
{
    private static class Instance
    {
        private static final ViewPermissionManager INSTANCE = new ViewPermissionManager();
    }

    public static ViewPermissionManager INSTANCE()
    {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link ViewPermission} model class
     * </p>
     *
     * @return The {@link ViewPermission}
     */
    @Override
    protected @NonNull Class<ViewPermission> getModelClass()
    {
        return ViewPermission.class;
    }

    /**
     * <p>
     * Gets the {@link ANAVIEWACCESSPERMISSION} table
     * </p>
     *
     * @return The {@link Table}
     */
    @Override
    public @NonNull Table getTable()
    {
        return Table.get( ANAVIEWACCESSPERMISSION.TABLE );
    }
}
