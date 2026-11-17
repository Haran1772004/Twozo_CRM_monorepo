package com.tlc.crm.core.api.analytics.manager.data.underlying;

import com.tlc.crm.core.api.analytics.entity.data.underlying.CRMDefaultFieldConfig;
import com.tlc.crm.core.api.analytics.entity.data.underlying.CRMFieldConfig;
import com.tlc.crm.core.api.analytics.entity.data.underlying.MapperTableDetails;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;

/**
 * <p>
 * Provides the abstract way to handle the crm field configuration
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface CRMFieldConfigManagerAPI
{
    /**
     * <p>
     * Returns an instance of the {@link CRMViewConfigManager} for field config actions
     * </p>
     *
     * @return the {@link CRMViewConfigManager} instance for field config actions
     */
    default CRMViewConfigManager<CRMFieldConfig> getFieldConfigManager()
    {
        return CRMFieldConfigManager.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the {@link CRMViewConfigManager} for field config actions
     * </p>
     *
     * @return the {@link CRMViewConfigManager} instance for field config actions
     */
    default CRMViewConfigManager<CRMDefaultFieldConfig> getDefaultFieldConfigManager()
    {
        return CRMDefaultFieldConfigManager.INSTANCE();
    }

    /**
     * <p>
     * Returns an instance of the {@link CRMViewConfigManager} for field mapper table details
     * </p>
     *
     * @return the {@link CRMViewConfigManager} instance for field mapper table details
     */
    default CRMViewConfigManager<MapperTableDetails> getFieldMapperTableDetails()
    {
        return MapperTableManager.INSTANCE();
    }
}
