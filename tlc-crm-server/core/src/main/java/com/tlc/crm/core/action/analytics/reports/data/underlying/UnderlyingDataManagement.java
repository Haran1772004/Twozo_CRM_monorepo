package com.tlc.crm.core.action.analytics.reports.data.underlying;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.listeners.Callback;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.iam.api.model.CrmUser;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.ReportsAction;
import com.tlc.crm.core.action.analytics.reports.data.ReportEntityResolver;
import com.tlc.crm.core.action.analytics.reports.data.UserIdFetchHandler;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.entity.config.modules.ReportModularField;
import com.tlc.crm.core.api.analytics.integ.models.config.layout.dashboard.view.CRMViewType;
import com.tlc.crm.core.api.analytics.service.module.ReportModularFieldService;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.Validator;
import com.tlc.crm.security.permission.annotation.authorizer.AnalyticsAuthorizer;
import com.tlc.i18n.I18nResolver;

import com.tlc.web.WebAction;

import lombok.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Manages underlying data Layout related web actions.
 *
 * <h3>Flow</h3>
 * <ol>
 *     <li>{@code validateRequest} (async) — delegates to {@code ViewUnderlyingValidator}'s
 *         async override, which fetches {@code reportFieldMap} via callback and validates
 *         inside that callback.</li>
 *     <li>{@code getResponse} — fetches {@code reportFieldMap} via its own callback,
 *         builds graph axes, resolves users, sends response — all inside the callback.</li>
 * </ol>
 * <p>Each method fetches {@code reportFieldMap} independently via its own callback.
 * No ThreadLocal, no shared mutable state, no race conditions.</p>
 *
 * @author KarthickV
 * @version 1.0
 * @see ReportsAction
 */
@AnalyticsAuthorizer
@WebAction( path = "/crm/analytics/underlying" )
public class UnderlyingDataManagement extends ReportsAction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UnderlyingDataManagement.class);

    private final Validator validator;
    private final CRMReportsAPI crmReportsAPI;
    private final UserIdFetchHandler userIdFetchHandler;
    private final ReportModularFieldService reportModularFieldService;
    private final ReportEntityResolver reportEntityResolver;

    public UnderlyingDataManagement()
    {
        validator = Validator.INSTANCE();
        crmReportsAPI = CRMReportsAPI.getInstance();
        userIdFetchHandler = UserIdFetchHandler.getINSTANCE();
        reportModularFieldService = ReportModularFieldService.getInstance();
        reportEntityResolver = ReportEntityResolver.getInstance();
    }

    /**
     * No-op sync override — async version handles validation.
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType ) throws Exception
    {
    }

    /**
     * Async override — delegates to {@code ViewUnderlyingValidator}'s async
     * {@code validateRequest}, which fetches {@code reportFieldMap} via callback
     * and validates inside that callback.
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType,
                                    final @NonNull Consumer<Exception> completionHandler )
    {
        validator.viewUnderlyingValidator()
                 .validateRequest( orgId, userId, data, completionHandler );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles only {@link ActionType#GET} — fetches report modular fields asynchronously,
     * builds the underlying data graph axes, resolves user details, and sends the response
     * from within the callback. Throws for unsupported action types.</p>
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
        if ( ActionType.GET != actionType )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_ACTION_TYPE,
                                 "i18n.analytics.invalid.action.type" );
        }

        final Long orgId = analyticsRequest.orgId();
        final CrmResponse crmResponse = analyticsRequest.crmResponse();

        reportModularFieldService.getByOrgId(orgId, new Callback<>() {

            @Override
            public void onSuccess( final Map<Long, ReportModularField> reportFieldMap )
            {
                try
                {
                    final List<GraphAxis<Object>> graphAxes = crmReportsAPI.underlyingDataLayoutAPI()
                            .getGraphAxis( analyticsRequest, actionType, requestData, i18nResolver, reportFieldMap );

                    reportEntityResolver.replaceEntityIdsWithDisplayNames(orgId, graphAxes, reportFieldMap);

                    final Collection<Long> userIds = userIdFetchHandler.getUserIds( orgId, graphAxes, reportFieldMap );

                    if ( userIds.isEmpty() )
                    {
                        final JsonObject response = crmReportsAPI.underlyingDataLayoutAPI()
                                .getResponse( orgId, graphAxes, Collections.emptyMap(), i18nResolver );

                        response.forEach( crmResponse::put );
                        crmResponse.send();
                    }
                    else
                    {
                        fetchAndDispatchUser( orgId, userIds, graphAxes, crmResponse, null, i18nResolver, null );
                    }
                }
                catch ( final Exception ex )
                {
                    LOGGER.error("CRM Analytics: Failed to process underlying data for orgId: {}", orgId, ex);
                    sendError( crmResponse, null,
                               ErrorCode.get( AnalyticsErrorCodes.UNDERLYING_DATA_FETCH_ACTION_FAILED,
                                              "i18n.analytics.underlying.data.fetch.action.failed" ) );
                }
            }

            @Override
            public void onFailure( final Throwable cause )
            {
                LOGGER.error("CRM Analytics: Failed to fetch report field map for orgId: {}", orgId, cause);
                sendError( crmResponse, null,
                           ErrorCode.get( AnalyticsErrorCodes.UNDERLYING_DATA_FETCH_ACTION_FAILED,
                                          "i18n.analytics.underlying.data.fetch.action.failed" ) );
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    protected JsonObject getJsonResponse( final @NonNull Long orgId,
                                          final @NonNull Map<Long, CrmUser> crmUserMap,
                                          final @NonNull Object data,
                                          final CRMViewType viewType,
                                          final @NonNull I18nResolver i18nResolver )
    {
        try
        {
            return crmReportsAPI.underlyingDataLayoutAPI()
                                .getResponse( orgId, ( List<GraphAxis<Object>> ) data, crmUserMap, i18nResolver );
        }
        catch ( final Exception error )
        {
            throw ErrorCode.get( AnalyticsErrorCodes.CHART_DATA_FETCH_ACTION_FAILED,
                                 "i18n.analytics.chart.data.fetch.action.failed" );
        }
    }
}
