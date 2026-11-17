package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.crm.notifications.fields.NotificationAttributes;
import com.tlc.crm.notifications.initializer.NotificationServiceInitializer;
import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of notifications services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class NotificationsInitializer extends AbstractModuleInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        NotificationServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        NotificationServiceInitializer.getInstance().preInitialize(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        NotificationServiceInitializer.getInstance().postInitialize(getProperties(NotificationAttributes.NOTIFICATION));
        LOGGER.info("Application Initializer: Notifications module is initialized successfully");
    }
}