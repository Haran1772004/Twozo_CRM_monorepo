package com.tlc.crm.zapier.action.web.secure;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.action.secure.SecureRequestAuthenticator;

import java.util.List;

/**
 * Concrete implementation of a secure CRM action that uses basic authentication.
 * <p>
 * Basic Auth (username and password) is used to authenticate the CRM user.
 * It ensures that the request is processed only after validating credentials.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class SecureZapierAction extends CrmSecureAction {

    /**
     * {@inheritDoc}
     *
     * @param request     The incoming CRM request containing relevant data.
     * @param crmResponse The CRM response object to be populated with results.
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse crmResponse) {
        zapProcess(request, crmResponse);
    }

    /**
     * Processes a Zapier request and generates a response.
     *
     * @param request  The incoming CRM request containing relevant data.
     * @param response The CRM response object to be populated with results.
     */
    protected abstract void zapProcess(final CrmRequest request, final CrmResponse response);

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SecureRequestAuthenticator> getRequestAuthenticators() {
        return List.of(ZapierBasicAuthenticator.get());
    }
}
