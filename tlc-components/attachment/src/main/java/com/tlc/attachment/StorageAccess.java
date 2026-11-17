package com.tlc.attachment;

import com.tlc.attachment.internal.registry.StorageServiceRegistry;
import com.tlc.attachment.service.StorageService;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.ConfHolder;

import com.tlc.attachment.storage.internal.StorageServiceImpl;

import com.tlc.commons.util.Configurations;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activates the bundle and provides the storage access.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class StorageAccess implements BundleActivator {

    private final ServiceRegistry<StorageService> storageServiceRegistry;

    public StorageAccess() {
        storageServiceRegistry = StorageServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final BundleContext context) {
        final StorageService storageService = new StorageServiceImpl();
        final Configurations configurations = ConfHolder.getInstance().getProperties("storage");

        storageService.start(configurations);
        storageServiceRegistry.register(storageService);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(final BundleContext context) throws Exception {
        final StorageService storageService = storageServiceRegistry.unregister();

        storageService.stop();
    }
}