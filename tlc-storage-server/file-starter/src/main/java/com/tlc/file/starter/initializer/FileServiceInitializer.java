package com.tlc.file.starter.initializer;

import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.file.client.initializer.FileClientInitializer;
import com.tlc.file.server.initializer.FileServerInitializer;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import org.springframework.stereotype.Component;

/**
 * Responsible for triggering the initialization process of the file module within the application.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
public class FileServiceInitializer extends AbstractModuleInitializer {

    private static final String FILE_SERVICE = "file-service";
    private static final String FILE_VALIDATION_POLICY = "file-validation-policy";

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(FILE_SERVICE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (isServiceEnabled()) {
            FileClientInitializer.getInstance().stop();
            FileServerInitializer.getInstance().stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {

        if (isServiceEnabled()) {
            final Configurations configurations = getProperties(FILE_SERVICE);

            FileClientInitializer.getInstance().preInitialize(configurations);
            FileServerInitializer.getInstance().preInitialize(configurations);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        if (isServiceEnabled()) {
            FileClientInitializer.getInstance().postInitialize(getProperties(FILE_VALIDATION_POLICY));
            FileServerInitializer.getInstance().postInitialize(null);
        }
    }
}
