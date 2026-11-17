package com.tlc.crm.core.action.analytics.reports.data.filter;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.commons.listeners.Callback;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.reports.data.UserIdFetchHandler;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.action.analytics.ReportsAction;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.i18n.I18nResolver;

import com.tlc.web.WebAction;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Manages report filter data related web actions
 * </p>
 *
 * @author jamesh
 * @version 1.0
 * @see ReportsAction
 */
@WebAction( path="/crm/analytics/filter-data" )
public class CRMReportFilterDataAPI extends ReportsAction
{

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMReportFilterDataAPI.class);

    private final UserIdFetchHandler userIdFetchHandler;
    private final CRMReportsAPI crmReportsAPI;
    private final ReportModularFieldService reportModularFieldService;

    public CRMReportFilterDataAPI()
    {
        userIdFetchHandler = UserIdFetchHandler.getINSTANCE();
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
     @throws Exception if the validation fails or any error occurs during validation
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType ) throws Exception
    {}

    /**
     * {@inheritDoc}
     *
     * @param analyticsRequest Represents the {@link AnalyticsRequest} details
     * @param actionType       Represents the {@link ActionType} that contains the type of action to be performed
     * @param requestData      A {@link JsonObject} containing the request data needed to generate the response
     * @param i18nResolver     An instance of {@link I18nResolver} for handling internationalization
     * @return the response as a {@link JsonObject}
     */
    @Override
    protected final void processRequest(final @NonNull AnalyticsRequest analyticsRequest,
                                        final @NonNull ActionType actionType,
                                        final @NonNull JsonObject requestData,
                                        final @NonNull I18nResolver i18nResolver) {
        final Long orgId = analyticsRequest.orgId();

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {
            @Override
            public void onSuccess(final Map<Long, ReportModularField> reportFieldMap) {
                processFilterData(analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap);
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("CRM Analytics: Failed to fetch report modular fields for filter-data, orgId: {}", orgId, cause);
                analyticsRequest.crmResponse().setErrorCode(ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD, "i18n.crm.analytics.invalid.field"));
                analyticsRequest.crmResponse().send();
            }
        });
    }

    /**
     * <p>
     * Processes filter data and sends the response
     * </p>
     *
     * @param analyticsRequest  Represents the {@link AnalyticsRequest} details
     * @param actionType        Represents the {@link ActionType}
     * @param requestData       Represents the {@link JsonObject} details
     * @param i18nResolver      Represents the {@link I18nResolver}
     * @param reportFieldMap    The fetched ReportModularField map, may be null
     */
    private void processFilterData(final @NonNull AnalyticsRequest analyticsRequest,
                                   final @NonNull ActionType actionType,
                                   final @NonNull JsonObject requestData,
                                   final @NonNull I18nResolver i18nResolver,
                                   final Map<Long, ReportModularField> reportFieldMap)
    {
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        try {
            final Long orgId = analyticsRequest.orgId();
            final List<GraphAxis<Object>> graphAxes = crmReportsAPI.getFilterDataAPI()
                                                                   .getGraphAxis( analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap );
            final Collection<Long> userIds = userIdFetchHandler.getUserIds( orgId, graphAxes, reportFieldMap );

            if( userIds.isEmpty() )
            {
                crmReportsAPI.getFilterDataAPI()
                            .getResponse( orgId, graphAxes, Collections.emptyMap(), i18nResolver )
                            .forEach(crmResponse::put);
                crmResponse.send();
                return;
            }

            fetchAndDispatchUser( orgId, userIds, graphAxes, analyticsRequest.crmResponse(), null, i18nResolver, null );
        } catch (final Exception exception) {
            crmResponse.setErrorCode(ErrorCode.get(AnalyticsErrorCodes.INVALID_FIELD, "i18n.crm.analytics.invalid.field", exception));
            crmResponse.send();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents an organisation id
     * @param crmUserMap   Contains the collection of crm users
     * @param data         Represents the data
     * @param i18nResolver Represents the {@link I18nResolver}
     * @return The {@link JsonObject}
     */
    @NonNull
    @Override
    protected JsonObject getJsonResponse( final @NonNull Long orgId,
                                          final @NonNull Map<Long, CrmUser> crmUserMap,
                                          final @NonNull Object data,
                                          final CRMViewType viewType,
                                          final @NonNull I18nResolver i18nResolver )
    {
        return crmReportsAPI.getFilterDataAPI()
                            .getResponse( orgId, ( List<GraphAxis<Object>> ) data, crmUserMap, i18nResolver );
    }
}
