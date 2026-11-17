package com.tlc.file.starter;

import com.tlc.components.starter.ApplicationStarter;

import com.tlc.file.starter.initializer.FileServiceInitializer;
import com.tlc.file.starter.initializer.StorageModules;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Serves as the entry point for activating the File service application by extending {@link ApplicationStarter}, ensuring
 * that the component services are loaded first before initiating the file startup process.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
class FileServiceActivator extends ApplicationStarter {

    @Autowired
    private FileServiceInitializer fileServiceInitializer;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void register() {
        moduleRegistry.register(List.of(StorageModules.values()));
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during pre-run initialization.
     */
    @Override
    protected void init() throws Exception {
        fileServiceInitializer.initialize();
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during post-run tasks.
     */
    @Override
    protected void postInit() throws Exception {
        fileServiceInitializer.postInitialize();
    }

}