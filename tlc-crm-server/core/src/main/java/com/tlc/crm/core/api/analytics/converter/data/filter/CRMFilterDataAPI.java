package com.tlc.crm.core.api.analytics.converter.data.filter;

import com.tlc.crm.core.api.analytics.converter.data.CRMReportsData;

/**
 * <p>
 * Provides an abstract way to manage the filter data
 * </p>
 *
 * @author Jamesh
 * @version 1.0
 */
public interface CRMFilterDataAPI
{
    /**
     * <p>
     * Retrieves the manager responsible for filter data
     * </p>
     *
     * @return an instance of {@link CRMReportsData} that handles the {@link CRMFilterData}
     */
    default CRMReportsData getFilterDataAPI()
    {
        return CRMFilterData.INSTANCE();
    }
}
