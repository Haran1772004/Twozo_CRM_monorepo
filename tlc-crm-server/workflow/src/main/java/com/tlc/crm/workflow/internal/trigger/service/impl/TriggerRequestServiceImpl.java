package com.tlc.crm.workflow.internal.trigger.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.workflow.internal.trigger.handler.TriggerRequestHandler;
import com.tlc.crm.workflow.internal.trigger.handler.impl.TriggerRequestHandlerImpl;
import com.tlc.crm.workflow.internal.trigger.service.TriggerRequestService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a service for obtaining the asynchronous {@link TriggerRequestHandler}
 * responsible for processing the trigger request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class TriggerRequestServiceImpl implements TriggerRequestService {

    private static final AtomicReference<TriggerRequestService> TRIGGER_SERVICE_REFERENCE = new AtomicReference<>();

    private TriggerRequestHandler triggerRequestHandler;

    /**
     * Gets the instance of {@link TriggerRequestService}.
     *
     * @return {@link TriggerRequestService}.
     */
    public static TriggerRequestService getInstance() {
        final TriggerRequestService service = TRIGGER_SERVICE_REFERENCE.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        }
        return service;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Which contains properties to be used to initiate the {@link TriggerRequestService}.
     */
    @Override
    public void start(final Configurations configurations) {

        if (!TRIGGER_SERVICE_REFERENCE.compareAndSet(null, this)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
        final String requestHandlerPoolWithSize = configurations.getConfig("trigger.handler.pool");

        if (Objects.nonNull(requestHandlerPoolWithSize)) {
            final String[] poolNameAndSize = requestHandlerPoolWithSize.split(":");

            this.triggerRequestHandler = new TriggerRequestHandlerImpl(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        TRIGGER_SERVICE_REFERENCE.set(null);

        if (Objects.nonNull(triggerRequestHandler)) {
            ((TriggerRequestHandlerImpl) triggerRequestHandler).shutdown();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link TriggerRequestHandler}.
     */
    @Override
    public TriggerRequestHandler getTriggerHandler() {
        return triggerRequestHandler;
    }
}
