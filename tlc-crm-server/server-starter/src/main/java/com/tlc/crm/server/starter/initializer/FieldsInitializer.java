package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.crm.fields.service.FieldsServiceInitializer;
import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of entity services.
 *
 * @author KarthickV
 * @version 1.0
 */
@Component
public class FieldsInitializer extends AbstractModuleInitializer  {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return true;
    }

    @Override
    public void initialize() {
        FieldsServiceInitializer.getInstance().preInitialize(null);
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during the post-initialization process.
     */
    @Override
    public void postInitialize() {
        FieldsServiceInitializer.getInstance().postInitialize(null);
        LOGGER.info("Application Initializer: Fields module is initialized successfully");
    }

    @Override
    public void shutdown() {
        FieldsServiceInitializer.getInstance().stop();
    }
}
