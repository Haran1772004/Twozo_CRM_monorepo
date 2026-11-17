package com.tlc.crm.marketplace.app.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.marketplace.app.internal.status.CrmMarketplaceErrorCodes;
import com.tlc.marketplace.fields.MarketplaceFields;

import com.tlc.crm.marketplace.app.handler.MarketplaceAppExecutionHandler;
import com.tlc.crm.marketplace.app.registry.MarketplaceAppHandlerRegistry;
import com.tlc.crm.marketplace.app.service.CrmMarketplaceAppService;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;

import com.tlc.web.WebAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Abstract base class for managing marketplace app operations within the CRM.
 * Provides a secured foundation for app-related actions such as
 * installation, uninstallation, activation, or configuration.
 * </br>
 * <span style="color:red"><b>Note:</b></span>
 * Subclasses should implement specific app-handling logic tailored to their
 * respective operations or interfaces.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/marketplace/mgmt")
public class CrmMarketplaceAppManagement extends CrmSecureAction {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CrmMarketplaceAppManagement.class);

    private final CrmMarketplaceAppService crmMarketplaceAppService;
    private final MarketplaceAppHandlerRegistry marketplaceAppHandlerRegistry;

    public CrmMarketplaceAppManagement() {
        crmMarketplaceAppService = CrmMarketplaceAppService.getInstance();
        marketplaceAppHandlerRegistry = MarketplaceAppHandlerRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the request.
     * @param response Refers the response to be sent.
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(MarketplaceFields.DATA);
        final String appName = crmMarketplaceAppService.get(data.getLong(MarketplaceFields.ID)).getName();
        final MarketplaceAppExecutionHandler marketplaceAppHandler = marketplaceAppHandlerRegistry.get(appName);

        if (Objects.nonNull(marketplaceAppHandler)) {
            marketplaceAppHandler.handle(request, response);
        } else {
            throw ErrorCode.get(CrmMarketplaceErrorCodes.MARKETPLACE_APP_HANDLER_NOT_FOUND);
        }
    }
}