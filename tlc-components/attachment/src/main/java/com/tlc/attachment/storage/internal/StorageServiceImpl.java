package com.tlc.attachment.storage.internal;

import com.tlc.attachment.service.FileStorageService;
import com.tlc.attachment.service.OrgFileStorageService;
import com.tlc.attachment.service.ObjectStorageService;
import com.tlc.attachment.storage.internal.config.AzureBlobStorageConfig;
import com.tlc.attachment.storage.internal.config.S3StorageConfig;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.attachment.service.StorageService;

import com.tlc.commons.util.Env;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

/**
 * Provides the storage services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class StorageServiceImpl implements StorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageServiceImpl.class);
    private ObjectStorageService objectStorageService;
    private OrgFileStorageService orgFileStorageService;
    private FileStorageService fileStorageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        try {
            this.objectStorageService = getServiceByClient(configurations);
            this.fileStorageService = FileStorageServiceImpl.getInstance();
            this.orgFileStorageService = OrgFileStorageServiceImpl.getInstance();

            ((FileStorageServiceImpl) fileStorageService).setFileDirectoryPath(Objects.requireNonNullElseGet(configurations.optConfig("file.path", null),
                    () -> String.format("%s%s%s", Env.getWorkDirectory(), File.separator, "uploads")));
        } catch (Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.STORAGE_SERVICE_INITIALIZATION_FAILED, exception.getMessage());
        }
        LOGGER.info("Storage service started");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        LOGGER.info("Storage service stopped");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ObjectStorageService getObjectStorageService() {
        return objectStorageService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OrgFileStorageService getOrgFileStorageService() {
        return orgFileStorageService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileStorageService getFileStorageService() {
        return fileStorageService;
    }

    /**
     * Returns the appropriate {@code ObjectStorageService} instance
     * based on the provided client configurations.
     *
     * @param configurations The configuration object containing client-specific settings.
     * @return The matching {@code ObjectStorageService}.
     */
    private ObjectStorageService getServiceByClient(final Configurations configurations) {
        final String client = configurations.optConfig("client", "");

        return switch (client) {
            case "azure" -> new AzureBlobStorageServiceImpl(new AzureBlobStorageConfig(configurations.getNestedConfigs(client)));
            case "aws" -> new S3StorageServiceImpl(new S3StorageConfig(configurations.getNestedConfigs(client)));
            default -> throw ErrorCode.get(StorageErrorCodes.STORAGE_SERVICE_INITIALIZATION_FAILED);
        };
    }
}
