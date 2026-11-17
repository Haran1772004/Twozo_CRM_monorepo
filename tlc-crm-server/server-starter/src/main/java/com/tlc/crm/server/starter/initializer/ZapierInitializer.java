package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.crm.zapier.initializer.ZapierServiceInitializer;

import org.springframework.stereotype.Component;

/**
 * Responsible for initiating the initialization process of zapier module.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class ZapierInitializer extends AbstractModuleInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        ZapierServiceInitializer.getInstance().preInitialize(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        ZapierServiceInitializer.getInstance().postInitialize(getProperties("zapier"));
        LOGGER.info("Application Initializer: Zapier module is initialized successfully");
    }

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
        ZapierServiceInitializer.getInstance().stop();
    }
}
