package com.tlc.crm.core.features.api.file.model.converter;

import com.tlc.attachment.io.File;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.features.api.file.model.FileHolder;
import com.tlc.crm.core.features.field.file.FileHolderFields;
import com.tlc.crm.entity.attachment.api.models.EntityAttachment;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;

import com.tlc.file.common.models.Attachment;
import com.tlc.crm.entity.attachment.fields.AttachmentFields;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link FileHolder}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class FileHolderConverter extends EntityConverter<FileHolder> {

    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final FileHolderConverter INSTANCE = new FileHolderConverter();
    }

    private FileHolderConverter() {
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * Gets the {@link FileHolderConverter} Instance.
     *
     * @return the {@link FileHolderConverter}.
     */
    public static FileHolderConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts the {@link JsonObject} into {@link FileHolder}.
     *
     * @param converterConfig Converter configuration.
     * @param jsonObject      Which contains the data.
     * @return {@link FileHolder} file entity.
     */
    @Override
    public FileHolder convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return constructFileHolderDetails(converterConfig, jsonObject);
    }

    /**
     * Converts the {@link JsonObject} into {@link Attachment}.
     *
     * @param converterConfig Converter configuration.
     * @param jsonObject      Which contains the data.
     * @return File upload entity.
     */
    private FileHolder constructFileHolderDetails(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final FileHolder fileHolder = new FileHolder();

        setJsonDataToModel(converterConfig, fileHolder, jsonObject);
        return fileHolder;
    }

    /**
     * Sets the json data into the {@link FileHolder}.
     *
     * @param converterConfig Converter configuration.
     * @param fileHolder      File attributes.
     * @param jsonObject      Which contains the data.
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final FileHolder fileHolder,
                                    final JsonObject jsonObject) {
        final String entityIdKey = FileHolderFields.ENTITY_ID.getName();
        final String sourceKey = FileHolderFields.SOURCE.getName();

        fileHolder.setOrgId(converterConfig.orgId());
        fileHolder.setUserId(converterConfig.ownerId());
        fileHolder.setEntityId(jsonObject.containsKey(entityIdKey) ? jsonObject.getLong(entityIdKey) : null);
        fileHolder.setSource(jsonObject.containsKey(sourceKey) ? jsonObject.getString(sourceKey) : fileHolder.getSource());
    }

    /**
     * Converts the {@link EntityAttachment} into the {@link JsonObject}.
     *
     * @param entityAttachment Refers the {@link EntityAttachment}.
     * @return The json object of entity attachment details.
     */
    public final JsonObject convert(final EntityAttachment entityAttachment) {
        final JsonObject attachmentJson = Json.object();
        final CrmUser owner = entityAttachment.getOwner();

        attachmentJson.put(AttachmentFields.ID.getName(), entityAttachment.id());
        attachmentJson.put(AttachmentFields.ORG_ID.getName(), entityAttachment.orgId());
        attachmentJson.put(AttachmentFields.DESCRIPTION.getName(), entityAttachment.getDescription());
        attachmentJson.put(AttachmentFields.FILE_NAME.getName(), entityAttachment.getFileName());
        attachmentJson.put(AttachmentFields.FILE_SIZE.getName(), entityAttachment.getFileSize());
        attachmentJson.put(AttachmentFields.EXTENSION.getName(), entityAttachment.getExtension());
        attachmentJson.put(AttachmentFields.MEDIA_TYPE.getName(), entityAttachment.getMediaType());
        attachmentJson.put(AttachmentFields.UPLOADED_TIME.getName(), dateTimeUtil.getTimeStamp(entityAttachment.getCreatedTime()));
        attachmentJson.put(AttachmentFields.UPDATED_TIME.getName(), dateTimeUtil.getTimeStamp(entityAttachment.getUpdatedTime()));

        if (Objects.nonNull(owner)) {
            attachmentJson.put(AttachmentFields.OWNER_NAME.getName(), owner.getFullName());
        }

        return attachmentJson;
    }

    /**
     * Converts the {@link FileHolder} into the {@link JsonObject}.
     *
     * @param fileHolder Which contains the data.
     * @return {@link JsonObject} filled with data.
     */
    @Override
    public JsonObject convert(final FileHolder fileHolder) {
        return null;
    }

}