package com.tlc.crm.server.starter.initializer;

import com.tlc.crm.icon.initializer.IconServiceInitializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of icon services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class IconInitializer extends AbstractModuleInitializer {

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
        IconServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        IconServiceInitializer.getInstance().preInitialize(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postInitialize() throws Exception {
        IconServiceInitializer.getInstance().postInitialize(null);
    }
}
