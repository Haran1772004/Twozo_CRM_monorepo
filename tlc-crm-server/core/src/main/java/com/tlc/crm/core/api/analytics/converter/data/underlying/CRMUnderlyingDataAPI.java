package com.tlc.crm.core.api.analytics.converter.data.underlying;

import com.tlc.crm.core.api.analytics.converter.data.CRMReportsData;

/**
 * <p>
 * Provides an abstract way to manage the underlying data
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public interface CRMUnderlyingDataAPI
{
    /**
     * <p>
     * Retrieves the manager responsible for underlying data
     * </p>
     *
     * @return an instance of {@link CRMReportsData} that handles the {@link CRMUnderlyingDataConverter}
     */
    default CRMReportsData underlyingDataLayoutAPI()
    {
        return CRMUnderlyingDataConverter.INSTANCE();
    }
}
