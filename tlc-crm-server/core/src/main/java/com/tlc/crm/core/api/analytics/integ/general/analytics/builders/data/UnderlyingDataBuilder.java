package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.data;

import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsConfiguredField;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsLimit;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsSort;
import com.tlc.crm.core.api.analytics.entity.data.underlying.CRMUnderlyingData;

import lombok.NonNull;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to build the underlying data
 * </p>
 *
 * @author KarthickV
 * @version 1.0
 */
public interface UnderlyingDataBuilder
{

    /**
     * <p>
     * Retrieves the layout of CRMUnderlyingData based on the specified view
     * </p>
     *
     * @param viewId                 The unique identifier for the view
     * @param moduleId               The unique identifier for the module
     * @param crmViewId              The unique identifier for the CRM child view
     * @param configuredFields       A collection of unique identifiers for the fields that are configured for the layout
     * @param configuredFieldDetails A collection of detailed configurations for each field specified in configuredFields
     * @param limit                  Optional limit on the number of records to retrieve
     * @param analyticsSort          Represents the {@link AnalyticsSort} details
     * @return A CRMUnderlyingData object representing the layout configured for the specified view and module
     */
    default CRMUnderlyingData getCRMUnderlyingDataBuilder( final @NonNull Long viewId,
                                                           final @NonNull Integer moduleId,
                                                           final @NonNull Long crmViewId,
                                                           final @NonNull Collection<Long> configuredFields,
                                                           final @NonNull Collection<AnalyticsConfiguredField> configuredFieldDetails,
                                                           final AnalyticsLimit limit,
                                                           final AnalyticsSort analyticsSort )
    {
        return UnderlyingDataBuilderImpl.getCRMUnderlyingDataLayout( viewId,
                                                                     moduleId,
                                                                     crmViewId,
                                                                     configuredFields,
                                                                     configuredFieldDetails,
                                                                     limit,
                                                                     analyticsSort );
    }

}
