package com.tlc.file.common.models;

/**
 * Represents the {@link FileAttachment}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FileAttachment extends Attachment {

    public FileAttachment() {
        super(StorageType.FILE);
    }

    private String alternateName;

    public String getAlternateName() {
        return alternateName;
    }

    public void setAlternateName(final String alternateName) {
        this.alternateName = alternateName;
    }

}
