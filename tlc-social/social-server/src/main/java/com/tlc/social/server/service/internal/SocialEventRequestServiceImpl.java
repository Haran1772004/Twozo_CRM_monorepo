package com.tlc.social.server.service.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;

import com.tlc.social.server.handler.SocialEventHandler;
import com.tlc.social.server.handler.internal.SocialEventHandlerImpl;
import com.tlc.social.server.service.SocialEventRequestService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a service for obtaining the asynchronous {@link SocialEventHandler} responsible for processing the request
 *
 * @author Maharaja S
 * @version 1.0
 */
public class SocialEventRequestServiceImpl implements SocialEventRequestService {

    private static final AtomicReference<SocialEventRequestService> REQUEST_SERVICE_REFERENCE = new AtomicReference<>();

    private SocialEventHandler requestHandler;

    /**
     * Gets the instance of {@link SocialEventRequestService}.
     *
     * @return {@link SocialEventRequestService}.
     */
    public static SocialEventRequestService getInstance() {
        final SocialEventRequestService service = REQUEST_SERVICE_REFERENCE.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Social event request service not initialized");
        }
        return service;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the configs
     */
    @Override
    public void start(final Configurations configurations) {
        if (!REQUEST_SERVICE_REFERENCE.compareAndSet(null, this)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Social event request service already initialized");
        }
        final String requestHandlerPoolWithSize = configurations.getConfig("request.handler.pool");

        if (Objects.nonNull(requestHandlerPoolWithSize)) {
            final String[] poolNameAndSize = requestHandlerPoolWithSize.split(":");

            this.requestHandler = new SocialEventHandlerImpl(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        REQUEST_SERVICE_REFERENCE.set(null);

        if (Objects.nonNull(requestHandler)) {
            ((SocialEventHandlerImpl) requestHandler).shutdown();
        }
    }

    /**
     * Gets the request handler to handle the request asynchronously
     *
     * @return {@link SocialEventHandler}
     */
    @Override
    public SocialEventHandler getRequestHandler() {
        return requestHandler;
    }
}