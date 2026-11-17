package com.tlc.crm.common.internal.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.internal.handler.impl.BulkRequestHandler;
import com.tlc.crm.common.internal.handler.RequestHandler;
import com.tlc.crm.common.internal.service.RequestService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides implementation for the {@link RequestService} that manages bulk requests in an asynchronous manner.
 *
 * @author Sathish
 * @version 1.0
 * @see RequestService
 */
public class BulkRequestService implements RequestService {

    private static final AtomicReference<RequestService> BULK_REQUEST_SERVICE_REFERENCE = new AtomicReference<>();

    private RequestHandler bulkRequestHandler;

    /**
     * Gets the instance of {@link RequestService}.
     *
     * @return {@link RequestService}.
     */
    public static RequestService getInstance() {
        final RequestService service = BULK_REQUEST_SERVICE_REFERENCE.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        }
        return service;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Which contains properties to be used to initiate the {@link RequestService}.
     */
    @Override
    public void start(final Configurations configurations) {
        if (!BULK_REQUEST_SERVICE_REFERENCE.compareAndSet(null, this)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
        final String requestHandlerPoolWithSize = configurations.getConfig("request.handler.pool.bulk");

        if (Objects.nonNull(requestHandlerPoolWithSize)) {
            final String[] poolNameAndSize = requestHandlerPoolWithSize.split(":");

            this.bulkRequestHandler = new BulkRequestHandler(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        BULK_REQUEST_SERVICE_REFERENCE.set(null);

        if (Objects.nonNull(bulkRequestHandler)) {
            ((BulkRequestHandler) bulkRequestHandler).shutdown();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link RequestHandler}.
     */
    @Override
    public RequestHandler getRequestHandler() {
        return bulkRequestHandler;
    }
}
