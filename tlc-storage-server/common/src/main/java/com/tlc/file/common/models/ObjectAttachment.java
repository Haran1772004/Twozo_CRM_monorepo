package com.tlc.file.common.models;

import com.tlc.attachment.storage.model.ObjectUploadResponse;

/**
 * Represents the {@link ObjectAttachment}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class ObjectAttachment extends Attachment {

    public ObjectAttachment() {
        super(StorageType.OBJECT);
    }

    private ObjectUploadResponse objectResponse;

    public ObjectUploadResponse getObjectResponse() {
        return objectResponse;
    }

    public void setObjectResponse(final ObjectUploadResponse objectResponse) {
        this.objectResponse = objectResponse;
    }

}