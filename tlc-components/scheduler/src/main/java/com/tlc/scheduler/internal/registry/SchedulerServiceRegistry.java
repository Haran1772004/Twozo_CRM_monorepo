package com.tlc.scheduler.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.scheduler.service.SchedulerService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link SchedulerService} into the registry.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SchedulerServiceRegistry implements ServiceRegistry<SchedulerService> {

    private final AtomicReference<SchedulerService> schedulerService;

    private static final class Instance {
        private static final SchedulerServiceRegistry SCHEDULER_SERVICE_REGISTRY = new SchedulerServiceRegistry();
    }

    private SchedulerServiceRegistry() {
        this.schedulerService = new AtomicReference<>();
    }

    public static ServiceRegistry<SchedulerService> getInstance() {
        return Instance.SCHEDULER_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final SchedulerService service) {
        if (!schedulerService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public SchedulerService get() {
        return schedulerService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link SchedulerService}.
     */
    @Override
    public SchedulerService unregister() {
        return schedulerService.getAndSet(null);
    }
}
