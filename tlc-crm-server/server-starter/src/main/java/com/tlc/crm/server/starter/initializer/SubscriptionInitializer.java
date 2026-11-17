package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.crm.plan.and.billing.initializer.SubscriptionServiceInitializer;

import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of subscription services.
 *
 * @author Sathish
 * @version 1.0
 */
@Component
final class SubscriptionInitializer extends AbstractModuleInitializer {

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
        SubscriptionServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        SubscriptionServiceInitializer.getInstance().preInitialize(null);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void postInitialize() {
        SubscriptionServiceInitializer.getInstance().postInitialize(null);
        LOGGER.info("Application Initializer: Plans and Billings module is initialized successfully");
    }
}
