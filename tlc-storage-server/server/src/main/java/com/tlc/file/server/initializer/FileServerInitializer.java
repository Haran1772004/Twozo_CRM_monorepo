package com.tlc.file.server.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.file.common.action.RequestServiceImpl;
import com.tlc.file.server.internal.async.PreSignedURLCleaner;

/**
 * An initializer that initializes the service which needs to be started.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FileServerInitializer implements ServiceInitializer {

    private final PreSignedURLCleaner preSignedURLCleaner;
    private final RequestServiceImpl requestService;

    private FileServerInitializer() {
        this.preSignedURLCleaner = PreSignedURLCleaner.getInstance();
        this.requestService = new RequestServiceImpl();
    }

    private static final class Instance {
        private static final FileServerInitializer FILE_SERVICE_INITIALIZER = new FileServerInitializer();
    }

    public static FileServerInitializer getInstance() {
        return Instance.FILE_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        requestService.start(configurations);
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        preSignedURLCleaner.start(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        preSignedURLCleaner.stop();
        requestService.stop();
    }
}
