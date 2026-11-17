package com.tlc.crm.common.internal.service;

import com.tlc.commons.service.Service;
import com.tlc.crm.common.internal.handler.RequestHandler;
import com.tlc.crm.common.internal.service.impl.BulkRequestService;
import com.tlc.crm.common.internal.service.impl.SingleRequestService;
import com.tlc.crm.common.models.RequestType;

/**
 * Provides a service for obtaining the asynchronous {@link RequestHandler} responsible for processing the request
 *
 * @author sundar
 * @version 1.0
 * @see Service
 */
public interface RequestService extends Service {

    /**
     * Gets the {@link RequestService} instance
     *
     * @return {@link RequestService}
     */
    static RequestService get(final RequestType requestType) {
        return switch (requestType) {
            case BULK -> BulkRequestService.getInstance();
            case SINGLE -> SingleRequestService.getInstance();
            //TODO If you need any request handler, then add the suitable request type and then implement the class {RequestService}
        };
    }

    /**
     * Gets the request handler to handle the request asynchronously
     *
     * @return {@link RequestHandler}
     */
    RequestHandler getRequestHandler();
}