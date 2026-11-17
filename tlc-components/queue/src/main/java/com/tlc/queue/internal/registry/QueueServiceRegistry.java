package com.tlc.queue.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.test.QueueTestInitializer;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link QueueService} into the registry.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class QueueServiceRegistry implements ServiceRegistry<QueueService> {

    private final AtomicReference<QueueService> queueService;

    private static final class Instance {
        private static final QueueServiceRegistry QUEUE_SERVICE_REGISTRY = new QueueServiceRegistry();
    }

    private QueueServiceRegistry() {
        this.queueService = new AtomicReference<>();
    }

    public static ServiceRegistry<QueueService> getInstance() {
        return Instance.QUEUE_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final QueueService service) {
        if (!queueService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
        final Configurations configurations = ConfHolder.getInstance().getProperties("queue");
        final boolean testRequire = Boolean.parseBoolean(configurations.optConfig("testRequire", "false"));

        if (testRequire) {
            final QueueTestInitializer queueTestInitializer = new QueueTestInitializer();

            queueTestInitializer.start(configurations);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public QueueService get() {
        return queueService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link QueueService}.
     */
    @Override
    public QueueService unregister() {
        return queueService.getAndSet(null);
    }
}
