package com.tlc.crm.common.service;

import com.tlc.commons.service.Service;

import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.internal.handler.impl.FileEventRequestHandler;
import com.tlc.crm.common.internal.handler.impl.SocialEventRequestHandler;
import com.tlc.crm.common.internal.handler.impl.TelephoneEventRequestHandler;
import com.tlc.crm.common.internal.service.impl.EventRequestServiceImpl;

/**
 * Provides a service for obtaining the asynchronous {@link EventRequestHandler}.
 * responsible for processing the event request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface EventRequestService extends Service {

    /**
     * Gets the {@link EventRequestService} instance.
     *
     * @return {@link EventRequestService}.
     */
    static EventRequestService get() {
        return EventRequestServiceImpl.getInstance();
    }

    /**
     * Gets the request handler to handle the event request asynchronously.
     *
     * @return {@link EventRequestHandler}.
     */
    EventRequestHandler getEventHandler();

    /**
     * Gets the request handler to handle the social event request asynchronously
     *
     * @return {@link SocialEventRequestHandler}
     */
    SocialEventRequestHandler getSocialEventRequestHandler();

    /**
     * <p>
     * Gets the request handler to handle the telephone event request asynchronously
     * </p>
     *
     * @return {@link TelephoneEventRequestHandler}
     */
    TelephoneEventRequestHandler getTelephoneEventRequestHandler();

    /**
     * <p>
     * Gets the request handler to handle the file event request asynchronously
     * </p>
     *
     * @return {@link FileEventRequestHandler}
     */
    FileEventRequestHandler getFileEventRequestHandler();
}
