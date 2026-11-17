package com.tlc.crm.server.starter.initializer;//package com.tlc.spring.service.initiator.server;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.crm.server.starter.fields.ServerStarterFields;
import com.tlc.crm.workflow.initializer.WorkflowServiceInitializer;

import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of workflow services.
 *
 * @author ThalaimalaiPandiyanT
 */
@Component
final class WorkflowInitializer extends AbstractModuleInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ServerStarterFields.WORKFLOW_EXECUTOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (isServiceEnabled()) {
            WorkflowServiceInitializer.getInstance().stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            WorkflowServiceInitializer.getInstance().preInitialize(getProperties(ServerStarterFields.WORKFLOW_EXECUTOR));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        if (isServiceEnabled()) {
            WorkflowServiceInitializer.getInstance().postInitialize(getProperties(ServerStarterFields.WORKFLOW_EXECUTOR));
            LOGGER.info("Application Initializer: Workflow module is initialized successfully");
        }
    }
}
