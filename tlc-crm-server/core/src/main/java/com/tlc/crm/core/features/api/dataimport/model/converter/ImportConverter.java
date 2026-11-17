package com.tlc.crm.core.features.api.dataimport.model.converter;

import com.tlc.attachment.io.File;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo.State;
import com.tlc.crm.core.features.api.dataimport.model.ImportInfo.Status;
import com.tlc.crm.core.features.field.dataimport.ImportFields;
import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.crm.entity.attachment.fields.AttachmentFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.file.common.models.Attachment;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;
import com.tlc.iam.common.models.User;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Manages the conversion of JSON data into {@link ImportInfo} and {@link ImportInfo} into {@link JsonObject}
 *
 * @author sundar
 * @version 1.0
 */
public final class ImportConverter extends EntityConverter<ImportInfo> {

    private final AttachmentConverter attachmentConverter;
    private final I18nService i18nService;
    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final ImportConverter INSTANCE = new ImportConverter();
    }

    private ImportConverter() {
        this.attachmentConverter = AttachmentConverter.getInstance();
        this.i18nService = I18nServiceProvider.get();
        this.dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * Gets the {@link ImportConverter} Instance.
     *
     * @return the {@link ImportConverter}.
     */
    public static ImportConverter getInstance() {
        return ImportConverter.Instance.INSTANCE;
    }

    /**
     * Converts the {@link JsonObject} into {@link ImportInfo}.
     *
     * @param converterConfig Converter configuration.
     * @param jsonObject      Which contains the data.
     * @return {@link ImportInfo} entity.
     */
    @Override
    public ImportInfo convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final ImportInfo importInfo = new ImportInfo();
        final JsonArray attachmentArray = jsonObject.optJsonArray(AttachmentFields.ATTACHMENTS.getName());
        final String idKey = ImportFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            importInfo.setId(jsonObject.getLong(idKey));
        } else {
            importInfo.setStatus(ImportInfo.Status.PENDING);
        }

        importInfo.setOrgId(converterConfig.orgId());
        importInfo.setUserId(converterConfig.ownerId());

        return importInfo;
    }

    /**
     * {@inheritDoc}
     *
     * @param importInfo Which contains the data import information to be converted
     * @return {@link JsonObject} that contains the converted data
     */
    @Override
    public JsonObject convert(final ImportInfo importInfo) {
        final JsonObject importJson = Json.object();

        importJson.put(ImportFields.ID.getName(), importInfo.id());
        importJson.put(ImportFields.ORG_ID.getName(), importInfo.orgId());
        importJson.put(ImportFields.USER_ID.getName(), importInfo.getUserId());
        importJson.put(ImportFields.STATUS.getName(), importInfo.getStatus().name());
        importJson.put(ImportFields.ATTACHMENT.getName(), attachmentConverter.convert(importInfo.getAttachment()));

        return importJson;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the converter configuration
     * @param importInfo      Represents the entity model
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final ImportInfo importInfo) {
        final JsonObject importJson = Json.object();

        importJson.put(ImportFields.ID.getName(), importInfo.id());
        importJson.put(ImportFields.ORG_ID.getName(), importInfo.orgId());
        importJson.put(ImportFields.USER_ID.getName(), importInfo.getUserId());
        importJson.put(ImportFields.STATUS.getName(), i18nService.getResolver(converterConfig.locale()).get(importInfo.getStatus().i18nKey()));
        importJson.put(ImportFields.ATTACHMENT.getName(), attachmentConverter.convert(importInfo.getAttachment()));

        return importJson;
    }

    /**
     * Converts the {@link ImportInfo} to {@link JsonObject}.
     *
     * @param converterConfig Represents the converter configuration.
     * @param importInfo      Represents the {@link ImportInfo} model.
     * @param user            Represents the user to be included
     * @return {@link JsonObject}.
     */
    public JsonObject convertJsonForHistory(final ConverterConfig converterConfig, final ImportInfo importInfo, final User user) {
        final JsonObject importJson = Json.object();
        final Status importStatus = importInfo.getStatus();
        final Status pending = Status.PENDING;
        final boolean isStatusInProgress = Status.IN_PROGRESS == importStatus;

        importJson.put(ImportFields.ID.getName(), importInfo.id());
        importJson.put(ImportFields.ORG_ID.getName(), importInfo.orgId());
        importJson.put(ImportFields.STATUS.getName(), i18nService.getResolver(converterConfig.locale()).get(importStatus.i18nKey()));
        importJson.put(ImportFields.ATTACHMENT.getName(), attachmentConverter.convert(importInfo.getAttachment()));
        importJson.put(ImportFields.CREATED_TIME.getName(), dateTimeUtil.getTimeStamp(importInfo.getCreatedTime()));

        if (Objects.nonNull(user)) {
            final JsonObject userJson = Json.object();

            userJson.put(ImportFields.ID.getName(), importInfo.getUserId());
            userJson.put(ImportFields.NAME.getName(), user.getFullName());

            importJson.put(ImportFields.USER.getName(), userJson);
        }

        if (Status.FINISHED == importStatus) {
            final JsonArray fieldMapJsonArray = importInfo.getFieldsMap();
            final DateTime modifiedTime = importInfo.getModifiedTime();

            if (Objects.nonNull(modifiedTime) && TimeUnit.HOURS.toMillis(48) >= (Instant.now().toEpochMilli() - modifiedTime.getTimeStamp())) {
                importJson.put(ImportFields.REVERT_ALLOWED.getName(), true);
            }

            if (Objects.nonNull(fieldMapJsonArray)) {
                final String sourceKey = ImportFields.SOURCE.getName();
                final Collection<String> sources = new HashSet<>();

                for (int index = 0; index < fieldMapJsonArray.size() && sources.size() < Source.values().length; index++) {
                    final JsonObject fieldMapJson = fieldMapJsonArray.getJsonObject(index);

                    if (fieldMapJson.containsKey(sourceKey)) {
                        sources.add(fieldMapJson.getString(sourceKey));
                    }
                }

                if (!sources.isEmpty()) {
                    final JsonArray sourceJsonArray = Json.array();

                    sources.forEach(sourceJsonArray::put);
                    importJson.put(ImportFields.SOURCES.getName(), sourceJsonArray);
                }
            }
        }

        if (pending == importStatus) {
            importJson.put(ImportFields.STATE.getName(),
                    Objects.isNull(importInfo.getFieldsMap()) ? State.UPLOADED : State.FIELDS_MAPPED);
        } else if (isStatusInProgress) {
            importJson.put(ImportFields.STATE.getName(), State.IMPORT_INITIALIZED);
        }

        return importJson;
    }
}
