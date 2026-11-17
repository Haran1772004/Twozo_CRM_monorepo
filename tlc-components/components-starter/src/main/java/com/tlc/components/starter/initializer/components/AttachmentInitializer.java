package com.tlc.components.starter.initializer.components;

import com.tlc.attachment.initializer.AttachmentModuleInitializer;
import com.tlc.attachment.internal.registry.StorageServiceRegistry;
import com.tlc.attachment.service.StorageService;
import com.tlc.attachment.storage.internal.StorageServiceImpl;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import org.springframework.stereotype.Component;

/**
 * Responsible for triggering the initialization process of the attachment services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class AttachmentInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<StorageService> storageServiceRegistry;

    private AttachmentInitializer() {
        this.storageServiceRegistry = StorageServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.STORAGE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return storageServiceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final StorageService storageService = new StorageServiceImpl();
            final Configurations configurations = getProperties(ComponentsStarterFields.STORAGE);

            storageService.start(configurations);
            storageServiceRegistry.register(storageService);
            AttachmentModuleInitializer.getInstance().preInitialize(configurations);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postInitialize() throws Exception {
        final Configurations configurations = getProperties(ComponentsStarterFields.STORAGE);

        AttachmentModuleInitializer.getInstance().postInitialize(configurations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        AttachmentModuleInitializer.getInstance().stop();
        super.shutdown();
    }
}
