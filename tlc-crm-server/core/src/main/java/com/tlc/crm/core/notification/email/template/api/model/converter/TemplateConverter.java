package com.tlc.crm.core.notification.email.template.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.core.notification.email.template.api.TemplateManager;
import com.tlc.crm.core.notification.email.template.fields.TemplateFields;
import com.tlc.file.common.models.Attachment;
import com.tlc.file.client.converter.AttachmentConverter;
import com.tlc.crm.entity.attachment.fields.AttachmentFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.notification.email.template.api.model.EntityMailTemplate;
import com.tlc.crm.core.notification.email.template.api.model.TemplateMetrics;
import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;

import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link EntityMailTemplate}
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public class TemplateConverter extends EntityConverter<EntityMailTemplate> {

    private static final String ZER_PERCENTAGE = "0%";

    private final TemplateManager templateManager;
    private final AttachmentConverter attachmentConverter;
    private final TemplateTagConverter templateTagConverter;
    private final I18nService i18nService;

    private TemplateConverter() {
        templateManager = TemplateManager.getInstance();
        attachmentConverter = AttachmentConverter.getInstance();
        templateTagConverter = TemplateTagConverter.getInstance();
        i18nService = I18nServiceProvider.get();
    }

    private static class Instance {
        private static final TemplateConverter INSTANCE = new TemplateConverter();
    }

    /**
     * Gets the {@link TemplateConverter} Instance
     *
     * @return {@link TemplateConverter}
     */
    public static TemplateConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Represents the request data
     * @return The {@link EntityMailTemplate}
     */
    @Override
    public EntityMailTemplate convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? constructTemplatePartialData(converterConfig, jsonObject) :
                constructTemplate(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Converts the template into json object
     * </p>
     *
     * @param template An entity that holds template attributes
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final EntityMailTemplate template) {
        return convertTemplateToJson(converterConfig, template);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param template Represents the entity model
     * @return template json
     */
    @Override
    public JsonObject convert(final EntityMailTemplate template) {
        return convertTemplateToJson(null, template);
    }

    /**
     * <p>
     * Converts the {@link JsonObject} to {@link EntityMailTemplate}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Represents the request data
     * @return {@link EntityMailTemplate}
     */
    private EntityMailTemplate constructTemplatePartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Optional<EntityMailTemplate> optionalTemplate = templateManager.partialGet(converterConfig.orgId(), List.of(jsonObject.getLong(TemplateFields.ID.getName()))).stream().findFirst();
        final EntityMailTemplate template = optionalTemplate.orElseGet(() -> null);

        if (Objects.nonNull(template)) {
            setJsonDataToTemplate(converterConfig, template, jsonObject);
        }

        return template;
    }

    /**
     * <p>
     * Converts json attributes into {@link EntityMailTemplate}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Represents the request data
     * @return {@link EntityMailTemplate}
     */
    private EntityMailTemplate constructTemplate(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final EntityMailTemplate template = new EntityMailTemplate();
        final String idKey = TemplateFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            template.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToTemplate(converterConfig, template, jsonObject);

        return template;
    }

    /**
     * <p>
     * Sets JSON data into {@link EntityMailTemplate}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param template        An entity that holds template attributes
     * @param jsonObject      Represents the request data
     */
    private void setJsonDataToTemplate(final ConverterConfig converterConfig, final EntityMailTemplate template, final JsonObject jsonObject) {
        final String nameKey = TemplateFields.NAME.getName();
        final String contentKey = TemplateFields.CONTENT.getName();
        final String sourceKey = TemplateFields.SOURCE.getName();
        final String entityKey = TemplateFields.ENTITY_ID.getName();
        final String folderKey = TemplateFields.FOLDER_ID.getName();

        template.setOrgId(converterConfig.orgId());
        template.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey).trim() : template.getName());
        template.setSubject(jsonObject.optString(TemplateFields.SUBJECT.getName(), template.getSubject()));
        template.setContent(jsonObject.containsKey(contentKey) ? jsonObject.getString(contentKey) : template.getContent());
        template.setSource(jsonObject.containsKey(sourceKey) ? Source.get(jsonObject.getString(sourceKey)) : template.getSource());
        template.setEntityId(jsonObject.containsKey(entityKey) ? jsonObject.getLong(entityKey) : null);
        template.setFolderId(jsonObject.containsKey(folderKey) ? jsonObject.getLong(TemplateFields.FOLDER_ID.getName()) : null);

        if (jsonObject.containsKey(AttachmentFields.ATTACHMENT_IDS.getName())) {
            constructAttachmentDetails(template, jsonObject);
        }

        setJsonDataToTemplateTag(converterConfig.ownerId(), template, jsonObject);
    }

    /**
     * <p>
     * Set the Json object to {@link TemplateTag}
     * </p>
     *
     * @param ownerId    Represents the owner id
     * @param template   Represents the template attributes
     * @param jsonObject Represents the request data
     */
    private void setJsonDataToTemplateTag(final long ownerId, final EntityMailTemplate template, final JsonObject jsonObject) {
        final String tagsKey = TemplateFields.TAGS.getName();

        if (jsonObject.containsKey(tagsKey)) {
            final Collection<TemplateTag> templateTags = new HashSet<>();
            final JsonArray jsonArray = jsonObject.getJsonArray(tagsKey);
            final String idKey = TemplateFields.ID.getName();
            final String nameKey = TemplateFields.NAME.getName();

            for (int index = 0; index < jsonArray.size(); index++) {
                final TemplateTag templateTag = new TemplateTag();
                final JsonObject tagJson = jsonArray.getJsonObject(index);

                if (tagJson.containsKey(idKey)) {
                    templateTag.setId(tagJson.getLong(idKey));
                }

                if (tagJson.containsKey(nameKey)) {
                    templateTag.setName(tagJson.getString(nameKey));
                }
                templateTag.setOrgId(template.orgId());
                templateTag.setOwnerId(ownerId);
                templateTags.add(templateTag);
            }
            template.setTemplateTags(templateTags);
        }
    }

    /**
     * Sets {@link Attachment} details into {@link EntityMailTemplate}.
     *
     * @param template   Represents the templates.
     * @param jsonObject Which contains the data.
     */
    private void constructAttachmentDetails(final EntityMailTemplate template, final JsonObject jsonObject) {
        final JsonArray attachmentArray = jsonObject.getJsonArray(AttachmentFields.ATTACHMENT_IDS.getName());
        final Set<Long> attachmentIds = new HashSet<>();

        for (int index = 0; index < attachmentArray.size(); index++) {
            attachmentIds.add(attachmentArray.getLong(index));
        }

        template.setAttachmentIds(attachmentIds);
    }

    /**
     * <p>
     * Converts the {@link EntityMailTemplate} to {@link JsonObject}.
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param template        Represents the {@link EntityMailTemplate}
     * @return template {@link JsonObject}
     */
    private JsonObject convertTemplateToJson(final ConverterConfig converterConfig, final EntityMailTemplate template) {
        final JsonObject jsonObject = Json.object();
        final JsonArray jsonArray = Json.array();
        final JsonArray templateTagJson = Json.array();
        final Collection<Attachment> attachments = template.getAttachments();
        final String subject = template.getSubject();
        final String content = template.getContent();
        final Collection<TemplateTag> templateTags = template.getTemplateTags();
        final TemplateMetrics metrics = template.getTemplateMetrics();

        jsonObject.put(TemplateFields.ID.getName(), template.id());
        jsonObject.put(TemplateFields.NAME.getName(), template.getName());
        jsonObject.put(TemplateFields.CREATED_TIME.getName(), template.getCreatedTime());
        jsonObject.put(TemplateFields.SUBJECT.getName(), subject);
        jsonObject.put(TemplateFields.CONTENT.getName(), content);

        if (Objects.nonNull(attachments)) {
            attachments.forEach(attachment ->
                    jsonArray.put(attachmentConverter.convert(attachment).remove(AttachmentFields.ORG_ID.getName())));
            jsonObject.put(AttachmentFields.ATTACHMENTS.getName(), jsonArray);
        }

        if (Objects.nonNull(templateTags) && !templateTags.isEmpty()) {
            templateTags.forEach(templateTag -> templateTagJson.put(templateTagConverter.convertPartial(templateTag)));
            jsonObject.put(TemplateFields.TAGS.getName(), templateTagJson);
        }

        if (Objects.nonNull(metrics)) {
            final JsonObject statisticsJson = Json.object();
            final long sent = metrics.getSentCount();
            final long replied = metrics.getRepliedCount();
            final long unsubscribed = metrics.getUnsubscribedCount();

            statisticsJson.put(TemplateFields.SENT_COUNT.getName(), sent == 0 ? ZER_PERCENTAGE : sent);
            statisticsJson.put(TemplateFields.REPLIED_PERCENTAGE.getName(), replied == 0 ?
                    ZER_PERCENTAGE : metrics.calculateStatisticsInPercentage(replied, sent));
            statisticsJson.put(TemplateFields.UNSUBSCRIBED_PERCENTAGE.getName(), unsubscribed == 0 ?
                    ZER_PERCENTAGE : metrics.calculateStatisticsInPercentage(unsubscribed, sent));
            statisticsJson.put(TemplateFields.OPENED_PERCENTAGE.getName(), getTrackingPercentage(converterConfig, metrics.getOpenedCount(), sent, metrics));
            statisticsJson.put(TemplateFields.CLICKED_PERCENTAGE.getName(), getTrackingPercentage(converterConfig, metrics.getClickedCount(), sent, metrics));

            jsonObject.put(TemplateFields.STATISTICS.getName(), statisticsJson);
        }

        return jsonObject;
    }

    /**
     * <p>
     * Gets the percentage for tracking.
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param trackingCount   Represents the tracking count
     * @param sentCount       Represents the sent count
     * @param metrics         Represents the {@link TemplateMetrics}
     * @return the value of tracking percentage
     */
    private String getTrackingPercentage(final ConverterConfig converterConfig, final long trackingCount, final long sentCount, final TemplateMetrics metrics) {
        final String i18nKey = Objects.isNull(converterConfig) ? null : I18nServiceProvider.get()
                .getResolver(converterConfig.locale()).get("i18n.notifications.tracking.not.available");

        if (trackingCount == 0) {
            return Objects.isNull(converterConfig) ? ZER_PERCENTAGE : i18nKey;
        } else {
            return metrics.calculateStatisticsInPercentage(trackingCount, sentCount);
        }
    }
}