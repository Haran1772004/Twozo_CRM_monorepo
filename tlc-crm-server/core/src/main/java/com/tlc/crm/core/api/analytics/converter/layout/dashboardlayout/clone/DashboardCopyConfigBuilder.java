package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.clone;

import com.tlc.analytics.config.manager.AnalyticsConfigManager;
import com.tlc.analytics.config.manager.AnalyticsConfigManagerImpl;
import com.tlc.analytics.config.model.view.ViewDetails;
import com.tlc.analytics.config.model.view.filter.FilterField;
import com.tlc.analytics.config.model.view.filter.FilterFunctionMapper;
import com.tlc.analytics.config.model.view.layout.DashboardLayout;
import com.tlc.analytics.config.model.view.layout.DashboardParentView;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.KPIView;
import com.tlc.analytics.config.model.view.report.ReportField;
import com.tlc.analytics.config.model.view.report.ReportFieldCustomRange;
import com.tlc.analytics.config.model.view.report.ReportType;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparator;
import com.tlc.analytics.config.model.view.report.kpi.KPIComparatorSide;
import com.tlc.analytics.config.model.view.report.path.PathInfo;
import com.tlc.analytics.config.model.view.report.path.RelationsInPath;
import com.tlc.analytics.config.model.view.report.path.ReportPathMapping;
import com.tlc.analytics.sql.resource.ANADASHBOARDPARENTVIEW;
import com.tlc.analytics.sql.resource.ANAREPORT;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.functions.SafeFunction;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.entity.config.fields.bucket.FieldBucketUnit;
import com.tlc.crm.core.api.analytics.entity.config.function.FieldPeriodicFilter;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMDashboardKeys;
import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewConfigManager;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.CRMDashboardDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutPosition;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRMapperColumn;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.CRMRViewFieldMapper;
import com.tlc.crm.core.api.analytics.manager.CRMReportManagerAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.sql.resource.CRMFIELDBUCKETUNIT;
import com.tlc.crm.core.sql.resource.CRMFIELDPERIODICFILTER;
import com.tlc.crm.core.sql.resource.CRMREPORTVIEWFIELDMAPPER;
import com.tlc.crm.core.sql.resource.DASHBOARDLAYOUTPOSITION;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.entity.EntityManager;
import com.tlc.entity.EntityManagerFactory;

import com.tlc.pk.copy.CopyHandler;
import com.tlc.pk.copy.EntityCopyConfig;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Contract for building {@link EntityCopyConfig} within the CRM reporting framework.
 *
 * @author KarthickV
 * @version 1.0
 */
public class DashboardCopyConfigBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger( DashboardCopyConfigBuilder.class );

    private final CRMViewManagerAPI crmViewManagerAPI;
    private final AnalyticsServiceAPI analyticsServiceAPI;
    private final CRMReportManagerAPI crmReportManagerAPI;
    private final AnalyticsConfigManagerImpl analyticsConfigManager;
    private final CRMReportsAPI crmReportsAPI;
    private final CRMViewConfigManager<DashboardLayoutPosition> dashboardLayoutPositionManager;
    private final CRMViewConfigManager<FieldPeriodicFilter> periodicFilterManager;
    private final CRMViewConfigManager<CRMDashboardDetails> crmDashboardManager;
    private final AnalyticsConfigManager<DashboardParentView> dashboardParentViewManager;

    private DashboardCopyConfigBuilder()
    {
        crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        analyticsServiceAPI = AnalyticsServiceAPI.INSTANCE();
        crmReportManagerAPI = CRMReportManagerAPI.INSTANCE();
        analyticsConfigManager = AnalyticsConfigManagerImpl.INSTANCE();
        crmReportsAPI = CRMReportsAPI.getInstance();
        dashboardLayoutPositionManager = crmViewManagerAPI.getDashboardLayoutManager();
        this.periodicFilterManager = crmReportManagerAPI.getFieldPeriodicFilterManager();
        this.crmDashboardManager = crmViewManagerAPI.getCRMDashboardManager();
        this.dashboardParentViewManager = analyticsConfigManager.getDashboardParentViewManager();
    }

    private static class Instance
    {
        private static final DashboardCopyConfigBuilder INSTANCE = new DashboardCopyConfigBuilder();
    }

    public static DashboardCopyConfigBuilder INSTANCE()
    {
        return DashboardCopyConfigBuilder.Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link EntityCopyConfig} object based on the provided organization ID and request data
     * </p>
     *
     * @param orgId   Represents the organization ID
     * @param request Holds the request details
     * @param dataContainer    Holds the rows that will get committed
     * @return A {@link EntityCopyConfig} object representing the customized dashboard copy configuration
     */
    public EntityCopyConfig<Long, CRMDashboardDetails> getDashboardCopyConfig( final @NonNull Long orgId,
                                                                               final @NonNull JsonObject request,
                                                                               final DataContainer dataContainer,
                                                                               final Long userId )
    {
        final Long existingDashboardId = request.getLong( CRMReportKeys.DASHBOARD_ID );
        final Supplier<OrgDataStore> supplier = () -> DataStoreProvider.getSecureOrgDataStore( orgId );
        final EntityManager<Long, CRMDashboardDetails> entityManager = EntityManagerFactory.create( CRMDashboardDetails.class, supplier );

        final SafeFunction<CRMDashboardDetails, CRMDashboardDetails> customizer = dashboardDetails ->
        {
            final long currentTime = Instant.now()
                                            .toEpochMilli();
            final DashboardLayout dashboardLayout = analyticsServiceAPI.getDashboardLayout()
                                                                       .get( orgId, dashboardDetails.getReportsDashboardId() );

            final DashboardLayout newDashboardlayout = DashboardLayout.builder()
                                                          .orgId( orgId )
                                                          .isSharable( dashboardLayout.isSharable() )
                                                          .viewDetails( getViewDetails( orgId,
                                                                                        userId,
                                                                                        request.getString( CRMDashboardKeys.DASHBOARD_NAME ),
                                                                                        request.optString( CRMDashboardKeys.DESCRIPTION, "" ),
                                                                                        dashboardLayout.getViewDetails() ) )
                                                          .build();

            analyticsServiceAPI.getDashboardLayout()
                               .loadDataIntoContainer( List.of( newDashboardlayout ), dataContainer );

            return CRMDashboardDetails.builder()
                                      .name( request.getString( CRMDashboardKeys.DASHBOARD_NAME ) )
                                      .isCurated( false )
                                      .modifiedTime( currentTime )
                                      .modifiedBy( userId )
                                      .markAsTrash( false )
                                      .createdBy( userId )
                                      .createdTime( currentTime )
                                      .reportsDashboardId( newDashboardlayout.id() )
                                      .description( request.optString( CRMDashboardKeys.DESCRIPTION, "" ) )
                                      .orgId( orgId )
                                      .build();
        };
        final CopyHandler<CRMDashboardDetails> copyHandler = new CopyHandler.DefaultHandler<>()
        {
            @Override
            public void afterCopy( final @NonNull CRMDashboardDetails newCrmDashboardDetails )
            {
                try
                {
                    setDashboardDetails( orgId, existingDashboardId, newCrmDashboardDetails.getReportsDashboardId(),
                                         newCrmDashboardDetails, dataContainer, userId );
                }
                catch( Exception exception )
                {
                    throw ErrorCode.get( AnalyticsErrorCodes.CLONE_ACTION_FAILED,
                                         "i18n.analytics.clone.action.failed" );
                }
            }
        };

        return EntityCopyConfig.<Long, CRMDashboardDetails>builder()
                               .copyHandler( copyHandler)
                               .entityCustomizer( customizer )
                               .entityManager( entityManager )
                               .sourceId( existingDashboardId )
                               .build();
    }

    /**
     * <p>
     * Builds the new dashboard details for given existing dashboard
     * </p>
     *
     * @param existingDashboardId Represents the existing dashboard id
     * @param reportDashboardId     Represents the {@link DashboardLayout} details
     * @param crmDashboardDetails Represents the {@link CRMDashboardDetails}
     * @param dataContainer    Holds the rows that will get committed
     */
    private void setDashboardDetails( final long orgId,
                                      final Long existingDashboardId,
                                      final Long reportDashboardId,
                                      final CRMDashboardDetails crmDashboardDetails,
                                      final DataContainer dataContainer,
                                      final Long userId) throws Exception
    {
        final CRMDashboardDetails existingCrmDashboardDetails = crmDashboardManager.get( orgId, existingDashboardId );
        final DashboardLayout existingDashboardlayout = analyticsServiceAPI.getDashboardLayout()
                .get( orgId, existingCrmDashboardDetails.getReportsDashboardId() );
        final Collection<DashboardParentView> dashboardParentViews = existingDashboardlayout.getParentView();

        crmReportsAPI.getDashboardVisibility().createVisibility(orgId, userId, crmDashboardDetails.id(), dataContainer);

        if( !dashboardParentViews.isEmpty() )
        {
            final Collection<CRMChildViewDetails> crmChildViewDetails = existingCrmDashboardDetails.getCrmChildViews();
            final Map<Long, CRMChildViewDetails> crmChildViewDetailsMap = crmChildViewDetails.stream()
                    .collect( Collectors.toMap( CRMChildViewDetails::id, Function.identity() ) ) ;
            final Criteria criteria = Criteria.eq( dashboardLayoutPositionManager.getTable().getColumn(DASHBOARDLAYOUTPOSITION.DASHBOARD_ID), existingCrmDashboardDetails.getReportsDashboardId());
            final Collection<DashboardLayoutPosition> dashboardLayoutPositions = dashboardLayoutPositionManager.getAll(orgId, criteria);
            final Map<Long, DashboardLayoutPosition> dashboardLayoutPositionMap = dashboardLayoutPositions.stream()
                    .collect(Collectors.toMap(DashboardLayoutPosition::getPosition, Function.identity()));
            final Set<Map.Entry<Long, CRMChildViewDetails>> entries = crmChildViewDetailsMap.entrySet();

            for ( Map.Entry<Long, CRMChildViewDetails> entry : entries)
            {
                final CRMChildViewDetails crmViewDetails = entry.getValue();
                final Long position = crmViewDetails.getCrmViewPosition();
                final DashboardLayoutPosition dashboardLayoutPosition = DashboardLayoutPosition.builder()
                        .layoutAsString(dashboardLayoutPositionMap.get(position).getLayoutAsString())
                        .dashboardId(crmDashboardDetails.getReportsDashboardId())
                        .position(position)
                        .build();

                final long currentTime = Instant.now().toEpochMilli();
                final CRMChildViewDetails newCrmViewDetails = CRMChildViewDetails.builder()
                                                                                 .crmViewPosition(position)
                                                                                 .crmDashboardId( crmDashboardDetails.id() )
                                                                                 .viewTypeId( crmViewDetails.getViewTypeId() )
                                                                                 .createdBy( crmViewDetails.getCreatedBy() )
                                                                                 .createdTime( currentTime )
                                                                                 .description( crmViewDetails.getDescription() )
                                                                                 .modifiedBy( crmViewDetails.getModifiedBy() )
                                                                                 .modifiedTime( currentTime )
                                                                                 .name( crmViewDetails.getName() )
                                                                                 .orgId( orgId )
                                                                                 .dashboardLayoutPosition(dashboardLayoutPosition)
                                                                                 .build();

                try
                {
                    crmViewManagerAPI.getCRMChildViewManager()
                                     .loadDataIntoContainer( List.of( newCrmViewDetails ), dataContainer );
                    crmViewManagerAPI.getDashboardLayoutManager().loadDataIntoContainer(List.of(dashboardLayoutPosition), dataContainer);
                }
                catch ( Exception error )
                {
                    LOGGER.error( "CRM Analytics: Failed to load dataContainer for orgId: {}. Reason: {}", orgId, error );
                    throw ErrorCode.get( AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed" );
                }

                final Long anaDashboardLayoutId = existingCrmDashboardDetails.getReportsDashboardId();
                final Table parentViewTable = dashboardParentViewManager.getTable();
                final Criteria dashIdCriteria = Criteria.eq( parentViewTable.getColumn( ANADASHBOARDPARENTVIEW.DASH_ID ), anaDashboardLayoutId );
                final Criteria rowIdCriteria = Criteria.eq( parentViewTable.getColumn( ANADASHBOARDPARENTVIEW.ROW_INDEX ), crmViewDetails.getCrmViewPosition() );
                final WhereClause whereClause = new WhereClause( dashIdCriteria ).and( rowIdCriteria );
                final Collection<DashboardParentView> oldDashboardParentViews = dashboardParentViewManager.getAll( orgId, whereClause );

                for( final DashboardParentView dashboardParentView : oldDashboardParentViews )
                {
                    final Long oldViewId = dashboardParentView.getChildViewId();
                    final AnalyticsReport analyticsReport = getAnalyticsReport( orgId, userId, oldViewId,
                                                                                dashboardParentView.getType() ,dataContainer );
                    final Long viewDetailsId = analyticsReport.getViewDetails().id();

                    final DashboardParentView newDashboardParentView = DashboardParentView.builder()
                                                                                          .title( dashboardParentView.getTitle())
                                                                                          .colIdx( dashboardParentView.getColIdx())
                                                                                          .rowIdx( dashboardParentView.getRowIdx() )
                                                                                          .height( dashboardParentView.getHeight() )
                                                                                          .orgId( orgId )
                                                                                          .childViewId( viewDetailsId )
                                                                                          .reportType(dashboardParentView.getViewType().getTypeCode())
                                                                                          .dashId( reportDashboardId )
                                                                                          .width( dashboardParentView.getWidth() )
                                                                                          .build();
                    analyticsServiceAPI.getDashboardParentView()
                                       .loadDataIntoContainer( List.of( newDashboardParentView ) , dataContainer );

                    createCrmViewDetails( orgId,
                                          oldViewId,
                                          analyticsReport.id(),
                                          viewDetailsId,
                                          newCrmViewDetails.id(),
                                          dataContainer );
                }
            }
        }
    }

    /**
     * <p>
     * Creates the new {@link CRMDashboardDetails} based on the existing dashboard
     * </p>
     *
     * @param orgId               Represents an organisation id
     * @param oldCrmChildViewId        Represents old child view id
     * @param newANAViewId        Represents the new ana view id
     * @param viewDetailsId       Represents the view details id
     * @param crmChildViewId      Represents the crm Child View id
     * @param dataContainer    Holds the rows that will get committed
     */
    private void createCrmViewDetails( final Long orgId,
                                       final Long oldCrmChildViewId,
                                       final Long newANAViewId,
                                       final Long viewDetailsId,
                                       final Long crmChildViewId,
                                       final DataContainer dataContainer )
    {
        try
        {
            crmViewManagerAPI.getCRMANAViewManager()
                             .loadDataIntoContainer( List.of( CRMANAViewMapper.builder()
                                                                              .anaViewId( newANAViewId )
                                                                              .anaViewDetailsId( viewDetailsId )
                                                                              .crmViewId( crmChildViewId )
                                                                              .orgId( orgId )
                                                                              .build() ), dataContainer );
        }
        catch ( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to load dataContainer for orgId: {}. Reason: {}", orgId, error );
            throw ErrorCode.get( AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed" );
        }

        createReportViewFieldMapper( orgId, oldCrmChildViewId, viewDetailsId, crmChildViewId, dataContainer );
    }

    /**
     * <p>
     * Creates the copy of {@link CRMRViewFieldMapper} entity
     * </p>
     *
     * @param orgId        Represents the organization id
     * @param oldCRMViewId Represents the analytics report old view id
     * @param newANAViewId Represents the analytics report new view id
     * @param newCRMViewId Represents the crm report new view id
     * @param dataContainer    Holds the rows that will get committed
     */
    private void createReportViewFieldMapper( final Long orgId,
                                              final Long oldCRMViewId,
                                              final Long newANAViewId,
                                              final Long newCRMViewId,
                                              final DataContainer dataContainer )
    {
        final CRMViewConfigManager<CRMRViewFieldMapper> viewFieldMapper = crmReportManagerAPI.getViewFieldMapper();

        try
        {
            final Collection<CRMRViewFieldMapper> oldCrmViewFieldMappers = viewFieldMapper.getAll( orgId,
                                                                                                   Criteria.eq( viewFieldMapper.getTable()
                                                                                                                               .getColumn( CRMREPORTVIEWFIELDMAPPER.VIEW_ID ),
                                                                                                                oldCRMViewId ) );

            for( final CRMRViewFieldMapper crmViewFieldMapper : oldCrmViewFieldMappers )
            {
                final Collection<CRMRMapperColumn> mapperColumns = crmViewFieldMapper.getMapperColumns();
                final CRMRViewFieldMapper newViewFieldMapper = CRMRViewFieldMapper.builder()
                                                                                  .modularFieldId( crmViewFieldMapper.getModularFieldId() )
                                                                                  .viewId( newANAViewId )
                                                                                  .crmViewId( newCRMViewId )
                                                                                  .anaActualColumnId( crmViewFieldMapper.getAnaActualColumnId() )
                                                                                  .orgId( orgId )
                                                                                  .anaActualComparatorId( crmViewFieldMapper.getAnaActualComparatorId() )
                                                                                  .anaActualOperationId( crmViewFieldMapper.getAnaActualOperationId() )
                                                                                  .anaYColumnID( crmViewFieldMapper.getAnaYColumnID() )
                                                                                  .isBuckedApplied( crmViewFieldMapper.isBuckedApplied() )
                                                                                  .isFilterColumn( crmViewFieldMapper.isFilterColumn() )
                                                                                  .isUnitApplied( crmViewFieldMapper.isUnitApplied() )
                                                                                  .isPeriodFilterApplied( crmViewFieldMapper.isPeriodFilterApplied() )
                                                                                  .mapperColumns( Objects.isNull( mapperColumns ) ? null
                                                                                                                                  : getMapperColumns( orgId, mapperColumns ) )
                                                                                  .isPeriodicFilterField(crmViewFieldMapper.isPeriodicFilterField())
                                                                                  .filterOrder( crmViewFieldMapper.getFilterOrder() )
                                                                                  .order( crmViewFieldMapper.getOrder() )
                                                                                  .axis(crmViewFieldMapper.getAxis())
                                                                                  .build();

                viewFieldMapper.loadDataIntoContainer( List.of( newViewFieldMapper ), dataContainer );
                final Long newViewFieldMapperId = newViewFieldMapper.id();
                final Long oldViewFieldMapperId = crmViewFieldMapper.id();

                createFieldPeriodicFilter( orgId, oldViewFieldMapperId, newViewFieldMapperId, dataContainer );
                createFieldBucketUnits( orgId, oldViewFieldMapperId, newViewFieldMapperId, dataContainer );
            }
        }
        catch( Exception exception )
        {
            LOGGER.error( "CRM Analytics: Failed to reset report view field mapper for orgId: {}. Reason: {}",
                          orgId, exception );
        }
    }

    /**
     * <p>
     * Gets the copy of {@link CRMRMapperColumn} entity
     * </p>
     *
     * @param orgId         Represents the organization ID
     * @param mapperColumns It holds the {@link CRMRMapperColumn} details
     * @return A collection of {@link CRMRMapperColumn} entities.
     */
    private Collection<CRMRMapperColumn> getMapperColumns( final Long orgId, final Collection<CRMRMapperColumn> mapperColumns )
    {
        final Collection<CRMRMapperColumn> newMapperColumns = new HashSet<>();

        for( final CRMRMapperColumn crmrMapperColumn : mapperColumns )
        {
            newMapperColumns.add( CRMRMapperColumn.builder()
                                                  .orgId( orgId )
                                                  .anaMapperColumn( crmrMapperColumn.getAnaMapperColumn() )
                                                  .isFilterColumn( crmrMapperColumn.isFilterColumn() )
                                                  .build() );
        }

        return newMapperColumns;
    }

    /**
     * <p>
     * Create the {@link FieldBucketUnit} entity
     * </p>
     *
     * @param orgId                Represents the organization ID
     * @param oldViewFieldMapperId Represents the old view field mapper ID
     * @param newViewFieldMapperId Represents the new view field mapper ID
     * @param dataContainer    Holds the rows that will get committed
     */
    private void createFieldBucketUnits( final Long orgId, final Long oldViewFieldMapperId, final Long newViewFieldMapperId,
                                         final DataContainer dataContainer ) throws Exception
    {
        final CRMViewConfigManager<FieldBucketUnit> fieldBucketUnitManager = crmReportManagerAPI.getFieldBucketUnitManager();
        final Collection<FieldBucketUnit> fieldBucketUnits = fieldBucketUnitManager.getAll( orgId, Criteria.eq(
                                                                                            fieldBucketUnitManager.getTable()
                                                                                                                  .getColumn( CRMFIELDBUCKETUNIT.CRM_VIEW_FIELD_ID ),
                                                                                            oldViewFieldMapperId ) );
        final Collection<FieldBucketUnit> newFieldBucketUnits = new HashSet<>();

        for ( final FieldBucketUnit fieldBucketUnit : fieldBucketUnits )
        {
            newFieldBucketUnits.add( FieldBucketUnit.builder()
                                                    .orgId( orgId )
                                                    .unitId( fieldBucketUnit.getUnitId() )
                                                    .viewFieldId( newViewFieldMapperId )
                                                    .build() );
        }

        fieldBucketUnitManager.loadDataIntoContainer( newFieldBucketUnits, dataContainer );
    }

    /**
     * <p>
     * Create the {@link FieldPeriodicFilter} entity
     * </p>
     *
     * @param orgId                The {@link AnalyticsReport} object whose fields need to be reset
     * @param oldViewFieldMapperId Represents the old view field mapper ID
     * @param newViewFieldMapperId Represents the new view field mapper ID
     * @param dataContainer    Holds the rows that will get committed
     */
    private void createFieldPeriodicFilter( final Long orgId, final Long oldViewFieldMapperId,
                                            final Long newViewFieldMapperId,
                                            final DataContainer dataContainer ) throws Exception
    {
        final Collection<FieldPeriodicFilter> fieldPeriodicFilters = periodicFilterManager.getAll( orgId, Criteria.eq(
                                                                                                   periodicFilterManager.getTable()
                                                                                                                        .getColumn( CRMFIELDPERIODICFILTER.CRM_VIEW_FIELD_ID ),
                                                                                                   oldViewFieldMapperId ) );
        final Collection<FieldPeriodicFilter> newFieldPeriodicFilter = new HashSet<>();

        for( final FieldPeriodicFilter fieldPeriodicFilter : fieldPeriodicFilters )
        {
            newFieldPeriodicFilter.add( FieldPeriodicFilter.builder()
                                                           .viewFieldId( newViewFieldMapperId )
                                                           .orgId( orgId )
                                                           .functionId( fieldPeriodicFilter.getFunctionId() )
                                                           .build() );
        }

        periodicFilterManager.loadDataIntoContainer( newFieldPeriodicFilter, dataContainer );
    }

    /**
     * <p>
     * Gets the {@link AnalyticsReport} object based on the provided organization ID and request data
     * </p>
     *
     * @param orgId       Represents the organization ID
     * @param viewDetailsId Represents the child view ID
     * @param reportType  It holds the {@link ReportType} details
     * @param dataContainer    Holds the rows that will get committed
     * @return A {@link AnalyticsReport} object representing the customized dashboard copy configuration
     */
    private AnalyticsReport getAnalyticsReport( final Long orgId, final Long userId, final Long viewDetailsId, final ReportType reportType,
                                                final DataContainer dataContainer )
    {
        if( ReportType.GRAPH == reportType )
        {
            final AnalyticsConfigManager<ChartView> chartLayout = analyticsServiceAPI.getChartLayout();
            final ChartView chartView = chartLayout.getAll( orgId, Criteria.eq( chartLayout.getTable()
                                                                                           .getColumn( ANAREPORT.VIEW_ID ),
                                                                                viewDetailsId ) )
                                                   .stream()
                                                   .findFirst()
                                                   .orElseThrow( () -> ErrorCode.get( AnalyticsErrorCodes.ANALYTICS_REPORT_NOT_FOUND ) );
            final ViewDetails viewDetails = chartView.getViewDetails();
            final Collection<FilterField> filterField = chartView.getFilterFields();
            final ViewDetails newViewDetails = getViewDetails( orgId, userId, viewDetails.getDisplayName(), viewDetails.getDescription(), viewDetails );
            final ChartView newChartView = ChartView.builder()
                                                    .graphType(chartView.getGraphType().getTypeCode())
                                                    .orgId( orgId )
                                                    .title( chartView.getTitle() )
                                                    .rootTableId(chartView.getRootTableId())
                                                    .filterFields( Objects.isNull( filterField ) ? null
                                                            : getFilterFields( orgId, filterField ) )
                                                    .reportFields( getReportFields( orgId, chartView.getReportFields() ) )
                                                    .viewDetails( newViewDetails )
                                                    .reportPathMapping( getReportPathMapping( orgId, chartView.getReportPathMapping() ) )
                                                    .build() ;

            try
            {
                chartLayout.loadDataIntoContainer( List.of( newChartView ), dataContainer );
            }
            catch ( Exception error )
            {
                LOGGER.error( "CRM Analytics: Failed to load dataContainer for orgId:{}. Reason: {}", orgId, error );
                throw ErrorCode.get( AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed" );
            }

            return newChartView;
        }
        else
        {
            final AnalyticsConfigManager<KPIView> kpiLayout = analyticsServiceAPI.getKPILayout();
            final KPIView kpiView = kpiLayout.getAll( orgId, Criteria.eq( kpiLayout.getTable()
                                                                                .getColumn(ANAREPORT.VIEW_ID),
                                                                          viewDetailsId ) )
                                             .stream()
                                             .findFirst()
                                             .orElseThrow( () -> ErrorCode.get( AnalyticsErrorCodes.ANALYTICS_REPORT_NOT_FOUND ) );
            final ViewDetails viewDetails = kpiView.getViewDetails();
            final ViewDetails newViewDetails = getViewDetails( orgId, userId, viewDetails.getDisplayName(), viewDetails.getDescription(), viewDetails );
            final KPIView newKpiView = KPIView.builder()
                                              .graphType(kpiView.getGraphType().getTypeCode())
                                              .orgId( orgId )
                                              .title( kpiView.getTitle() )
                                              .rootTableId( kpiView.getRootTableId() )
                                              .filterFields( getFilterFields( orgId, kpiView.getFilterFields() ) )
                                              .reportFields( getReportFields( orgId, kpiView.getReportFields() ) )
                                              .viewDetails( newViewDetails )
                                              .reportPathMapping( getReportPathMapping( orgId, kpiView.getReportPathMapping() ) )
                                              .comparators( getComparator( orgId, kpiView.getComparators() ) )
                                              .build() ;

            try {
                kpiLayout.loadDataIntoContainer( List.of( newKpiView ), dataContainer );
            }
            catch ( Exception error )
            {
                LOGGER.error( "CRM Analytics: Failed to load dataContainer for orgId: {}. Reason: {}", orgId, error );
                throw ErrorCode.get( AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed" );
            }

            return newKpiView;
        }
    }

    /**
     * <p>
     * Gets the copy of {@link ReportPathMapping} entity
     * </p>
     *
     * @param orgId             Represents the organization ID
     * @param reportPathMapping It holds the {@link ReportPathMapping} details
     * @return A collection of {@link ReportPathMapping} entities.
     */
    private ReportPathMapping getReportPathMapping( final Long orgId, final ReportPathMapping reportPathMapping )
    {
        return ReportPathMapping.builder()
                                .pathInfo( getReportPathInfo( orgId, reportPathMapping.getPathInfo() ) )
                                .orgId( orgId )
                                .build();
    }

    /**
     * <p>
     * Gets the copy of {@link PathInfo} entity
     * </p>
     *
     * @param orgId    Represents the organization ID
     * @param pathInfo It holds the {@link PathInfo} details
     * @return the {@link PathInfo} entity.
     */
    private PathInfo getReportPathInfo( final Long orgId, final PathInfo pathInfo )
    {
        final Collection<RelationsInPath> relationsInPaths = pathInfo.getRelationsInPaths();

        return PathInfo.builder()
                       .orgId( orgId )
                       .relationsInPaths( Objects.isNull( relationsInPaths ) ? null : getRelationsInPath( orgId, relationsInPaths ) )
                       .build();
    }

    /**
     * <p>
     * Gets the copy of {@link RelationsInPath} entity
     * </p>
     *
     * @param orgId            Represents the organization ID
     * @param relationsInPaths It holds the collection of {@link RelationsInPath} details
     * @return A collection of {@link RelationsInPath} entities.
     */
    private Collection<RelationsInPath> getRelationsInPath( final Long orgId, final Collection<RelationsInPath> relationsInPaths )
    {
        final Collection<RelationsInPath> newRelationsInPath = new HashSet<>();

        for ( final RelationsInPath relationsInPath : relationsInPaths )
        {
            newRelationsInPath.add( RelationsInPath.builder()
                                                   .orgId( orgId )
                                                   .relationId( relationsInPath.getRelationId() )
                                                   .isReverseJoin( relationsInPath.isReverseJoin() )
                                                   .relationIndex( relationsInPath.getRelationIndex() )
                                                   .build() );
        }

        return newRelationsInPath;
    }

    /**
     * <p>
     * Gets the copy of {@link ViewDetails} entity
     * </p>
     *
     * @param orgId       Represents the organization ID
     * @param description Represents the description of th view
     * @param displayName Represents the displayName of the view
     * @param viewDetails It holds the {@link ViewDetails} details
     * @return the copied {@link ViewDetails} entity.
     */
    private ViewDetails getViewDetails( final Long orgId, final Long userId, final String displayName, final String description,
                                        final ViewDetails viewDetails )
    {
        return ViewDetails.builder()
                          .orgId( orgId )
                          .createdBy(userId)
                          .modifiedBy(userId)
                          .createdTime(viewDetails.getCreatedTime() )
                          .modifiedTime(viewDetails.getModifiedTime() )
                          .isDeleted( viewDetails.getIsDeleted() )
                          .isHidden( viewDetails.getIsHidden() )
                          .isCustomView( viewDetails.getIsCustomView() )
                          .viewTypeId(viewDetails.getViewType().getTypeCode())
                          .displayName( displayName )
                          .description( description )
                          .moveToTrash( viewDetails.getMoveToTrash() )
                          .build();
    }

    /**
     * <p>
     * Gets the copy of {@link ReportField} entity
     * </p>
     *
     * @param orgId           Represents the organization ID
     * @param oldReportFields It holds the collection of {@link ReportField} details
     * @return A collection of {@link ReportField} entities.
     */
    private Collection<ReportField> getReportFields( final Long orgId, final Collection<ReportField> oldReportFields )
    {
        final Collection<ReportField> newReportFields = new HashSet<>();

        for ( final ReportField reportField : oldReportFields )
        {
            final ReportFieldCustomRange reportFieldCustomRange = reportField.getReportFieldCustomRange();

            newReportFields.add( ReportField.builder()
                                            .orgId( orgId )
                                            .columnId( reportField.getColumnId() )
                                            .createdTime( reportField.getCreatedTime() )
                                            .modifiedTime( reportField.getModifiedTime() )
                                            .order( reportField.getOrder() )
                                            .axisType(reportField.getAxis().getType())
                                            .fieldType(reportField.getFieldType().getTypeCode())
                                            .rangeSize( reportField.getRangeSize() )
                                            .label( reportField.getLabel() )
                                            .operationId( reportField.getOperationType().getTypeCode() )
                                            .reportFieldCustomRange( Objects.isNull( reportFieldCustomRange ) ? null
                                                                     :getFieldCustomRange( orgId, reportFieldCustomRange ) )
                                            .fieldId( reportField.getFieldId() )
                                            .moduleId( reportField.getModuleId() )
                                            .subModuleId( reportField.getSubModuleId() )
                                            .functionName(reportField.getFunctionName())
                                            .timePeriodId(reportField.getTimePeriodId())
                                            .storeTypeId(reportField.getStoreTypeId())
                                            .build() );
        }

        return newReportFields;
    }

    /**
     * <p>
     * Gets the copy of {@link ReportFieldCustomRange} entity
     * </p>
     *
     * @param orgId                  Represents the organization ID
     * @param reportFieldCustomRange It holds the {@link ReportFieldCustomRange} details
     * @return A collection of {@link ReportFieldCustomRange} entities.
     */
    private ReportFieldCustomRange getFieldCustomRange( final Long orgId, final ReportFieldCustomRange reportFieldCustomRange )
    {
        return ReportFieldCustomRange.builder()
                                     .to( reportFieldCustomRange.getTo() )
                                     .from( reportFieldCustomRange.getFrom() )
                                     .orgId( orgId )
                                     .build();
    }

    /**
     * <p>
     * Gets the copy of {@link FilterField} entity
     * </p>
     *
     * @param orgId           Represents the organization ID
     * @param oldFilterFields It holds the collection of {@link FilterField} details
     * @return A collection of {@link FilterField} entities.
     */
    private Collection<FilterField> getFilterFields( final Long orgId, final Collection<FilterField> oldFilterFields )
    {
        final Collection<FilterField> newFilterFields = new HashSet<>();

        for ( final FilterField filterField : oldFilterFields)
        {
            final FilterFunctionMapper filterFunctionMapper = filterField.getFilterFunctionMapper();

            newFilterFields.add( FilterField.builder()
                                            .orgId( orgId)
                                            .to( filterField.getTo() )
                                            .viewColumnId( filterField.getViewColumnId() )
                                            .filterType(filterField.getFilterType().getTypeCode())
                                            .format( filterField.getFormat() )
                                            .from( filterField.getFrom() )
                                            .operatorType(filterField.getOperatorType().getTypeCode())
                                            .value( filterField.getValue() )
                                            .columnId( filterField.getColumnId() )
                                            .createdTime( filterField.getCreatedTime() )
                                            .modifiedTime( filterField.getModifiedTime() )
                                            .operationId(filterField.getOperationType().getTypeCode())
                                            .order( filterField.getOrder() )
                                            .filterFunctionMapper( Objects.isNull( filterFunctionMapper ) ? null
                                                                                                          : getFilterFunctionMapper( orgId, filterFunctionMapper) )
                                            .fieldId( filterField.getFieldId() )
                                            .actualColumnId( filterField.getActualColumnId() )
                                            .isCustomFilter( filterField.isCustomFilter() )
                                            .storeTypeId(filterField.getStoreTypeId())
                                            .build() );
        }

        return newFilterFields;
    }

    /**
     * <p>
     * Gets the new {@link FilterFunctionMapper} details
     * </p>
     *
     * @param orgId                Represents an organisation id
     * @param filterFunctionMapper Represents the existing {@link FilterFunctionMapper}
     * @return The new filter function mapper
     */
    private FilterFunctionMapper getFilterFunctionMapper( final Long orgId, final FilterFunctionMapper filterFunctionMapper)
    {
        return FilterFunctionMapper.builder()
                                   .orgId( orgId )
                                   .functionName( filterFunctionMapper.getFunctionName() )
                                   .build();
    }

    /**
     * <p>
     * Gets the copied {@link KPIComparator} entity
     * </p>
     *
     * @param orgId       Represents the organization ID
     * @param comparators The collection of {@link KPIComparator} object whose fields need to be reset
     */
    private Collection<KPIComparator> getComparator( final Long orgId, final Collection<KPIComparator> comparators )
    {
        final Collection<KPIComparator> kpiComparators = new HashSet<>();

        for ( final KPIComparator kpiComparator : comparators )
        {
            final KPIComparatorSide comparatorSide = kpiComparator.getComparatorSide();

            kpiComparators.add( KPIComparator.builder()
                                             .orgId( orgId )
                                             .comparatorSide( comparatorSide.getTypeCode() )
                                             .comparatorType(Math.toIntExact(KPIComparatorSide.RIGHT == comparatorSide ? kpiComparator.getComparatorType().getTypeCode()
                                                     : kpiComparator.getOperationType().getTypeCode()))
                                             .from( kpiComparator.getFrom() )
                                             .to( kpiComparator.getTo() )
                                             .createdTime( kpiComparator.getCreatedTime() )
                                             .modifiedTime( kpiComparator.getModifiedTime() )
                                             .build() );
        }

        return kpiComparators;
    }
}

