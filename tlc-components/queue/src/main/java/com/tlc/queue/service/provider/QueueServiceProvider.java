package com.tlc.queue.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.queue.internal.registry.QueueServiceRegistry;
import com.tlc.queue.service.QueueService;

import java.util.Objects;

/**
 * Provides the services that enable the {@link QueueService} to execute its functions effectively.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class QueueServiceProvider {

    private static final ServiceRegistry<QueueService> QUEUE_SERVICE_REGISTRY;

    static {
        QUEUE_SERVICE_REGISTRY = QueueServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link QueueService}.
     */
    public static QueueService get() {
        final QueueService service = QUEUE_SERVICE_REGISTRY.get();

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Queue service not initialized");
        } else {
            return service;
        }
    }
}
