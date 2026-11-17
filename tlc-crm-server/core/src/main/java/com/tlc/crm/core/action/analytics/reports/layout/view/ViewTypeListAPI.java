package com.tlc.crm.core.action.analytics.reports.layout.view;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.ReportsAction;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import com.tlc.i18n.I18nResolver;

import com.tlc.web.WebAction;

import lombok.NonNull;

import java.util.Objects;

/**
 * Handles the HTTP requests that are relevant to view type list related actions
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction( path = "/crm/analytics/view-layout/list" )
public class ViewTypeListAPI extends ReportsAction
{

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents the organization ID
     * @param userId     Represents the user ID
     * @param data       Which contains the requested data
     * @param actionType The type of action being performed
     * @throws Exception if any validation fails.
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
     * <p>Handles only {@link ActionType#LIST} — fetches the available view type list and sends
     * the response. Throws for unsupported action types.</p>
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
        if ( Objects.equals( ActionType.LIST, actionType ) )
        {
            final CrmResponse crmResponse = analyticsRequest.crmResponse();
            CRMReportsAPI.getInstance()
                    .getViewTypeList(i18nResolver)
                    .forEach(crmResponse::put);
            crmResponse.send();
            return;
        }
        throw ErrorCode.get( AnalyticsErrorCodes.INVALID_ACTION_TYPE,
                             "i18n.analytics.invalid.action.type" );
    }
}
