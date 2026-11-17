package com.tlc.crm.core.action.analytics.reports.layout.dashboard;

import com.tlc.analytics.config.action.AnalyticsRequest;
import com.tlc.analytics.config.model.view.layout.DashboardLayout;

import com.tlc.crm.core.action.analytics.ActionType;
import com.tlc.crm.core.action.analytics.ReportsAction;
import com.tlc.crm.core.api.analytics.converter.CRMReportsAPI;
import com.tlc.crm.core.api.analytics.converter.layout.CRMReportsLayout;
import com.tlc.crm.core.initializer.AnalyticsORGModularFieldInitializer;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.core.validator.analytics.Validator;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.security.permission.annotation.authorizer.AnalyticsAuthorizer;

import com.tlc.i18n.I18nResolver;

import com.tlc.web.WebAction;

import lombok.NonNull;

import java.util.function.Consumer;

/**
 * <p>
 * Manages Dashboard Layout related web actions
 * </p>
 *
 * @author James
 * @version 1.0
 * @see ReportsAction
 */
@AnalyticsAuthorizer
@WebAction(path = "/crm/analytics/dashboard-layout")
public class DashboardLayoutAPI extends ReportsAction {
    private final Validator validator;
    private final CRMReportsLayout<DashboardLayout> dashboardLayoutAPI;
    private final AnalyticsORGModularFieldInitializer anaOrgModularFieldInitializer;

    /**
     * Creates dashboard layout action dependencies.
     */
    public DashboardLayoutAPI() {
        validator = Validator.INSTANCE();
        dashboardLayoutAPI = CRMReportsAPI.getInstance().dashboardLayoutAPI();
        anaOrgModularFieldInitializer = AnalyticsORGModularFieldInitializer.getInstance();
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
        switch (actionType) {
            case CREATE -> validator.dashCreateValidator().validateRequest(orgId, userId, data);
            case UPDATE -> validator.dashUpdateValidator().validateRequest(orgId, userId, data);
            case GET -> validator.dashGetValidator().validateRequest(orgId, userId, data);
            case CLONE -> validator.dashCloneValidator().validateRequest(orgId, userId, data);
            case MOVE -> validator.dashMoveValidator().validateRequest(orgId, userId, data);
            case FAV -> validator.dashFavouriteValidator().validateRequest(orgId, userId, data);
            case TRASH -> validator.dashboardTrashValidator().validateRequest(orgId, userId, data);
            case RESTORE -> validator.dashboardRestoreValidator().validateRequest(orgId, userId, data);
            case DELETE -> validator.dashboardDeleteValidator().validateRequest(orgId, userId, data);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Async version that delegates to the validator's async method for UPDATE action,
     * since dashboard update validation involves child view validation which requires
     * async report modular field fetching. For other actions, delegates to the base class default.
     * </p>
     *
     * @param orgId             Represents an organization id associated with the request
     * @param userId            Represents the user id associated with the request
     * @param data              Represents the JSON data that needs to be validated
     * @param actionType        Represents the type of action to be performed
     * @param completionHandler Called with {@code null} on success, or with the exception on failure
     */
    @Override
    protected void validateRequest(final @NonNull Long orgId,
                                   final @NonNull Long userId,
                                   final @NonNull JsonObject data,
                                   final @NonNull ActionType actionType,
                                   final @NonNull Consumer<Exception> completionHandler) {
        if (ActionType.UPDATE == actionType) {
            validator.dashUpdateValidator().validateRequest(orgId, userId, data, completionHandler);
        } else {
            super.validateRequest(orgId, userId, data, actionType, completionHandler);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to the dashboard layout API based on the action type (CREATE, UPDATE, GET,
     * MOVE, CLONE, FAV, TRASH, RESTORE, DELETE) and sends the response. Throws for unsupported
     * action types.</p>
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
        switch (actionType) {
            case CREATE -> dashboardLayoutAPI.create(analyticsRequest, requestData, i18nResolver, actionType);
            case UPDATE -> dashboardLayoutAPI.update(analyticsRequest, requestData, i18nResolver, actionType);
            case GET -> dashboardLayoutAPI.processDashboardResponse(analyticsRequest, requestData, i18nResolver, actionType);
            case MOVE -> dashboardLayoutAPI.move(analyticsRequest, requestData, i18nResolver);
            case CLONE -> dashboardLayoutAPI.clone(analyticsRequest, requestData, i18nResolver);
            case FAV -> dashboardLayoutAPI.favourite(analyticsRequest, requestData, i18nResolver);
            case TRASH -> dashboardLayoutAPI.trash(analyticsRequest, requestData, i18nResolver);
            case RESTORE -> dashboardLayoutAPI.restore(analyticsRequest, requestData, i18nResolver);
            case DELETE -> dashboardLayoutAPI.delete(analyticsRequest, requestData, i18nResolver);
            case LOAD -> anaOrgModularFieldInitializer.load();
            default -> throw ErrorCode.get(AnalyticsErrorCodes.INVALID_ACTION_TYPE,
                    "i18n.analytics.invalid.action.type");
        }
    }
}
