package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.crm.email.notification.initializer.EmailNotificationServiceInitializer;

import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * <p>
 * Provides the services for email notification service initializer.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractModuleInitializer
 */
@Component
public class EmailNotificationInitializer extends AbstractModuleInitializer {

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
        EmailNotificationServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        EmailNotificationServiceInitializer.getInstance().preInitialize(null);
    }

    @Override
    public void postInitialize() {
        EmailNotificationServiceInitializer.getInstance().postInitialize(null);
        LOGGER.info("Application Initializer: Email-notification module is initialized successfully");
    }
}
