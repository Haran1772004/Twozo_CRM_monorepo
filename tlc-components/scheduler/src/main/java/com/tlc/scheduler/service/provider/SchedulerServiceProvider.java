package com.tlc.scheduler.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.scheduler.internal.registry.SchedulerServiceRegistry;
import com.tlc.scheduler.service.SchedulerService;

import java.util.Objects;

/**
 * Provides the services that enable the {@link SchedulerService} to execute its functions effectively.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SchedulerServiceProvider {

    private static final ServiceRegistry<SchedulerService> SCHEDULER_SERVICE_REGISTRY;

    static {
        SCHEDULER_SERVICE_REGISTRY = SchedulerServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link SchedulerService}.
     */
    public static SchedulerService get() {
        final SchedulerService service = SCHEDULER_SERVICE_REGISTRY.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}
