package com.tlc.crm.server.starter.initializer;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.crm.server.starter.fields.ServerStarterFields;
import com.tlc.crm.webform.initializer.WebFormServiceInitializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import org.springframework.stereotype.Component;

/**
 * Responsible for initiating the initialization process of webForm services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class WebFormInitializer extends AbstractModuleInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ServerStarterFields.WEBFORM);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (isServiceEnabled()) {
            WebFormServiceInitializer.getInstance().stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            WebFormServiceInitializer.getInstance().preInitialize(getProperties(ServerStarterFields.WEBFORM));
        }
    }
}
