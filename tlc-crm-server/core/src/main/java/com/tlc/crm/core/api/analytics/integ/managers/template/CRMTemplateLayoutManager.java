package com.tlc.crm.core.api.analytics.integ.managers.template;

import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMTemplateDetails;

/**
 * <p>
 * Defines the contract for managing CRM template layouts within the analytics dashboard.
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
public interface CRMTemplateLayoutManager {
    /**
     * <p>
     * Gets the {@link CRMViewConfigManager} instance
     * </p>
     *
     * @return The {@link CRMTemplateManager} instance
     */
    default CRMViewConfigManager<CRMTemplateDetails> getCRMTemplatedManager() {
        return CRMTemplateManager.INSTANCE();
    }
}

