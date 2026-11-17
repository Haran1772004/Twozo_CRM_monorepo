package com.tlc.analytics.config.manager;

import com.tlc.analytics.config.manager.filter.FilterConfigManager;
import com.tlc.analytics.config.manager.function.FunctionConfigAPI;
import com.tlc.analytics.config.manager.table.TableConfigManager;
import com.tlc.analytics.config.manager.view.ViewConfigManager;
import com.tlc.analytics.config.manager.view.layout.LayoutConfigManager;
import com.tlc.analytics.config.manager.view.path.PathConfigManager;
import com.tlc.analytics.config.manager.view.permission.PermissionConfigManager;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Provides the abstract way to manage the access to various configuration managers for tables, views, permissions, layouts, and filters
 *
 * @author Aswath
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class AnalyticsConfigManagerImpl implements TableConfigManager,
                                                   ViewConfigManager,
                                                   PermissionConfigManager,
                                                   LayoutConfigManager,
                                                   FilterConfigManager,
                                                   PathConfigManager,
                                                   FunctionConfigAPI
{
    private static class Instance
    {
        private static final AnalyticsConfigManagerImpl INSTANCE = new AnalyticsConfigManagerImpl();
    }

    public static AnalyticsConfigManagerImpl INSTANCE()
    {
        return Instance.INSTANCE;
    }
}
