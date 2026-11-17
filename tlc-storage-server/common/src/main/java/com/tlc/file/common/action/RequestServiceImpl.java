package com.tlc.file.common.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a service for obtaining the asynchronous {@link RequestHandler} responsible for processing the request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class RequestServiceImpl implements RequestService {

    private static final AtomicReference<RequestService> REQUEST_SERVICE_REFERENCE = new AtomicReference<>();

    private RequestHandler requestHandler;

    /**
     * Gets the instance of {@link RequestService}.
     *
     * @return {@link RequestService}.
     */
    public static RequestService getInstance() {
        final RequestService service = REQUEST_SERVICE_REFERENCE.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        }
        return service;
    }

    @Override
    public void start(final Configurations configurations) {
        if (!REQUEST_SERVICE_REFERENCE.compareAndSet(null, this)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
        final String requestHandlerPoolWithSize = configurations.getConfig("request.handler.pool");

        if (Objects.nonNull(requestHandlerPoolWithSize)) {
            final String[] poolNameAndSize = requestHandlerPoolWithSize.split(":");

            this.requestHandler = new RequestHandlerImpl(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0]);
        }
    }

    @Override
    public void stop() {
        REQUEST_SERVICE_REFERENCE.set(null);

        if (Objects.nonNull(requestHandler)) {
            ((RequestHandlerImpl) requestHandler).shutdown();
        }
    }

    /**
     * Gets the request handler to handle the request asynchronously
     *
     * @return {@link RequestHandler}
     */
    @Override
    public RequestHandler getRequestHandler() {
        return requestHandler;
    }
}
