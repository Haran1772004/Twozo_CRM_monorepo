package com.tlc.crm.core.api.analytics.integ.managers;

import com.tlc.crm.core.api.analytics.integ.managers.template.CRMTemplateLayoutManager;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * CRMTemplateManagerAPI provides a singleton API for managing CRM template layouts.
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CRMTemplateManagerAPI implements CRMTemplateLayoutManager {
    private static class Instance {
        private static final CRMTemplateManagerAPI INSTANCE = new CRMTemplateManagerAPI();
    }

    public static CRMTemplateManagerAPI INSTANCE() {
        return CRMTemplateManagerAPI.Instance.INSTANCE;
    }
}
