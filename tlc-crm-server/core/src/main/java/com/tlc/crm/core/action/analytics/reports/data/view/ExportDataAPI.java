package com.tlc.crm.core.action.analytics.reports.data.view;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.listeners.Callback;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.ReportsAction;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.general.keys.common.CRMReportKeys;

import com.tlc.crm.core.api.analytics.integ.managers.CRMViewManagerAPI;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMChildViewDetails;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.i18n.I18nResolver;

import com.tlc.web.WebAction;
import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Manages the export data related web actions
 * </p>
 *
 * @author jamesh
 * @version 1.0
 */
@WebAction( path = "/crm/analytics/export/view-data" )
public class ExportDataAPI extends ReportsAction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportDataAPI.class);

    private final CRMViewManagerAPI crmViewManagerAPI;
    private final CRMReportsAPI crmReportsAPI;
    private final ReportModularFieldService reportModularFieldService;

    public ExportDataAPI()
    {
        crmViewManagerAPI = CRMViewManagerAPI.INSTANCE();
        crmReportsAPI = CRMReportsAPI.getInstance();
        reportModularFieldService = ReportModularFieldService.getInstance();
    }


    /**
     * {@inheritDoc}
     *
     * @param orgId      It represents the organization ID
     * @param userId     It represents the user ID
     * @param data       The JsonObject containing the request data, must not be null
     * @param actionType The type of action being performed, must not be null
     * @throws Exception
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType ) throws Exception
    {

    }

    /**
     * {@inheritDoc}
     *
     * <p>Fetches the report modular fields asynchronously and, on success, builds the view
     * response JSON and sends it. On failure, sends an error response immediately.</p>
     *
     * @param analyticsRequest the analytics request context, including org, user, and response handle
     * @param actionType       the type of action being performed, must not be null
     * @param requestData      the JSON payload containing request-specific data, must not be null
     * @param i18nResolver     an instance of {@link I18nResolver} for handling internationalization
     */
    @Override
    protected void processRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                  final @NonNull ActionType actionType,
                                  final @NonNull JsonObject requestData,
                                  final @NonNull I18nResolver i18nResolver) {
        final long orgId = analyticsRequest.orgId();

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {
            @Override
            public void onSuccess(final Map<Long, ReportModularField> reportFieldMap) {
                final JsonObject jsonObject = getJsonResponse(analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);
                final CrmResponse crmResponse = analyticsRequest.crmResponse();

                jsonObject.forEach(crmResponse::put);
                crmResponse.send();
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("CRM Analytics: Failed to fetch report modular fields for export-data, orgId: {}", orgId, cause);
                analyticsRequest.crmResponse().setErrorCode(ErrorCode.get(AnalyticsErrorCodes.MODULAR_FIELD_NOT_FOUND, "i18n.analytics.modular.fields.get.failed"));
                analyticsRequest.crmResponse().send();
            }
        });
    }

    /**
     * Resolves the view type, fetches the corresponding graph axis data, and builds
     * the response {@link JsonObject} for the export view request.
     *
     * @param analyticsRequest the analytics request context
     * @param actionType       the type of action being performed
     * @param requestData      the JSON payload containing the view ID and other request details
     * @param i18nResolver     an instance of {@link I18nResolver} for handling internationalization
     * @param reportFieldMap   map of report modular fields fetched for the organisation
     * @return the built response {@link JsonObject}
     */
    private JsonObject getJsonResponse(final @NonNull AnalyticsRequest analyticsRequest,
                                       final @NonNull ActionType actionType,
                                       final @NonNull JsonObject requestData,
                                       final @NonNull I18nResolver i18nResolver,
                                       final Map<Long, ReportModularField> reportFieldMap) {
        final Long orgId = analyticsRequest.orgId();
        final Long viewId = requestData.getLong( CRMReportKeys.VIEW_ID );

        final CRMChildViewDetails crmChildViewDetails = crmViewManagerAPI.getCRMChildViewManager()
                                                                         .get( orgId, viewId );

        final CRMViewType viewType = crmChildViewDetails.getViewType();

        final List<GraphAxis<Object>> graphAxisList = getGraphAxis( viewType,
                                                                    analyticsRequest,
                                                                    actionType,
                                                                    requestData,
                                                                    i18nResolver, reportFieldMap );

        return getJsonResponse(orgId, Collections.emptyMap(), graphAxisList, viewType, i18nResolver);
    }

    /**
     * {@inheritDoc }
     *
     * @param orgId      Represents an organisation id
     * @param crmUserMap Contains the collection of crm users
     * @param data       Represents the data
     * @return The {@link JsonObject} details
     */
    @NonNull
    @Override
    protected JsonObject getJsonResponse( final @NonNull Long orgId,
                                          final @NonNull Map<Long, CrmUser> crmUserMap,
                                          final @NonNull Object data,
                                          final CRMViewType viewType,
                                          final @NonNull I18nResolver i18nResolver)
    {
        if( CRMViewType.KPI == viewType )
        {
            return crmReportsAPI.getKPIDataAPI()
                                .getResponse( orgId, ( List<GraphAxis<Object>> ) data, crmUserMap, i18nResolver );
        }

        if(Objects.equals(CRMViewType.BUBBLE, viewType)){
            return crmReportsAPI.getBubbleChartDataAPI()
                                .getResponse( orgId, ( List<GraphAxis<Object>> ) data, crmUserMap, i18nResolver );
        }

        return crmReportsAPI.getChartDataAPI()
                            .getResponse( orgId, ( List<GraphAxis<Object>> ) data, crmUserMap, i18nResolver );
    }

    /**
     * <p>
     * Gets the list of {@link GraphAxis} details
     * </p>
     *
     * @param viewType       Represents the {@link CRMViewType}
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType}
     * @param requestData      Represents the {@link JsonObject} details
     * @param i18nResolver     Represents the {@link I18nResolver}
     * @param reportFieldMap    The fetched ReportModularField map, may be null
     * @return The list of {@link GraphAxis}
     */
    private List<GraphAxis<Object>> getGraphAxis( final CRMViewType viewType,
                                                  final AnalyticsRequest analyticsRequest,
                                                  final ActionType actionType,
                                                  final JsonObject requestData,
                                                  final I18nResolver i18nResolver,
                                                  final Map<Long, ReportModularField> reportFieldMap )
    {
        if( CRMViewType.KPI == viewType )
        {
            return crmReportsAPI.getKPIDataAPI()
                                .getGraphAxis( analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap );
        }

        if(Objects.equals(CRMViewType.BUBBLE, viewType)){
            return crmReportsAPI.getBubbleChartDataAPI()
                                .getGraphAxis( analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap );
        }
        return crmReportsAPI.getChartDataAPI()
                            .getGraphAxis( analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap );
    }

}
