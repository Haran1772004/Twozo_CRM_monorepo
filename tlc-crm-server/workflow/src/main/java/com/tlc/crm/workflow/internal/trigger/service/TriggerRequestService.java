package com.tlc.crm.workflow.internal.trigger.service;

import com.tlc.commons.service.Service;

import com.tlc.crm.workflow.internal.trigger.handler.TriggerRequestHandler;
import com.tlc.crm.workflow.internal.trigger.service.impl.TriggerRequestServiceImpl;

/**
 * Provides a service for obtaining the asynchronous {@link TriggerRequestHandler}
 * responsible for processing the trigger request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface TriggerRequestService extends Service {

    /**
     * Gets the {@link TriggerRequestService} instance.
     *
     * @return {@link TriggerRequestService}.
     */
    static TriggerRequestService get() {
        return TriggerRequestServiceImpl.getInstance();
    }

    /**
     * Gets the request handler to handle the trigger request asynchronously.
     *
     * @return {@link TriggerRequestHandler}.
     */
    TriggerRequestHandler getTriggerHandler();
}
