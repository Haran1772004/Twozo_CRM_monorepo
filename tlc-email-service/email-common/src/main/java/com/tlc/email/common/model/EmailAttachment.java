package com.tlc.email.common.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.InputStream;

/**
 * <p>
 * A model which stores the attachment related details.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class EmailAttachment implements EmailModel {

    @NotNull(message = "i18n.notification.email.attachment.invalid.id")
    private Long id;

    @NotNull(message = "i18n.notification.email.attachment.invalid.name")
    @Size(max = 150, message = "i18n.notification.email.attachment.name.max.length")
    private String fileName;

    @NotNull(message = "i18n.notification.email.attachment.invalid.mediaType")
    @Size(max = 100, message = "i18n.notification.email.attachment.mediaType.max")
    private String mediaType;

    @NotNull(message = "i18n.notification.email.attachment.invalid.size")
    private Long fileSize;

    @NotNull(message = "i18n.notification.email.attachment.invalid.extension")
    private String extension;
    private Long createdTime;
    private Long updatedTime;

    @NotNull(message = "i18n.notification.email.attachment.invalid.inputStream")
    private InputStream inputStream;
    private String internalId;

    public void setId(final Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(final String mediaType) {
        this.mediaType = mediaType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(final Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final Long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public Long id() {
        return id;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }
}
