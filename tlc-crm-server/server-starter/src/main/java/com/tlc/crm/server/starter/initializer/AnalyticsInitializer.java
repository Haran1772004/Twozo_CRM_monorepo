package com.tlc.crm.server.starter.initializer;

import com.tlc.analytics.initializer.AnalyticsServiceInitializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of analytics services.
 *
 * @author Pandiarajkumar G
 * @version 1.0
 */
@Component
final class AnalyticsInitializer extends AbstractModuleInitializer {

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
        AnalyticsServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        AnalyticsServiceInitializer.getInstance().preInitialize( null );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        AnalyticsServiceInitializer.getInstance().postInitialize(null);
        LOGGER.info("Application Initializer: Analytics module is initialized successfully");
    }
}
