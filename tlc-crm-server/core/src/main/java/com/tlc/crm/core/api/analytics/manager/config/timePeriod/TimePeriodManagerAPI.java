package com.tlc.crm.core.api.analytics.manager.config.timePeriod;

import com.tlc.crm.core.api.analytics.entity.config.timePeriod.CRMTimePeriodField;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;

/**
 * <p>
 * Manages the {@link  TimePeriodFieldManager} API
 * </p>
 *
 * @author Ryan Sam Daniel
 * @version 1.0
 */
public interface TimePeriodManagerAPI {
    /**
     * <p>
     * Retrieves the manager responsible for handling timePeriodFields
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link CRMTimePeriodField}
     */
    default CRMViewConfigManager<CRMTimePeriodField> getTimePeriodFieldManager()
    {
        return TimePeriodFieldManager.INSTANCE();
    }
}
