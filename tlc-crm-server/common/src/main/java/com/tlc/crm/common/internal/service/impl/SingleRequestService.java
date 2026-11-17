package com.tlc.crm.common.internal.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.internal.handler.RequestHandler;
import com.tlc.crm.common.internal.handler.impl.SingleRequestHandler;
import com.tlc.crm.common.internal.service.RequestService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides implementation for the {@link RequestService} that manages requests in an asynchronous manner
 *
 * @author sundar
 * @version 1.0
 * @see RequestService
 */
public class SingleRequestService implements RequestService {

    private static final AtomicReference<RequestService> CRM_REQUEST_SERVICE_REFERENCE = new AtomicReference<>();

    private RequestHandler requestHandler;

    /**
     * Gets the instance of {@link RequestService}
     *
     * @return {@link RequestService}
     */
    public static RequestService getInstance() {
        final RequestService service = CRM_REQUEST_SERVICE_REFERENCE.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        }
        return service;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Which contains properties to be used to initiate the {@link RequestService}
     */
    @Override
    public void start(final Configurations configurations) {

        if (!CRM_REQUEST_SERVICE_REFERENCE.compareAndSet(null, this)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
        final String requestHandlerPoolWithSize = configurations.getConfig("request.handler.pool.single");

        if (Objects.nonNull(requestHandlerPoolWithSize)) {
            final String[] poolNameAndSize = requestHandlerPoolWithSize.split(":");

            this.requestHandler = new SingleRequestHandler(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        CRM_REQUEST_SERVICE_REFERENCE.set(null);

        if (Objects.nonNull(requestHandler)) {
            ((SingleRequestHandler) requestHandler).shutdown();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link RequestHandler}
     */
    @Override
    public RequestHandler getRequestHandler() {
        return requestHandler;
    }

}
