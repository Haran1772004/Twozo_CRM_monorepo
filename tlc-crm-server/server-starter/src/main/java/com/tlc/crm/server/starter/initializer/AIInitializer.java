package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of AI services.
 *
 * @author Sathish
 * @version 1.0
 */
@Component
final class AIInitializer extends AbstractModuleInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void postInitialize() {
        LOGGER.info("Application Initializer: AI module is initialized successfully");
    }
}
