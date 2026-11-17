package com.tlc.crm.core.api.analytics.manager;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRMapperColumn;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRViewFieldMapper;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;

import lombok.NonNull;

/**
 * <p>
 * Manages CRM Manager API
 * </p>
 *
 * @author James
 * @version 1.0
 **/
public interface CRMRIntegrationManagerAPI
{
    /**
     * Gets the configuration manager for {@link CRMRViewFieldMapper}
     *
     * @return the configuration manager for {@link CRMRViewFieldMapper}
     */
    default @NonNull CRMViewConfigManager<CRMRViewFieldMapper> getViewFieldMapper() {
        return ViewFieldMapperManager.INSTANCE();
    }

    /**
     * Gets the configuration manager for system modular-field column mapping.
     *
     * @return configuration manager for {@link CRMSystemModularFieldColumnMapping}
     */
    default @NonNull CRMViewConfigManager<CRMSystemModularFieldColumnMapping> getSystemFieldColumnMapping() {
        return CRMSystemModularFieldColumnMappingManager.INSTANCE();
    }

    /**
     * Gets the configuration manager for custom modular-field column mapping.
     *
     * @return configuration manager for {@link CRMCustomModularFieldColumnMapping}
     */
    default @NonNull CRMViewConfigManager<CRMCustomModularFieldColumnMapping> getCustomFieldColumnMapping() {
        return CRMCustomModularFieldColumnMappingManager.INSTANCE();
    }

    /**
     * Gets the configuration manager for {@link CRMRMapperColumn}
     *
     * @return the configuration manager for {@link CRMRMapperColumn}
     */
    default @NonNull CRMViewConfigManager<CRMRMapperColumn> getMapperColumn() {
        return MapperColumnManager.INSTANCE();
    }
}
