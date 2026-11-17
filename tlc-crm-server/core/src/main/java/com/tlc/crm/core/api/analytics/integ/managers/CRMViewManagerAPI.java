package com.tlc.crm.core.api.analytics.integ.managers;

import com.tlc.crm.core.api.analytics.integ.managers.config.layout.CRMViewLayoutManagerAPI;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * <p>
 * Provides an abstract way to manage the crm view
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class CRMViewManagerAPI implements CRMViewLayoutManagerAPI
{
    private static class Instance
    {
        private static final CRMViewManagerAPI INSTANCE = new CRMViewManagerAPI();
    }

    public static CRMViewManagerAPI INSTANCE()
    {
        return CRMViewManagerAPI.Instance.INSTANCE;
    }
}
