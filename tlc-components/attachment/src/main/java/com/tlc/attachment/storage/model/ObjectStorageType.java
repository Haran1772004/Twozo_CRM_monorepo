package com.tlc.attachment.storage.model;

/**
 * An enum which represents the different types of object storage services available.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ObjectStorageType {

    /**
     * MINIO object storage.
     */
    MINIO((short) 1),

    /**
     * Amazon object storage.
     */
    AMAZON_S3((short) 2),

    /**
     * Azure Blob storage.
     */
    AZURE_BLOB((short) 3),
    ;

    private final short id;

    ObjectStorageType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }
}
