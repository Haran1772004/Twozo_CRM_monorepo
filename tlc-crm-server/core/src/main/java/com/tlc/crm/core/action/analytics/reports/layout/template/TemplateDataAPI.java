package com.tlc.crm.core.action.analytics.reports.layout.template;

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

/**
 * <p>
 * Manages Template related web actions
 * </p>
 *
 * @author Ryan
 * @version 1.0
 * @see TemplateDataAPI
 */

@AnalyticsAuthorizer
@WebAction( path = "/crm/analytics/template" )
public class TemplateDataAPI extends ReportsAction
{
    private final Validator validator;
    private final CRMReportsAPI crmReportsAPI;

    public TemplateDataAPI() {
        this.validator = Validator.INSTANCE();
        this.crmReportsAPI = CRMReportsAPI.getInstance();
    }

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
    protected void validateRequest(final @NonNull Long orgId,
                                   final @NonNull Long userId,
                                   final @NonNull JsonObject data,
                                   final @NonNull ActionType actionType) throws Exception {
        switch(actionType) {
            case GET -> {
                validator.templateGetValidator()
                         .validateRequest(orgId, userId, data);
            }
            case LIST -> {
                validator.templateListValidator()
                         .validateRequest(orgId, userId, data);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the template data API based on the action type (GET, LIST, CREATE, UPDATE,
     * DELETE) and sends the response. Throws for unsupported action types.</p>
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
        switch(actionType) {
            case GET -> crmReportsAPI.templateDataAPI()
                                     .processTemplateDataRequest(analyticsRequest, requestData, i18nResolver, actionType);
            case LIST -> crmReportsAPI.templateDataAPI()
                                      .processTemplateListRequest(analyticsRequest, requestData, i18nResolver, actionType);
            default -> throw ErrorCode.get(AnalyticsErrorCodes.INVALID_ACTION_TYPE);
        }
    }
}
