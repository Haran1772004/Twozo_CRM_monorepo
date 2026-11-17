package com.tlc.crm.core.api.analytics.manager.config.function;

import com.tlc.crm.core.api.analytics.entity.config.function.CRMFunctionFieldColumnMapper;
import com.tlc.crm.core.api.analytics.entity.config.function.FieldPeriodicFilter;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;

/**
 * <p>
 *     Provides an abstract way to deal the periodic filter functions
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface FunctionService
{
    /**
     * <p>
     * Retrieves the manager responsible for function related periodic filter field
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link FieldPeriodicFilter}
     */
    default CRMViewConfigManager<FieldPeriodicFilter> getFieldPeriodicFilterManager()
    {
        return FieldPeriodicFilterManager.INSTANCE();
    }

    /**
     * <p>
     * Retrieves the manager responsible for FunctionFieldColumnMapper
     * </p>
     *
     * @return an instance of {@link CRMViewConfigManager} that manages {@link CRMFunctionFieldColumnMapper}
     */
    default CRMViewConfigManager<CRMFunctionFieldColumnMapper> getFunctionFieldColumnMapper()
    {
        return CRMFunctionFieldColumnMapperManager.INSTANCE();
    }
}
