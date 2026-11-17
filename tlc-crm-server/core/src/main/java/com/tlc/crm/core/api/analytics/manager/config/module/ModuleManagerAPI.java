package com.tlc.crm.core.api.analytics.manager.config.module;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModule;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleAccess;
import com.tlc.crm.core.api.analytics.entity.config.modules.ModularField;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMModuleConnector;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSubModuleMapper;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMMeasureType;

/**
 * <p>
 * Provides an abstract way to manage the module
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface ModuleManagerAPI
{
    /**
     * <p>
     * Retrieves the manager responsible for handling module
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link CRMModule}
     */
    default CRMViewConfigManager<CRMModule> getModuleManager()
    {
        return ModuleManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling modular fields
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link ModularField}
     */
    default CRMViewConfigManager<ModularField> getModularFieldManager()
    {
        return ModularFieldManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling modular column mapping
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link CRMModuleConnector}
     */
    default CRMViewConfigManager<CRMModuleConnector> getModuleConnectorManager()
    {
        return CRMModuleConnectorManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling module measure fields
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link CRMMeasureType}
     */
    default CRMViewConfigManager<CRMMeasureType> getMeasureTypeManager()
    {
        return CRMMeasureTypeManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling module
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link CRMModule}
     */
    default CRMViewConfigManager<CRMModuleAccess> getModuleAccessManager()
    {
        return CRMModuleAccessManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for handling subModule mapping
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link CRMSubModuleMapper}
     */
    default CRMViewConfigManager<CRMSubModuleMapper> getSubModuleMapperManager()
    {
        return CRMSubModuleMapperManager.INSTANCE();
    }
}
