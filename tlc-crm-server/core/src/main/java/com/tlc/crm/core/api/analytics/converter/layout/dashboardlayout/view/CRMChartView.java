package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view;

import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.ChartView;

import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Responsible for managing the layout of chart views in CRM reports.
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
class CRMChartView extends CRMViewConverter<ChartView>
{

    private static final Logger LOGGER = LoggerFactory.getLogger( CRMChartView.class );
    private final AnalyticsServiceAPI analyticsServiceAPI;

    private CRMChartView()
    {
        analyticsServiceAPI = AnalyticsServiceAPI.INSTANCE();
    }


    private static class Instance
    {
        private static final CRMChartView INSTANCE = new CRMChartView();
    }

    public static CRMChartView INSTANCE()
    {
        return CRMChartView.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId             Represents an organization id
     * @param createdUserId     Represents the created user id
     * @param modifiedUserId    Represents the modified user id
     * @param createdTime       Represents the created time
     * @param modificationTime  Represents the modification time
     * @param chartUniqueId     A unique id of the chart
     * @param viewDetails       Represents the {@link ViewDetails}
     * @param reportFields      Contains the collection of {@link ReportField}
     * @param filterFields      Contains the collection of {@link FilterField}
     * @param rootTableId       Represents the root table id
     * @param reportPathMapping Represents the {@link ReportPathMapping} details
     * @param graphTypeId       Represents the graph type id
     * @param request           Holds the {@link JsonObject} details
     * @return The {@link ChartView} details
     */
    @Override
    protected final @NonNull ChartView getModel( final @NonNull Long orgId,
                                                 final @NonNull Long createdUserId,
                                                 final @NonNull Long modifiedUserId,
                                                 final @NonNull Long createdTime,
                                                 final @NonNull Long modificationTime,
                                                 final Long chartUniqueId,
                                                 final @NonNull ViewDetails viewDetails,
                                                 final @NonNull Collection<ReportField> reportFields,
                                                 final Collection<FilterField> filterFields,
                                                 final @NonNull Long rootTableId,
                                                 final @NonNull ReportPathMapping reportPathMapping,
                                                 final int graphTypeId,
                                                 final @NonNull JsonObject request )
    {
        return ChartView.builder()
                        .title( viewDetails.getDisplayName() )
                        .viewDetails( viewDetails )
                        .rootTableId( rootTableId )
                        .filterFields( filterFields )
                        .reportFields( reportFields )
                        .reportPathMapping( reportPathMapping )
                        .graphType(graphTypeId)
                        .chartUniqueId(chartUniqueId)
                        .build();
    }

    @Override
    protected Collection<ChartView> getModels( final Long orgId, final  Collection<Long> viewIds)
    {
        return analyticsServiceAPI.getChartLayout()
                                  .get( orgId, viewIds );
    }

    @Override
    protected void setViewSpecificProperties( final Optional<ChartView> model, final JsonObject response )
    {
        return;
    }
}
