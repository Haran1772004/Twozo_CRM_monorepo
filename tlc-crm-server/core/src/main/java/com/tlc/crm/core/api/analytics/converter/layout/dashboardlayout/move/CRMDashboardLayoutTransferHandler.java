package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.move;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.model.view.layout.DashboardParentView;
import com.tlc.analytics.sql.resource.ANADASHBOARDPARENTVIEW;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.functions.SafeFunction;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;

import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import com.tlc.crm.core.api.analytics.integ.general.analytics.builders.CRMReportsBuilder;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutPosition;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMANAVIEWMAPPER;
import com.tlc.crm.core.sql.resource.CRMCHILDVIEWDETAILS;

import com.tlc.crm.core.sql.resource.DASHBOARDLAYOUTPOSITION;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.entity.EntityManager;
import com.tlc.entity.EntityManagerFactory;

import com.tlc.pk.transfer.model.EntityTransferConfig;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.ds.OrgDataStore;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.LongStream;

/**
 * <p>
 * Handles the transfer operation for {@link DashboardParentView}
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public class CRMDashboardLayoutTransferHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger( CRMDashboardLayoutTransferHandler.class );
    private static final long DEFAULT_CHART_POSITION = 1L;
    private static final long DASHBOARD_MIN_LIMIT = 1L;

    private final AnalyticsServiceAPI analyticsServiceAPI;
    private final CRMReportsBuilder crmReportsBuilder;
    private final CRMViewManagerAPI crmViewManagerAPI;
    private final CRMViewConfigManager<DashboardLayoutPosition> dashboardLayoutPositionManager;
    private final long dashboardMaxLimit;

    private CRMDashboardLayoutTransferHandler()
    {
        this.analyticsServiceAPI = AnalyticsServiceAPI.INSTANCE();
        this.crmReportsBuilder = CRMReportsBuilder.INSTANCE();
        this.crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        this.dashboardLayoutPositionManager = crmViewManagerAPI.getDashboardLayoutManager();
        dashboardMaxLimit = NumberUtils.parseNumber( ConfHolder.getInstance()
                                                               .getProperties( CRMReportKeys.ANALYTICS )
                                                               .getNestedConfigs( CRMReportKeys.DASHBOARD )
                                                               .getConfig( CRMReportKeys.VIEW_LIMIT ) )
                                       .longValue();
    }

    private static class Instance
    {
        private static final CRMDashboardLayoutTransferHandler CRM_DASHBOARD_LAYOUT_TRANSFER_HANDLER = new CRMDashboardLayoutTransferHandler();
    }

    public static CRMDashboardLayoutTransferHandler getInstance()
    {
        return Instance.CRM_DASHBOARD_LAYOUT_TRANSFER_HANDLER;
    }

    /**
     * <p>
     * Gets the {@link EntityTransferConfig} based on the given dashboard id
     * </p>
     *
     * @param orgId   Represents an organisation id
     * @param request Holds the {@link JsonObject}
     * @return The {@link EntityTransferConfig}
     */
    public EntityTransferConfig<Long, CRMChildViewDetails> getEntityConfig( final @NonNull Long orgId, final JsonObject request ) throws Exception
    {
        final Long sourceDashboardId = request.getLong( CRMDashboardKeys.SOURCE_DASHBOARD_ID );
        final Long targetDashboardId = request.getLong( CRMDashboardKeys.TARGET_DASHBOARD_ID );
        final Long viewId = request.getLong( CRMReportKeys.VIEW_ID );
        final Supplier<OrgDataStore> supplier = () -> DataStoreProvider.getSecureOrgDataStore( orgId );
        final EntityManager<Long, CRMChildViewDetails> entityManager = EntityManagerFactory.create( CRMChildViewDetails.class, supplier );

        LOGGER.info( "CRM Analytics: Fetched the dashboard parent view for orgId:{}, dashboard id:{}", orgId, sourceDashboardId );

       final CRMDashboardDetails targetDashboardDetails = crmViewManagerAPI.getCRMDashboardManager()
                                                                            .get( orgId, targetDashboardId );

        final SafeFunction<CRMChildViewDetails, CRMChildViewDetails> dashboardParentViewSafeFunction = crmChildViewDetails ->
        {
            final CRMViewConfigManager<CRMANAViewMapper> crmViewConfigManager = crmViewManagerAPI.getCRMANAViewManager();
            final CRMDashboardDetails sourceDashboardDetails = crmViewManagerAPI.getCRMDashboardManager()
                                                                                .get( orgId, sourceDashboardId );
            final DashboardLayoutPosition dashboardLayoutPosition = dashboardLayoutPositionManager.getAll(orgId,
                        Criteria.eq( dashboardLayoutPositionManager.getTable().getColumn(DASHBOARDLAYOUTPOSITION.DASHBOARD_ID), sourceDashboardDetails.getReportsDashboardId()))
                .stream()
                .filter(dashboardLayoutPos -> Objects.equals(viewId, dashboardLayoutPos.getViewId()))
                .findFirst().get();

            final DashboardLayoutPosition newDashboardLayoutPosition = DashboardLayoutPosition.builder()
                    .layoutAsString(dashboardLayoutPosition.getLayoutAsString())
                    .dashboardId(targetDashboardDetails.getReportsDashboardId())
                    .viewId(viewId)
                    .position(findCRMChildViewPosition(orgId, targetDashboardId))
                    .build();

            final CRMANAViewMapper crmANAViewMapper= crmViewConfigManager.getAll( orgId,
                                                                                  Criteria.eq( crmViewConfigManager.getTable()
                                                                                                                   .getColumn( CRMANAVIEWMAPPER.CRM_VIEW_ID ),
                                                                                              viewId ) )
                                                                         .stream()
                                                                         .findFirst()
                                                                         .orElseThrow( () -> ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_ID,
                                                                                                            "i18n.analytics.config.view.invalid.id" ) );
            final Long anaViewDetailsId = crmANAViewMapper.getAnaViewDetailsId();
            final AnalyticsConfigManager<DashboardParentView> dashboardParentViewManager = analyticsServiceAPI.getDashboardParentView();
            final Collection<DashboardParentView> dashboardParentViewList = dashboardParentViewManager.getAll( orgId,
                                                                                                               Criteria.eq( dashboardParentViewManager.getTable()
                                                                                                                                                      .getColumn( ANADASHBOARDPARENTVIEW.DASH_ID ),
                                                                                                                            sourceDashboardDetails.getReportsDashboardId() ) );
            LOGGER.info( "CRM Analytics: Fetched the dashboard parent view for orgId:{}, dashboard id:{}", orgId, sourceDashboardId );

            final DashboardParentView dashboardParentView = dashboardParentViewList.stream()
                                                                                   .filter( parentView -> Objects.equals( anaViewDetailsId, parentView.getChildViewId() ) )
                                                                                   .findFirst()
                                                                                   .orElseThrow( () -> ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_ID,
                                                                                                                      "i18n.analytics.config.view.invalid.id" ) );

            final DashboardParentView newDashboardParentView = crmReportsBuilder.getDashboardParentView( orgId,
                                                                                                         findANAParentViewPosition( orgId, targetDashboardId ),
                                                                                                         dashboardParentView.getColIdx(),
                                                                                                         dashboardParentView.getTitle(),
                                                                                                         dashboardParentView.getChildViewId(),
                                                                                                         NumberUtils.parseNumber( dashboardParentView.getType()
                                                                                                                                                     .getTypeCode() )
                                                                                                                    .intValue(),
                                                                                                         dashboardParentView.getWidth(),
                                                                                                         dashboardParentView.getHeight(),
                                                                                                         targetDashboardDetails.getReportsDashboardId(),
                                                                                                         dashboardParentView.id() );

            analyticsServiceAPI.getDashboardParentView()
                               .update( newDashboardParentView );

            return CRMChildViewDetails.builder()
                                      .crmViewPosition( findCRMChildViewPosition( orgId, targetDashboardId ) )
                                      .createdTime( crmChildViewDetails.getCreatedTime() )
                                      .modifiedBy( crmChildViewDetails.getModifiedBy() )
                                      .name( crmChildViewDetails.getName() )
                                      .createdBy( crmChildViewDetails.getCreatedBy() )
                                      .crmDashboardId( targetDashboardId )
                                      .crmViewMapper( crmChildViewDetails.getCrmViewMapper() )
                                      .crmViewField( crmChildViewDetails.getCrmViewField() )
                                      .description( crmChildViewDetails.getDescription() )
                                      .modifiedTime( crmChildViewDetails.getModifiedTime() )
                                      .viewTypeId( crmChildViewDetails.getViewTypeId() )
                                      .id( crmChildViewDetails.id() )
                                      .orgId( crmChildViewDetails.orgId() )
                                      .dashboardLayoutPosition(newDashboardLayoutPosition)
                                      .build();
        };

        return EntityTransferConfig.<Long, CRMChildViewDetails>builder()
                                   .entityManager( entityManager )
                                   .entityCustomizer( dashboardParentViewSafeFunction )
                                   .sourceId( viewId )
                                   .build();
    }

    /**
     * <p>
     * Finds the dashboard parent view position for given target dashboard id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param dashboardId Represents the target dashboard id
     * @return The dashboard parent view position
     */
    private long findANAParentViewPosition( final Long orgId, final Long dashboardId )
    {
        final AnalyticsConfigManager<DashboardParentView> dashboardParentViewManager = analyticsServiceAPI.getDashboardParentView();
        final Collection<Long> dashboardParentViewList = dashboardParentViewManager.getAll( orgId, Criteria.eq( dashboardParentViewManager.getTable()
                                                                                                                                          .getColumn( ANADASHBOARDPARENTVIEW.DASH_ID ),
                                                                                                                dashboardId ) )
                                                                                   .stream()
                                                                                   .map( DashboardParentView::getRowIdx )
                                                                                   .toList();

        return dashboardParentViewList.isEmpty() ? DEFAULT_CHART_POSITION
                                                 : LongStream.rangeClosed( DASHBOARD_MIN_LIMIT, dashboardMaxLimit )
                                                             .filter( position -> !dashboardParentViewList.contains( position ) )
                                                             .findFirst()
                                                             .orElseGet( () -> DEFAULT_CHART_POSITION );
    }

    /**
     * <p>
     * Finds the crm child view details position for given target dashboard id
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param dashboardId Represents the target dashboard id
     * @return The dashboard parent view position
     */
    private long findCRMChildViewPosition( final Long orgId, final Long dashboardId )
    {
        try
        {
            final CRMViewConfigManager<CRMChildViewDetails> crmChildViewManager = crmViewManagerAPI.getCRMChildViewManager();
            final Collection<Long> crmChildViewDetailsList = crmChildViewManager.getAll( orgId, Criteria.eq( crmChildViewManager.getTable()
                                                                                                                                .getColumn( CRMCHILDVIEWDETAILS.CRM_DASHBOARD_ID ),
                                                                                                             dashboardId ) )
                                                                                .stream()
                                                                                .map( CRMChildViewDetails::getCrmViewPosition )
                                                                                .toList();

            return crmChildViewDetailsList.isEmpty() ? DEFAULT_CHART_POSITION
                                                     : LongStream.rangeClosed( DASHBOARD_MIN_LIMIT, dashboardMaxLimit )
                                                                 .filter( position -> !crmChildViewDetailsList.contains( position ) )
                                                                 .findFirst()
                                                                 .orElseGet( () -> DEFAULT_CHART_POSITION );
        }
        catch( Exception exception )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_VIEW_ID,
                                 "i18n.analytics.config.view.invalid.id" );
        }
    }
}
