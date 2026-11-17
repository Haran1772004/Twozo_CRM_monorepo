package com.tlc.crm.common.internal.service.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.handler.EventRequestHandler;
import com.tlc.crm.common.internal.handler.impl.EventRequestHandlerImpl;
import com.tlc.crm.common.internal.handler.impl.FileEventRequestHandler;
import com.tlc.crm.common.internal.handler.impl.SocialEventRequestHandler;
import com.tlc.crm.common.internal.handler.impl.TelephoneEventRequestHandler;
import com.tlc.crm.common.service.EventRequestService;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provides a service for obtaining the asynchronous {@link EventRequestHandler}
 * responsible for processing the event request.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class EventRequestServiceImpl implements EventRequestService {

    private static final AtomicReference<EventRequestService> TRIGGER_SERVICE_REFERENCE = new AtomicReference<>();

    private EventRequestHandler eventRequestHandler;
    private SocialEventRequestHandler socialEventRequestHandler;
    private TelephoneEventRequestHandler telephoneEventRequestHandler;
    private FileEventRequestHandler fileEventRequestHandler;

    /**
     * Gets the instance of {@link EventRequestService}.
     *
     * @return {@link EventRequestService}.
     */
    public static EventRequestService getInstance() {
        final EventRequestService service = TRIGGER_SERVICE_REFERENCE.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        }
        return service;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Which contains properties to be used to initiate the {@link EventRequestService}.
     */
    @Override
    public void start(final Configurations configurations) {

        if (!TRIGGER_SERVICE_REFERENCE.compareAndSet(null, this)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
        final String requestHandlerPoolWithSize = configurations.getConfig("event.handler.pool");
        final String telephoneEventHandlerPoolWithSize = configurations.getConfig("telephone.event.handler.pool");
        final String fileEventHandlerPoolWithSize = configurations.getConfig("file.event.handler.pool");

        if (Objects.nonNull(requestHandlerPoolWithSize)) {
            final String[] poolNameAndSize = requestHandlerPoolWithSize.split(":");
            final int poolSize = Integer.parseInt(poolNameAndSize[1]);
            final String poolName = poolNameAndSize[0];

            this.eventRequestHandler = new EventRequestHandlerImpl(poolSize, poolName);
            this.socialEventRequestHandler = new SocialEventRequestHandler(poolSize, poolName);
        }

        if (Objects.nonNull(telephoneEventHandlerPoolWithSize)) {
            final String[] poolNameAndSize = telephoneEventHandlerPoolWithSize.split(":");

            this.telephoneEventRequestHandler = new TelephoneEventRequestHandler(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0]);
        }

        if (Objects.nonNull(fileEventHandlerPoolWithSize)) {
            final String[] poolNameAndSize = fileEventHandlerPoolWithSize.split(":");

            this.fileEventRequestHandler = new FileEventRequestHandler(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        TRIGGER_SERVICE_REFERENCE.set(null);

        if (Objects.nonNull(eventRequestHandler)) {
            ((EventRequestHandlerImpl) eventRequestHandler).shutdown();
        }

        if (Objects.nonNull(socialEventRequestHandler)) {
            socialEventRequestHandler.shutdown();
        }

        if (Objects.nonNull(telephoneEventRequestHandler)) {
            telephoneEventRequestHandler.shutdown();
        }

        if (Objects.nonNull(fileEventRequestHandler)) {
            fileEventRequestHandler.shutdown();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public EventRequestHandler getEventHandler() {
        return eventRequestHandler;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link SocialEventRequestHandler}
     */
    @Override
    public SocialEventRequestHandler getSocialEventRequestHandler() {
        return socialEventRequestHandler;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link TelephoneEventRequestHandler}
     */
    @Override
    public TelephoneEventRequestHandler getTelephoneEventRequestHandler() {
        return telephoneEventRequestHandler;
    }

    /**
     * {@inheritDoc}
     *
     * @return the {@link FileEventRequestHandler}
     */
    @Override
    public FileEventRequestHandler getFileEventRequestHandler() {
        return fileEventRequestHandler;
    }
}
