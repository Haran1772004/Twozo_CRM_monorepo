package com.tlc.analytics.config.manager.filter;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.model.view.filter.FilterFunctionMapper;
import com.tlc.analytics.config.model.view.filter.FilterField;

/**
 * <p>
 * The FilterConfigAPI interface provides access to the configuration managers responsible for handling filter types
 * and filter fields used in analytics views
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
public interface FilterConfigManager
{

    /**
     * Retrieves the configuration manager for {@link FilterField}
     *
     * @return The {@link AnalyticsConfigManager} instance that manages {@link FilterField} configurations.
     */
    default AnalyticsConfigManager<FilterField> getFilterFieldManager()
    {
        return FilterFieldManager.INSTANCE();
    }

    /**
     * Retrieves the configuration manager for {@link FilterFunctionMapper}
     *
     * @return The {@link AnalyticsConfigManager} instance that manages {@link FilterFunctionMapper} configurations.
     */
    default AnalyticsConfigManager<FilterFunctionMapper> getFilterFunctionManager()
    {
        return FilterFunctionMapperManager.INSTANCE();
    }
}
