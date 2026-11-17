package com.tlc.file.common.action;

import com.tlc.commons.service.Service;

/**
 * Provides a service for obtaining the asynchronous {@link RequestHandler} responsible for processing the request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface RequestService extends Service {

    /**
     * Gets the {@link RequestService} instance
     *
     * @return {@link RequestService}
     */
    static RequestService get() {
        return RequestServiceImpl.getInstance();
    }

    /**
     * Gets the request handler to handle the request asynchronously
     *
     * @return {@link RequestHandler}
     */
    RequestHandler getRequestHandler();
}
