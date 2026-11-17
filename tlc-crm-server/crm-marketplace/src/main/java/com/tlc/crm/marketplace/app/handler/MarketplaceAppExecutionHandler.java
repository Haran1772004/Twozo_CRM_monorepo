package com.tlc.crm.marketplace.app.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.marketplace.fields.MarketplaceFields;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.marketplace.app.api.model.converter.CrmMarketplaceAppConverter;
import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;
import com.tlc.crm.marketplace.app.fields.CrmMarketplaceFields;
import com.tlc.crm.marketplace.app.internal.status.CrmMarketplaceErrorCodes;
import com.tlc.crm.marketplace.app.service.CrmMarketplaceAppService;

import java.util.Objects;

/**
 * <p>
 * Abstract handler class for managing marketplace app actions
 * </p>
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public abstract class MarketplaceAppExecutionHandler {

    private final CrmMarketplaceAppService crmMarketplaceAppService;
    private final CrmMarketplaceAppConverter crmMarketplaceAppConverter;

    protected MarketplaceAppExecutionHandler() {
        crmMarketplaceAppService = CrmMarketplaceAppService.getInstance();
        crmMarketplaceAppConverter = CrmMarketplaceAppConverter.getInstance();
    }

    /**
     * Handles the marketplace app operations.
     *
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    public void handle(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final long orgId = crmRequest.orgId();
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(MarketplaceFields.TYPE);
        final JsonObject data = requestJson.getJsonObject(MarketplaceFields.DATA);
        final long appId = data.getLong(MarketplaceFields.ID);

        switch (type) {
            case CrmMarketplaceFields.INSTALL -> install(orgId, appId, crmRequest, crmResponse);
            case CrmMarketplaceFields.UNINSTALL -> uninstall(orgId, appId, crmRequest, crmResponse);
            case CrmMarketplaceFields.APP_PERMISSION -> appPermission(appId, crmResponse);
            case CrmMarketplaceFields.SUMMARY -> summary(orgId, appId, crmRequest, crmResponse);
            case CrmMarketplaceFields.EXECUTE_ACTION -> executeAction(crmRequest, crmResponse);
        }
    }

    /**
     * Constructs and returns a JSON representation of the marketplace app.
     *
     * @param orgId    Refers the organization identifier.
     * @param appId    Represents the ID of the application.
     * @param request  The {@code CrmRequest}.
     * @param response The response object used to report errors or status.
     */
    protected void summary(final long orgId, final long appId, final CrmRequest request, final CrmResponse response) {
        final CrmMarketplaceApp crmMarketplaceApp = crmMarketplaceAppService.get(appId);

        if (Objects.isNull(crmMarketplaceApp)) {
            response.setErrorCode(ErrorCode.get(CrmMarketplaceErrorCodes.APP_NOT_FOUND, "i18n.marketplace.app.not.found"));
            response.send();
        } else {
            final JsonObject jsonObject = crmMarketplaceAppConverter.convertSummaryJson(orgId, appId, request, crmMarketplaceApp);
            response.put(MarketplaceFields.SUMMARY, jsonObject);
            response.send();
        }
    }

    /**
     * Handles the permission logic for a marketplace app based on the provided input data.
     *
     * @param appId    Represents the ID of the application.
     * @param response The response object to populate with the result of the permission check.
     */
    protected void appPermission(final long appId, final CrmResponse response) {
        final CrmMarketplaceApp crmMarketplaceApp = crmMarketplaceAppService.get(appId);

        if (Objects.isNull(crmMarketplaceApp)) {
            response.setErrorCode(ErrorCode.get(CrmMarketplaceErrorCodes.APP_NOT_FOUND, "i18n.marketplace.app.not.found"));
        } else {
            response.put(MarketplaceFields.PERMISSION, crmMarketplaceApp.getPermission());
        }
        response.send();
    }

    /**
     * Installs a marketplace app for the specified organization.
     *
     * @param orgId    The ID of the organization where the app is to be installed.
     * @param appId    The ID of the application to be installed.
     * @param request  The CRM request context.
     * @param response The CRM response context to write results or errors.
     */
    protected abstract void install(final long orgId, final long appId, final CrmRequest request, final CrmResponse response);

    /**
     * Uninstalls a marketplace app from the specified organization.
     *
     * @param orgId    The ID of the organization where the app is to be uninstalled.
     * @param appId    The ID of the application to be uninstalled.
     * @param request  The CRM request context.
     * @param response The CRM response context to write results or errors.
     */
    protected abstract void uninstall(final long orgId, final long appId, final CrmRequest request, final CrmResponse response);

    /**
     * Handles a generic CRM request related to marketplace app operations.
     *
     * @param request  The CRM request context.
     * @param response The CRM response context to write results or errors.
     */
    protected abstract void executeAction(final CrmRequest request, final CrmResponse response);
}