package com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.model.view.ViewType;
import com.tlc.analytics.config.model.view.layout.DashboardParentView;
import com.tlc.analytics.config.model.view.report.AnalyticsReport;
import com.tlc.analytics.config.model.view.report.ChartView;
import com.tlc.analytics.config.model.view.report.KPIView;

import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.CRMDashboardViewRemover;
import com.tlc.crm.core.api.analytics.converter.layout.dashboardlayout.view.properties.CRMViewPropertyConverter;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;
import com.tlc.crm.core.api.analytics.general.keys.common.CommonKeys;
import com.tlc.crm.core.api.analytics.general.keys.layout.CRMChildViewKeys;
import com.tlc.crm.core.api.analytics.integ.general.analytics.AnalyticsServiceAPI;
import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutPosition;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.DashboardLayoutConfig;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMANAViewMapper;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.properties.wappers.ANAFieldsWrapper;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.i18n.I18nResolver;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.json.Json;

import com.tlc.sql.common.api.DataContainer;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * <p>
 * Provides the abstract way to converts the CRM analytics view configurations to ANA view configurations
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
abstract class CRMViewConverter<T extends AnalyticsReport> extends CRMANAViewConverter<T>
{
    private static final Logger LOGGER = LoggerFactory.getLogger( CRMViewConverter.class );

    private final CRMViewPropertyConverter crmViewPropertyConverter;
    private final AnalyticsServiceAPI analyticsServiceAPI;
    private final CRMViewManagerAPI crmViewManagerAPI;

    protected CRMViewConverter()
    {
        crmViewPropertyConverter = CRMViewPropertyConverter.INSTANCE();
        analyticsServiceAPI = AnalyticsServiceAPI.INSTANCE();
        crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
    }

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest  Represent the {@link AnalyticsRequest} details
     * @param childViewRequest  Holds the {@link JsonObject} details
     * @param i18nResolver Represents the {@link I18nResolver}
     * @param dataContainer    Holds the rows that will get committed
     * @return The created {@link JsonObject}
     */
    @Override
    public @NonNull JsonObject createModel( final @NonNull AnalyticsRequest analyticsRequest,
                                            final @NonNull Long crmDashboardId,
                                            final @NonNull Long anaDashboardId,
                                            final @NonNull JsonObject childViewRequest,
                                            final @NonNull I18nResolver i18nResolver,
                                            final @NonNull DataContainer dataContainer,
                                            final DashboardLayoutConfig dashboardLayoutConfig,
                                            final Map<Long, ReportModularField> reportFieldMap )
    {
        final Long orgId = analyticsRequest.orgId();
        final boolean isLayoutConfigPresent = Objects.nonNull(dashboardLayoutConfig);
        try
        {
            final Long accessTime = System.currentTimeMillis();
            final Long createdUserId = analyticsRequest.userId();
            final Long position = childViewRequest.getLong(CRMChildViewKeys.VIEW_POSITION);

            final DashboardLayoutPosition dashboardLayoutPosition = isLayoutConfigPresent ? DashboardLayoutPosition.builder()
                                                                                           .dashboardId(anaDashboardId)
                                                                                           .layoutAsString(dashboardLayoutConfig.getJson().toString())
                                                                                           .dashboardLayoutConfig(dashboardLayoutConfig)
                                                                                           .position(position)
                                                                                           .build()
                                                                                          : null;

            final Map<T, ANAFieldsWrapper> modelConfigMap = convert( analyticsRequest, createdUserId, accessTime,
                                                                     accessTime, childViewRequest, i18nResolver, reportFieldMap );

            final int crmViewTypeId = childViewRequest.getInt(CRMReportKeys.VIEW_TYPE_ID);
            final CRMViewType crmViewType = CRMViewType.getCrmViewType( crmViewTypeId );

            final String childViewName = childViewRequest.getString(CRMChildViewKeys.VIEW_NAME);
            final String childViewDescription = childViewRequest.optString( CommonKeys.DESCRIPTION, "" );
            CRMChildViewDetails crmChildView;

            if (isLayoutConfigPresent) {
                crmChildView = CRMChildViewDetails.builder()
                                                  .crmDashboardId(crmDashboardId)
                                                  .crmViewPosition(childViewRequest.getLong(CRMChildViewKeys.VIEW_POSITION))
                                                  .viewTypeId(crmViewTypeId)
                                                  .name(childViewName)
                                                  .description(childViewDescription)
                                                  .createdTime(accessTime)
                                                  .modifiedTime(accessTime)
                                                  .createdBy(createdUserId)
                                                  .modifiedBy(createdUserId)
                                                  .orgId(orgId)
                                                  .dashboardLayoutPosition(dashboardLayoutPosition)
                                                  .build();

                crmViewManagerAPI.getCRMChildViewManager().loadDataIntoContainer(List.of(crmChildView), dataContainer);

                try {
                    crmViewManagerAPI.getDashboardLayoutManager().loadDataIntoContainer(List.of(dashboardLayoutPosition), dataContainer);
                    LOGGER.info("CRM Analytics: Successfully loaded values into the dataContainer for orgId: {}", orgId);
                } catch (Exception error) {
                    LOGGER.error("CRM Analytics: Failed to load dataContainer for orgId: {}", orgId, error);
                    throw ErrorCode.get(AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed");
                }
            }
            else {
                crmChildView = CRMChildViewDetails.builder()
                                                  .crmDashboardId(crmDashboardId)
                                                  .crmViewPosition(childViewRequest.getLong(CRMChildViewKeys.VIEW_POSITION))
                                                  .viewTypeId(crmViewTypeId)
                                                  .name(childViewName)
                                                  .description(childViewDescription)
                                                  .createdTime(accessTime)
                                                  .modifiedTime(accessTime)
                                                  .createdBy(createdUserId)
                                                  .modifiedBy(createdUserId)
                                                  .orgId(orgId)
                                                  .build();

                crmViewManagerAPI.getCRMChildViewManager().loadDataIntoContainer(List.of(crmChildView), dataContainer);
            }

            final Long crmChildViewId = crmChildView.id();

            long columnIndex = 0L;

            final Collection<T> models = new ArrayList<>();

            for( final Map.Entry<T,ANAFieldsWrapper> entry: modelConfigMap.entrySet() )
            {
                final T model = entry.getKey();
                final ANAFieldsWrapper fieldsWrapper = entry.getValue();

                final T savedModel = saveView( crmViewType, model, dataContainer );
                final Long viewDetailsId = savedModel.getViewDetails().id();
                final int viewTypeId = CRMViewType.KPI == crmViewType ? ViewType.WIDGET.getTypeCode()
                                                                       : ViewType.CHART.getTypeCode();

                createParentView( viewDetailsId,
                                  childViewRequest.getLong( CRMChildViewKeys.VIEW_POSITION ),
                                  columnIndex,
                                  childViewRequest.getString( CRMChildViewKeys.VIEW_NAME ),
                                  viewTypeId,
                                  anaDashboardId,
                                  dataContainer );

                final CRMANAViewMapper viewMapper = CRMANAViewMapper.builder()
                                                                    .crmViewId( crmChildViewId )
                                                                    .anaViewId( savedModel.id() )
                                                                    .anaViewDetailsId( viewDetailsId )
                                                                    .orgId( orgId )
                                                                    .build();
                models.add( savedModel );

                crmViewManagerAPI.getCRMANAViewManager()
                                 .loadDataIntoContainer( List.of( viewMapper ), dataContainer );

                crmViewPropertyConverter.persistFields( orgId,
                                                        viewDetailsId, crmChildViewId, savedModel.getReportFields(),
                                                        savedModel.getFilterFields(), fieldsWrapper,
                                                        crmViewType, dataContainer );
                columnIndex +=1;
            }

            return getConfigJsonResponse(false, Collections.emptyList(), new HashSet<>(), models, crmChildView, analyticsRequest, i18nResolver);
        }
        catch( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to create view for orgId: {}", orgId, error );
            throw ErrorCode.get( AnalyticsErrorCodes.VIEW_CREATE_ACTION_FAILED,
                    "i18n.analytics.view.create.action.failed" );
        }
    }

    /**
     * <p>
     * Checks and updates the model based on the provided analytics request and child view request.
     * </p>
     *
     * @param analyticsRequest Represent the {@link AnalyticsRequest} details
     * @param childViewRequest Holds the {@link JsonObject} details
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @param dataContainer    Holds the rows that will get committed
     * @return The {@link JsonObject} details
     */
    @Override
    public @NonNull JsonObject checkAndUpdate( final @NonNull AnalyticsRequest analyticsRequest,
                                               final @NonNull Long crmDashboardId,
                                               final @NonNull Long anaDashboardId,
                                               final @NonNull JsonObject childViewRequest,
                                               final @NonNull I18nResolver i18nResolver,
                                               final @NonNull DataContainer dataContainer,
                                               final DashboardLayoutConfig dashboardLayoutConfig,
                                               final Map<Long, ReportModularField> reportFieldMap )
    {
        try
        {
            if( !childViewRequest.containsKey( CRMReportKeys.VIEW_ID ) )
            {
                throw ErrorCode.get( AnalyticsErrorCodes.VIEW_ID_NOT_FOUND,
                                     "i18n.analytics.view.not.found" );
            }
            deleteView( analyticsRequest, crmDashboardId, anaDashboardId, childViewRequest, i18nResolver, dataContainer );
            return createModel( analyticsRequest, crmDashboardId, anaDashboardId, childViewRequest, i18nResolver, dataContainer, dashboardLayoutConfig, reportFieldMap );
        }
        catch( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to updated view for orgId: {}", analyticsRequest.orgId(), error );
            throw ErrorCode.get( AnalyticsErrorCodes.VIEW_UPDATE_ACTION_FAILED,
                                 "i18n.analytics.view.update.action.failed" );
        }
    }

    /**
     * <p>
     * Gets the model configurations based on the provided analytics request and child view request
     * </p>
     *
     * @param analyticsRequest Represent the {@link AnalyticsRequest} details
     * @param childViewRequest Holds the {@link JsonObject} details
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @return Collection of models
     */
    public final @NonNull Collection<T> getModelConfig( final @NonNull AnalyticsRequest analyticsRequest,
                                                        final @NonNull JsonObject childViewRequest,
                                                        final @NonNull I18nResolver i18nResolver,
                                                        final Map<Long, ReportModularField> reportFieldMap )
    {
        try
        {
            final Long accessTime = System.currentTimeMillis();
            final Long createdUser = analyticsRequest.userId();

            Map<T, ANAFieldsWrapper> model = convert( analyticsRequest, createdUser, accessTime,
                                                      accessTime, childViewRequest, i18nResolver, reportFieldMap );

            return Collections.unmodifiableCollection( model.keySet() );
        }
        catch( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to fetch view for orgId: {}", analyticsRequest.orgId(), error );
            throw ErrorCode.get( AnalyticsErrorCodes.VIEW_GET_ACTION_FAILED,
                                 "i18n.analytics.view.get.action.failed" );
        }
    }

    /**
     * <p>
     * Gets the configuration json response based on the child view
     * </p>
     *
     * @param fetchProperties  if {@code true}, includes field and view-specific properties in the response
     * @param userIds          the collection of user IDs relevant to the configuration
     * @param fieldIds         the collection used to collect all field IDs involved in this configuration
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param childViewRequest Holds the child view {@link JsonObject}
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @return The {@link JsonObject}
     */
    @Override
    public @NonNull JsonObject getConfigJsonResponse(final boolean fetchProperties,
                                                     final @NonNull Collection<Long> userIds,
                                                     final @NonNull Collection<Long> fieldIds,
                                                     final @NonNull AnalyticsRequest analyticsRequest,
                                                     final @NonNull JsonObject childViewRequest,
                                                     final @NonNull I18nResolver i18nResolver) {
        return getViewConfigResponse(fetchProperties, userIds, fieldIds, analyticsRequest, childViewRequest, i18nResolver);
    }

    /**
     * <p>
     * Gets the configuration json response based on the child view
     * </p>
     *
     * @param fetchProperties     if {@code true}, includes field and view-specific properties in the response
     * @param userIds             A collection of userIds
     * @param fieldIds            the collection used to collect all field IDs involved in this configuration
     * @param models              the collection of models representing the analytics view mappings
     * @param crmChildViewDetails Details of the CRM child view configuration
     * @param analyticsRequest    Represents the {@link AnalyticsRequest} details
     * @param i18nResolver        An instance of {@link I18nResolver} for handling internationalization
     * @return The {@link JsonObject}
     */
    @Override
    public @NonNull JsonObject getConfigJsonResponse( final boolean fetchProperties,
                                                      final @NonNull Collection<Long> userIds,
                                                      final @NonNull Collection<Long> fieldIds,
                                                      final @NonNull Collection<T> models,
                                                      final @NonNull CRMChildViewDetails crmChildViewDetails,
                                                      final @NonNull AnalyticsRequest analyticsRequest,
                                                      final @NonNull I18nResolver i18nResolver)
    {
        return getViewConfigResponse(fetchProperties, userIds, fieldIds, models, crmChildViewDetails, analyticsRequest, i18nResolver);
    }

    /**
     * <p>
     * Deletes the view based on the child view json
     * </p>
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param childViewRequest Holds the child view json
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @param dataContainer    Holds the rows that will get committed
     * @return The {@link JsonObject}
     */
    @Override
    public @NonNull JsonObject deleteView( final @NonNull AnalyticsRequest analyticsRequest,
                                           final @NonNull Long crmDashboardId,
                                           final @NonNull Long anaDashboardId,
                                           final @NonNull JsonObject childViewRequest,
                                           final @NonNull I18nResolver i18nResolver,
                                           final DataContainer dataContainer )
    {
        final JsonObject response = Json.object();
        try
        {
            final Long orgId = analyticsRequest.orgId();
            final Long childViewId = childViewRequest.getLong( CRMReportKeys.VIEW_ID );
            final CRMChildViewDetails crmChildViewDetails = crmViewManagerAPI.getCRMChildViewManager()
                                                                             .get( orgId, childViewId );

            final Collection<CRMANAViewMapper> viewMappers = crmChildViewDetails.getCrmViewMapper();
            final Set<Long> anaViewIds = viewMappers.stream()
                                                    .map( CRMANAViewMapper::getAnaViewDetailsId )
                                                    .collect( Collectors.toSet() );

            CRMDashboardViewRemover.deleteUnMappedAnaViewDetails( orgId, anaViewIds, dataContainer, analyticsServiceAPI );

            CRMDashboardViewRemover.deleteUnMappedCrmDetails( orgId, List.of( crmChildViewDetails.id() ) , dataContainer, crmViewManagerAPI );

            response.put( "DELETE_STATUS", "SUCCESS" );
        }
        catch( Exception error )
        {
            response.put( "DELETE_STATUS", "ERROR" );
            LOGGER.error("ERROR in VIEW delete action", error );
        }
        return response;
    }

    /**
     * <p>
     * Saves the view based on the CRM view type and model
     * </p>
     *
     * @param viewType Represents the {@link CRMViewType} details
     * @param model    Represents the model
     * @param dataContainer    Holds the rows that will get committed
     * @return The saved view details
     */
    @NonNull
    @SuppressWarnings("unchecked")
    private T saveView( final @NonNull CRMViewType viewType, final @NonNull T model, final DataContainer dataContainer )
    {
        try {
            if( viewType.equals( CRMViewType.KPI ) )
            {
                analyticsServiceAPI.getKPILayout()
                                   .loadDataIntoContainer( List.of( ( KPIView ) model ), dataContainer );
                return model;
            }
            else
            {
                analyticsServiceAPI.getChartLayout()
                                   .loadDataIntoContainer( List.of( ( ChartView ) model ), dataContainer );
                return model;
            }
        }
        catch ( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to load dataContainer for orgId: {}", model.orgId(), error );
            throw ErrorCode.get( AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed" );
        }

    }

    private void createParentView( final Long childViewId,
                                   final Long rowIndex,
                                   final Long columnIndex,
                                   final String childViewName,
                                   final int viewTypeId,
                                   final Long dashboardId,
                                   final DataContainer dataContainer )
    {
        final DashboardParentView dashboardParentView = DashboardParentView.builder()
                                                                           .childViewId( childViewId )
                                                                           .width( 0L )
                                                                           .height( 0L )
                                                                           .rowIdx( rowIndex )
                                                                           .colIdx( columnIndex )
                                                                           .title( childViewName )
                                                                           .reportType( viewTypeId )
                                                                           .dashId( dashboardId )
                                                                           .build();
        try
        {
            analyticsServiceAPI.getDashboardParentView()
                               .loadDataIntoContainer( List.of( dashboardParentView ), dataContainer );
        }
        catch ( Exception error )
        {
            LOGGER.error( "CRM Analytics: Failed to load dataContainer for dashboardId: {}", dashboardId, error );
            throw ErrorCode.get( AnalyticsErrorCodes.CREATE_ACTION_FAILED, "i18n.analytics.create.action.failed" );
        }
    }
}
