package com.tlc.crm.zapier.internal.action.dispatcher;

import com.tlc.crm.zapier.api.models.ZapActionRequestContext;

/**
 * Responsible for dispatching Zapier action requests to their corresponding handlers.
 * Acts as an entry point to route the action based on the context of the request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
interface ZapActionDispatcher {

    /**
     * Dispatches the given Zapier action request context to the appropriate handler
     * based on the action type or event defined in the context.
     *
     * @param zapActionRequestContext The context containing the request, response, and action details.
     */
    void dispatch(final ZapActionRequestContext zapActionRequestContext);
}
