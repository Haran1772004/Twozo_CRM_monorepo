package com.tlc.file.client.converter;

import com.tlc.attachment.io.File;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.file.common.models.Attachment;
import com.tlc.file.common.fields.AttachmentAttributes;

import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Collection;

/**
 * Provides different ways to convert the data from / into {@link Attachment}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AttachmentConverter {

    private AttachmentConverter() {
    }

    private static class Instance {
        private static final AttachmentConverter ATTACHMENT_CONVERTER = new AttachmentConverter();
    }

    /**
     * Gets the {@link AttachmentConverter} Instance.
     *
     * @return the {@link AttachmentConverter}.
     */
    public static AttachmentConverter getInstance() {
        return Instance.ATTACHMENT_CONVERTER;
    }

    /**
     * Converts the {@link JsonObject} into {@link Attachment}.
     *
     * @param jsonObject Which contains the data.
     * @return {@link Attachment} entity.
     */
    public Attachment convert(final JsonObject jsonObject) {
        final Attachment attachment = new Attachment();

        if (jsonObject.containsKey(AttachmentAttributes.ID)) {
            attachment.setId(jsonObject.getLong(AttachmentAttributes.ID));
        }
        if (jsonObject.containsKey(AttachmentAttributes.UPLOADED_TIME)) {
            attachment.setCreatedTime(new DateTime(jsonObject.getLong(AttachmentAttributes.UPLOADED_TIME)));
        }
        attachment.setDescription(jsonObject.containsKey(AttachmentAttributes.DESCRIPTION)
                ? jsonObject.getString(AttachmentAttributes.DESCRIPTION) : attachment.getDescription());
        attachment.setFileName(jsonObject.getString(AttachmentAttributes.FILE_NAME));
        attachment.setFileSize(jsonObject.getLong(AttachmentAttributes.FILE_SIZE));
        attachment.setExtension(jsonObject.getString(AttachmentAttributes.EXTENSION));
        attachment.setMediaType(jsonObject.getString(AttachmentAttributes.MEDIA_TYPE));
        attachment.setStorageType(Attachment.StorageType.OBJECT);

        return attachment;
    }

    /**
     * Converts the {@link JsonObject} into {@link Attachment}.
     *
     * @param jsonObject Which contains the data.
     * @return {@link Attachment} entity.
     */
    public Attachment convert(final Attachment attachment, final JsonObject jsonObject) {
        setJsonDataToModel(attachment, jsonObject);

        return attachment;
    }

    /**
     * Sets the json data into the {@link Attachment}.
     *
     * @param attachment To be filled with data.
     * @param jsonObject Which contains the data.
     */
    private void setJsonDataToModel(final Attachment attachment, final JsonObject jsonObject) {
        if (jsonObject.containsKey(AttachmentAttributes.ID)) {
            attachment.setId(jsonObject.getLong(AttachmentAttributes.ID));
        }
        final File file = (File) jsonObject.get(AttachmentAttributes.FILE);

        attachment.setFileName(file.name());
        attachment.setFileSize(file.size());
        attachment.setExtension(file.extension());
        attachment.setMediaType(file.mediaType());
        attachment.setInputStream(file.inputStream());
    }

    /**
     * Converts the {@link Attachment} into the {@link JsonObject}.
     *
     * @param attachment Which contains the data.
     * @return {@link JsonObject} filled with data.
     */
    public JsonObject convert(final Attachment attachment) {
        final DateTimeUtil dateTimeUtil = DateTimeUtil.getInstance();
        final JsonObject attachmentJson = Json.object();

        attachmentJson.put(AttachmentAttributes.ID, attachment.id());
        attachmentJson.put(AttachmentAttributes.DESCRIPTION, attachment.getDescription());
        attachmentJson.put(AttachmentAttributes.FILE_NAME, attachment.getFileName());
        attachmentJson.put(AttachmentAttributes.FILE_SIZE, attachment.getFileSize());
        attachmentJson.put(AttachmentAttributes.EXTENSION, attachment.getExtension());
        attachmentJson.put(AttachmentAttributes.MEDIA_TYPE, attachment.getMediaType());
        attachmentJson.put(AttachmentAttributes.UPLOADED_TIME, dateTimeUtil.getTimeStamp(attachment.getCreatedTime()));
        attachmentJson.put(AttachmentAttributes.UPDATED_TIME, dateTimeUtil.getTimeStamp(attachment.getUpdatedTime()));

        return attachmentJson;
    }

    /**
     * Converts the {@link Attachment} into the {@link JsonObject}.
     *
     * @param attachments Refers the attachments to be converted.
     * @return {@link JsonObject} filled with data.
     */
    public JsonArray convert(final Collection<Attachment> attachments) {
        final JsonArray attachmentArray = Json.array();

        for (final Attachment attachment : attachments) {
            attachmentArray.put(convert(attachment));
        }
        return attachmentArray;
    }

}