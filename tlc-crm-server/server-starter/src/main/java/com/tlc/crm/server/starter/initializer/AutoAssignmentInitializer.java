package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.crm.auto.assignment.initializer.AutoAssignmentServiceInitializer;
import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of auto-assignment module.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
public class AutoAssignmentInitializer extends AbstractModuleInitializer {

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
        AutoAssignmentServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        AutoAssignmentServiceInitializer.getInstance().postInitialize(getProperties("auto-assignment"));
        LOGGER.info("Application Initializer: Auto-Assignment module is initialized successfully");
    }
}