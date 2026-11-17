package com.tlc.social.server.service;

import com.tlc.commons.service.Service;

import com.tlc.social.server.handler.SocialEventHandler;
import com.tlc.social.server.service.internal.SocialEventRequestServiceImpl;

/**
 * Provides a service for obtaining the asynchronous {@link SocialEventHandler} responsible for processing the request
 *
 * @author Maharaja S
 * @version 1.0
 */
public interface SocialEventRequestService extends Service {

    /**
     * Gets the {@link SocialEventRequestService} instance
     *
     * @return {@link SocialEventRequestService}
     */
    static SocialEventRequestService get() {
        return SocialEventRequestServiceImpl.getInstance();
    }

    /**
     * Gets the request handler to handle the request asynchronously
     *
     * @return {@link SocialEventHandler}
     */
    SocialEventHandler getRequestHandler();
}