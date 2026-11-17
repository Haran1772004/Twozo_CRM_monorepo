package com.tlc.crm.core.api.analytics.integ.general.analytics.builders.data;

import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsConfiguredField;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsLimit;
import com.tlc.analytics.config.model.view.report.dynamic.underlying.AnalyticsSort;
import com.tlc.crm.core.api.analytics.entity.data.underlying.CRMUnderlyingData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collection;

/**
 * Managing underlying data associated with CRM reports.
 *
 * @author james
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class UnderlyingDataBuilderImpl
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
    @NonNull
    protected static CRMUnderlyingData getCRMUnderlyingDataLayout( final @NonNull Long viewId,
                                                                   final @NonNull Integer moduleId,
                                                                   final @NonNull Long crmViewId,
                                                                   final @NonNull Collection<Long> configuredFields,
                                                                   final @NonNull Collection<AnalyticsConfiguredField> configuredFieldDetails,
                                                                   final AnalyticsLimit limit,
                                                                   final AnalyticsSort analyticsSort)
    {
        return CRMUnderlyingData.builder()
                                .viewId( viewId )
                                .moduleId( moduleId )
                                .childViewId( crmViewId )
                                .configuredFields( configuredFields )
                                .configuredFieldDetails( configuredFieldDetails )
                                .limit( limit )
                                .analyticsSort( analyticsSort )
                                .build();
    }

}
