package com.tlc.analytics.core.api.internal.request.view.kpi;

import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.core.api.KPIRequest;
import com.tlc.analytics.core.api.response.ResponseProvider;

/**
 * API for providing responses related to KPI.
 *
 * @author jamesh
 * @version 1.0
 */
public interface KPIResponseProviderAPI
{
    default ResponseProvider<KPIView, KPIRequest> getKPIResponseProvider()
    {
        return KPIResponseProvider.INSTANCE();
    }
}
