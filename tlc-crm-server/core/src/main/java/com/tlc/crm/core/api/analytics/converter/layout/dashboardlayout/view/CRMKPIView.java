package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view;

import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparator;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorSide;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorType;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;

import com.tlc.commons.json.Json;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMKPIKeys;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Responsible for managing the layout of kpi views in CRM reports.
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
class CRMKPIView extends CRMViewConverter<KPIView>
{
    private final AnalyticsServiceAPI analyticsServiceAPI;

    private CRMKPIView()
    {
        analyticsServiceAPI = AnalyticsServiceAPI.INSTANCE();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger( CRMKPIView.class );

    private static class Instance
    {
        private static final CRMKPIView INSTANCE = new CRMKPIView();
    }

    public static CRMKPIView INSTANCE()
    {
        return CRMKPIView.Instance.INSTANCE;
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
     * @return The {@link KPIView} details
     */
    @Override
    protected final @NonNull KPIView getModel( final @NonNull Long orgId,
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

        final JsonObject viewPropertiesJson = request.getJsonObject( CRMChildViewKeys.VIEW_PROPERTIES );

        final JsonObject durationJson = viewPropertiesJson.getJsonObject( CRMKPIKeys.DURATION );
        final JsonObject comparatorJson = viewPropertiesJson.getJsonObject( CRMKPIKeys.COMPARE_TO );

        final int comparatorTypeId = comparatorJson.getInt( CRMKPIKeys.COMPARE_UNIT_ID);

        final KPIComparator leftKPIComparator = KPIComparator.builder()
                                                             .comparatorSide( KPIComparatorSide.LEFT.getTypeCode())
                                                             .comparatorType(durationJson.getInt( CRMKPIKeys.TIME_UNIT_ID ))
                                                             .createdTime( createdTime )
                                                             .modifiedTime( modificationTime )
                                                             .from( durationJson.optLong( CRMKPIKeys.FROM, 0 ) )
                                                             .to( durationJson.optLong( CRMKPIKeys.TO, 0 ) )
                                                             .build();

        final KPIComparator rightKPIComparator = KPIComparator.builder()
                                                              .comparatorSide( KPIComparatorSide.RIGHT.getTypeCode() )
                                                              .comparatorType(comparatorTypeId)
                                                              .createdTime( createdTime )
                                                              .modifiedTime( modificationTime )
                                                              .from( comparatorJson.optLong( CRMKPIKeys.FROM, 0 ) )
                                                              .to( comparatorJson.optLong( CRMKPIKeys.TO, 0 ) )
                                                              .build();

        return  KPIView.builder()
                        .title( viewDetails.getDisplayName() )
                        .viewDetails( viewDetails )
                        .filterFields( filterFields )
                        .reportFields( reportFields )
                        .rootTableId( rootTableId )
                        .reportPathMapping( reportPathMapping )
                        .comparators( List.of( leftKPIComparator, rightKPIComparator ) )
                        .graphType(graphTypeId)
                        .build();
    }


    @Override
    protected Collection<KPIView> getModels( final @NonNull Long orgId, final @NonNull Collection<Long> viewIds )
    {
        return analyticsServiceAPI.getKPILayout()
                                  .get( orgId, viewIds );
    }

    @Override
    protected void setViewSpecificProperties( final Optional<KPIView> model, final JsonObject response )
    {
        final Collection<KPIComparator> widgetComparators = model.get().getComparators();

        if( Objects.nonNull(widgetComparators) )
        {
            for( final KPIComparator widgetComparator : widgetComparators )
            {
                if( widgetComparator.getComparatorSide().equals( KPIComparatorSide.RIGHT ) )
                {
                    final JsonObject jsonObject = Json.object();
                    final KPIComparatorType kpiComparatorType = widgetComparator.getComparatorType();

                    jsonObject.put( CRMKPIKeys.COMPARE_UNIT_ID, kpiComparatorType.getTypeCode());
                    jsonObject.put( CRMKPIKeys.FROM, widgetComparator.getFrom() );
                    jsonObject.put( CRMKPIKeys.TO, widgetComparator.getTo() );

                    if( response.containsKey( CRMChildViewKeys.VIEW_PROPERTIES ) )
                    {
                        final JsonObject viewPropertiesJson = response.getJsonObject( CRMChildViewKeys.VIEW_PROPERTIES );

                        viewPropertiesJson.put( CRMKPIKeys.COMPARE_TO, jsonObject );
                    }
                }

                if( widgetComparator.getComparatorSide().equals( KPIComparatorSide.LEFT ) )
                {
                    final JsonObject viewPropertiesJson = response.getJsonObject( CRMChildViewKeys.VIEW_PROPERTIES );

                    final JsonObject durationResponse = viewPropertiesJson.getJsonObject( CRMKPIKeys.DURATION );

                    durationResponse.put( CRMKPIKeys.FROM, widgetComparator.getFrom() );
                    durationResponse.put( CRMKPIKeys.TO, widgetComparator.getTo() );
                }
            }
        }
    }
}
