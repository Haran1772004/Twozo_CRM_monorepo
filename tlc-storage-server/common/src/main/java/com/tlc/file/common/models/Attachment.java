package com.tlc.file.common.models;

import com.tlc.commons.code.ErrorCode;

import com.tlc.file.common.validator.Group.Upload;
import com.tlc.file.common.status.FileErrorCodes;

import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * A Model which represents the Attachment details.
 *
 * @author ThalaimalaiPandiyanT
 */
public class Attachment implements Attachable {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.attachment.invalid.id")
    private Long id;

    @Size(max = 140, groups = {Update.class}, message = "i18n.attachment.description.max.length")
    private String description;

    @NotNull(groups = {Upload.class}, message = "i18n.attachment.invalid.name")
    @Size(max = 255, groups = {Upload.class}, message = "i18n.attachment.name.max.length")
    private String fileName;

    @NotNull(groups = {Upload.class}, message = "i18n.attachment.invalid.mediaType")
    @Size(max = 100, groups = {Upload.class}, message = "i18n.attachment.mediaType.max")
    private String mediaType;

    @NotNull(groups = {Upload.class}, message = "i18n.attachment.invalid.size")
    private Long fileSize;// TODO max file size

    @NotNull(groups = {Upload.class}, message = "i18n.attachment.invalid.extension")
    private String extension;

    @NotNull(groups = {Upload.class}, message = "i18n.attachment.invalid.inputStream")
    private transient InputStream inputStream;

    private StorageType storageType;
    private Supplier<InputStream> inputStreamSupplier;

    private DateTime createdTime;
    private DateTime updatedTime;

    public Attachment() {
    }

    public Attachment(final StorageType storageType) {
        this.storageType = storageType;
    }

    /**
     * Represents the types of storage services available.
     */
    public enum StorageType {

        FILE((short) 1),
        OBJECT((short) 2);

        private final short id;

        StorageType(final short id) {
            this.id = id;
        }

        public short getId() {
            return id;
        }

        /**
         * Gets the {@link StorageType} by its id.
         *
         * @param id Refers the storageType id.
         * @return {@link StorageType}.
         */
        public static StorageType getById(final short id) {
            return switch (id) {
                case 1 -> FILE;
                case 2 -> OBJECT;
                default ->
                        throw ErrorCode.get(FileErrorCodes.INVALID_STORAGE_TYPE, "i18n.attachment.storage.type.invalid");
            };
        }
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(final String extension) {
        this.extension = extension;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public DateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(final DateTime createdTime) {
        this.createdTime = createdTime;
    }

    public DateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(final DateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(final StorageType storageType) {
        this.storageType = storageType;
    }

    public InputStream getInputStream() {

        if (inputStreamSupplier != null) {
            return inputStreamSupplier.get();
        } else {
            return inputStream;
        }
    }

    public void setInputStream(final InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setInputStreamSupplier(final Supplier<InputStream> inputStreamSupplier) {
        this.inputStreamSupplier = inputStreamSupplier;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return null;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * Returns the current instance cast to the specified type.
     *
     * @param <T> The type to cast the current instance to.
     * @return The current instance cast as the specified type.
     */
    public <T> T getAsType() {
        return (T) this;
    }

}