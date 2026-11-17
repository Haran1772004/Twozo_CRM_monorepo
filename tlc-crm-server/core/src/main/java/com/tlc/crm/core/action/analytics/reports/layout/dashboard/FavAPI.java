package com.tlc.crm.core.action.analytics.reports.layout.dashboard;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.json.Json;
import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.ReportsAction;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.core.validator.analytics.Validator;
import com.tlc.i18n.I18nResolver;

import com.tlc.web.WebAction;

import lombok.NonNull;

/**
 * Handles the HTTP requests that are relevant to fav related actions
 *
 * @author KarthickV
 * @version 1.0
 */
@WebAction( path = "/crm/analytics/dashboard/fav" )
public class FavAPI extends ReportsAction
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
        switch( actionType )
        {
            case CREATE, DELETE -> Validator.INSTANCE()
                                            .favouriteValidator()
                                            .validateRequest( orgId, userId, data );
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stub implementation — the Fav API is not available for this release.
     * Always sends an error response regardless of the action type.</p>
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
        final CrmResponse crmResponse = analyticsRequest.crmResponse();
        final JsonObject response = Json.object();
        response.put("ERROR", "Fav API is not available for this release").forEach(crmResponse::put);
        crmResponse.send();
    }
}
