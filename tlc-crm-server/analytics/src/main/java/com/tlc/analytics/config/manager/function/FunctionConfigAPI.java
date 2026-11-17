package com.tlc.analytics.config.manager.function;

import com.tlc.analytics.config.function.ConfigurableFunctionDetail;
import com.tlc.analytics.config.function.Function;
import com.tlc.analytics.config.manager.AnalyticsConfigManager;

/**
 * <p>
 * Provides the functionality for {@link FunctionConfigAPI}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public interface FunctionConfigAPI
{

    /**
     * <p>
     * Gets the {@link PresetFunctionManager} instance
     * </p>
     *
     * @return The {@link PresetFunctionManager} instance
     */
    default AnalyticsConfigManager<Function> getFunctionManager()
    {
        return PresetFunctionManager.INSTANCE();
    }

    /**
     * <p>
     * Gets the {@link ConfigurableFunctionDetail} instance
     * </p>
     *
     * @return The {@link ConfigurableFunctionDetail} instance
     */
    default AnalyticsConfigManager<ConfigurableFunctionDetail> getConfigurableManager()
    {
        return ConfigurableFunctionManager.INSTANCE();
    }
}
