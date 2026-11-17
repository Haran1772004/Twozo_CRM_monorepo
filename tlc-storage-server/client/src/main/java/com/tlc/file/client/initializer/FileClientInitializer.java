package com.tlc.file.client.initializer;

import com.tlc.commons.service.initializer.ServiceInitializer;
import com.tlc.commons.util.Configurations;

import com.tlc.file.client.service.AttachmentStorageService;
import com.tlc.file.client.internal.async.AttachmentDeletionController;
import com.tlc.file.common.internal.AttachmentPolicyRegistry;

/**
 * Initializes the file client module services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FileClientInitializer implements ServiceInitializer {

    private final AttachmentStorageService attachmentService;
    private final AttachmentDeletionController attachmentDeletionController;
    private final AttachmentPolicyRegistry attachmentPolicyRegistry;

    private FileClientInitializer() {
        this.attachmentService = AttachmentStorageService.getInstance();
        this.attachmentDeletionController = new AttachmentDeletionController();
        this.attachmentPolicyRegistry = AttachmentPolicyRegistry.getInstance();
    }

    private static final class Instance {
        private static final FileClientInitializer FILE_SERVICE_INITIALIZER = new FileClientInitializer();
    }

    public static FileClientInitializer getInstance() {
        return Instance.FILE_SERVICE_INITIALIZER;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void preInitialize(final Configurations configurations) {
        attachmentService.start(configurations);
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Holds the configuration properties.
     */
    @Override
    public void postInitialize(final Configurations configurations) {
        attachmentDeletionController.start(null);
        attachmentPolicyRegistry.loadPolicies(configurations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        attachmentService.stop();
        attachmentDeletionController.stop();
        attachmentPolicyRegistry.clear();
    }
}
