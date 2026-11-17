package com.tlc.crm.zapier.action.web;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.zapier.action.web.secure.SecureZapierAction;
import com.tlc.crm.zapier.api.marketplace.action.manager.ZapierAppManager;
import com.tlc.crm.zapier.fields.ZapAttributes;

import com.tlc.web.WebAction;

/**
 * Handles web actions for zapier user authentication management.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@WebAction(path = "/zap/user/authenticate")
public class ZapUserAuthentication extends SecureZapierAction {

    /**
     * {@inheritDoc}
     *
     * @param request  The request containing necessary data for processing.
     * @param response The response object where the processed result will be stored.
     */
    @Override
    protected void zapProcess(final CrmRequest request, final CrmResponse response) {
        ZapierAppManager.getInstance().updateConnectionState(request.orgId(), request.userId());
        response.put(ZapAttributes.MESSAGE, response.resolveI18nString("i18n.crm.zapier.user.authenticate.success"));
        response.send();
    }
}
