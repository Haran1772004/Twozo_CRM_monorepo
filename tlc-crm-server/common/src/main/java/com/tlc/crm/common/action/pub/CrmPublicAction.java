package com.tlc.crm.common.action.pub;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.AbstractCrmAction;
import com.tlc.crm.common.action.CrmResponse;

/**
 * @author Abishek
 * @version 1.0
 */
public abstract class CrmPublicAction extends AbstractCrmAction
{
    @Override
    public final void process(CrmRequest request, CrmResponse crmResponse) throws Exception
    {
        try {
            pProcess(request, crmResponse);
        } catch (Throwable throwable) {
            sendError(crmResponse, null, throwable);
        }

        if (!crmResponse.isAsync()) {
            crmResponse.send();
        }
    }

    public abstract void pProcess(CrmRequest request, CrmResponse crmResponse) throws Exception;
}