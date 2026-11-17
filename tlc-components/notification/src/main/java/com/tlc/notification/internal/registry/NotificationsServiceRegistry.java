package com.tlc.notification.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.notification.service.NotificationService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link NotificationService} into the registry.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class NotificationsServiceRegistry implements ServiceRegistry<NotificationService> {

    private final AtomicReference<NotificationService> notificationService;

    private NotificationsServiceRegistry() {
        this.notificationService = new AtomicReference<>();
    }

    private static final class Instance {
        private static final NotificationsServiceRegistry NOTIFICATION_SERVICE_REGISTRY = new NotificationsServiceRegistry();
    }

    public static ServiceRegistry<NotificationService> getInstance() {
        return Instance.NOTIFICATION_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final NotificationService service) {
        if (!notificationService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Notification service already initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public NotificationService get() {
        return notificationService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link NotificationService}.
     */
    @Override
    public NotificationService unregister() {
        return notificationService.getAndSet(null);
    }
}