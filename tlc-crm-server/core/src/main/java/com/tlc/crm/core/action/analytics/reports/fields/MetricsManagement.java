package com.tlc.crm.core.action.analytics.reports.fields;

import com.tlc.analytics.config.action.AnalyticsRequest;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.ReportsAction;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.Validator;

import com.tlc.crm.security.permission.annotation.authorizer.AnalyticsAuthorizer;

import com.tlc.i18n.I18nResolver;

import com.tlc.web.WebAction;

import lombok.NonNull;

import java.util.Objects;
import java.util.function.Consumer;


/**
 * <p>
 * Manages view metrics related web actions
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 * @see ReportsAction
 */
@AnalyticsAuthorizer
@WebAction( path = "/crm/analytics/metrics" )
public class MetricsManagement extends ReportsAction
{

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id associated with the request
     * @param userId     Represents the user id associated with the request
     * @param data       Represents the JSON data that needs to be validated
     * @param actionType Represents the type of action to be performed
     * @throws Exception if the action type is invalid or if the validation fails
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType ) throws Exception
    {
        if( Objects.equals( ActionType.GET, actionType ) )
        {
            Validator.INSTANCE()
                     .getMetricsFieldValidator()
                     .validateRequest( orgId, userId, data );
        }
        else
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_ACTION_TYPE,
                                 "i18n.analytics.invalid.action.type" );
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Async version that delegates metrics field validation to the validator's async method,
     * avoiding blocking the caller thread while waiting for the validation result.
     * </p>
     *
     * @param orgId             Represents an organization id associated with the request
     * @param userId            Represents the user id associated with the request
     * @param data              Represents the JSON data that needs to be validated
     * @param actionType        Represents the type of action to be performed
     * @param completionHandler Called with {@code null} on success, or with the exception on failure
     */
    @Override
    protected void validateRequest( final @NonNull Long orgId,
                                    final @NonNull Long userId,
                                    final @NonNull JsonObject data,
                                    final @NonNull ActionType actionType,
                                    final @NonNull Consumer<Exception> completionHandler )
    {
        if( Objects.equals( ActionType.GET, actionType ) )
        {
            Validator.INSTANCE()
                     .getMetricsFieldValidator()
                     .validateRequest( orgId, userId, data, completionHandler );
        }
        else
        {
            completionHandler.accept( ErrorCode.get( AnalyticsErrorCodes.INVALID_ACTION_TYPE,
                                                     "i18n.analytics.invalid.action.type" ) );
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Handles only {@link ActionType#GET} — delegates to the metrics API to fetch
     * available metric fields and send the response. Throws for unsupported action types.</p>
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
        if( Objects.equals( ActionType.GET, actionType ) )
        {
            CRMReportsAPI.getInstance()
                         .getMetrics()
                         .getResponse( analyticsRequest, requestData, i18nResolver );
        }
        else
        {
            throw ErrorCode.get( AnalyticsErrorCodes.INVALID_ACTION_TYPE,
                                 "i18n.analytics.invalid.action.type" );
        }
    }
}
