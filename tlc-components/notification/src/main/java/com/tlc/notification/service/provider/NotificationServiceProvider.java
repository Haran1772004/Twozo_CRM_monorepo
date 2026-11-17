package com.tlc.notification.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.notification.internal.registry.NotificationsServiceRegistry;
import com.tlc.notification.service.NotificationService;

import java.util.Optional;

/**
 * Provides the services that enable the {@link NotificationService} to execute its functions effectively.
 *
 * @author Sathish
 * @version 1.0
 */
public final class NotificationServiceProvider {

    private static final ServiceRegistry<NotificationService> NOTIFICATION_SERVICE_REGISTRY;

    static {
        NOTIFICATION_SERVICE_REGISTRY = NotificationsServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The instance of {@link NotificationService}.
     */
    public static NotificationService get() {
        return Optional.ofNullable(NOTIFICATION_SERVICE_REGISTRY.get())
                .orElseThrow(() -> ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized"));
    }
}
