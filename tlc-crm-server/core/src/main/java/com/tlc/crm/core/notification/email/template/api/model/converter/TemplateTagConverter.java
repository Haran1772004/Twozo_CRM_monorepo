package com.tlc.crm.core.notification.email.template.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.core.notification.email.template.api.TemplateTagManager;
import com.tlc.crm.core.notification.email.template.fields.TemplateTagFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.crm.core.notification.email.template.api.model.TemplateTag;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link TemplateTag}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class TemplateTagConverter extends EntityConverter<TemplateTag> {

    private final CrmUserConverter crmUserConverter;
    private final TemplateTagManager templateTagManager;
    private final DateTimeUtil dateTimeUtil;

    private TemplateTagConverter() {
        templateTagManager = TemplateTagManager.getInstance();
        crmUserConverter = CrmUserConverter.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    private static class Instance {
        private static final TemplateTagConverter INSTANCE = new TemplateTagConverter();
    }

    /**
     * Gets the {@link TemplateTagConverter} Instance
     *
     * @return {@link TemplateTagConverter}
     */
    public static TemplateTagConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Represents the request data
     * @return The {@link TemplateTag}
     */
    @Override
    public TemplateTag convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? buildTemplateTagByPartialData(converterConfig, jsonObject) :
                buildTemplateTag(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     *
     * @param templateTag Represents the {@link TemplateTag} attributes
     * @return The {@link JsonObject}
     */
    @Override
    public JsonObject convert(final TemplateTag templateTag) {
        final JsonObject jsonObject = Json.object();
        final DateTime lastUsedTime = templateTag.getLastUsedTime();
        final CrmUser currentUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = currentUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();

        jsonObject.put(TemplateTagFields.ID.getName(), templateTag.id());
        jsonObject.put(TemplateTagFields.NAME.getName(), templateTag.getName());
        jsonObject.put(TemplateTagFields.ORG_ID.getName(), templateTag.orgId());
        jsonObject.put(TemplateTagFields.CREATED_TIME.getName(), dateTimeUtil.getTimeStamp(templateTag.getCreatedTime()));
        jsonObject.put(TemplateTagFields.UPDATED_TIME.getName(), dateTimeUtil.getTimeStamp(templateTag.getUpdatedTime()));
        jsonObject.put(TemplateTagFields.LAST_USED_TIME.getName(), Objects.isNull(lastUsedTime) ? TemplateTagFields.NA.getName() :
                TemporalServiceProvider.get(lastUsedTime.getTimeStamp()).convertTimeStampToRelativeTime(timezoneInfo));
        final CrmUser crmUser = templateTag.getCreatedBy();

        if (Objects.nonNull(crmUser)) {
            jsonObject.put(TemplateTagFields.CREATED_BY.getName(), crmUserConverter.convert(crmUser, true, false));
        }

        return jsonObject;
    }

    /**
     * <p>
     * Converts partial {@link TemplateTag} into {@link JsonObject}
     * </p>
     *
     * @param templateTag Represents the {@link TemplateTag} attributes
     * @return The {@link JsonObject}
     */
    public JsonObject convertPartial(final TemplateTag templateTag) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(TemplateTagFields.ID.getName(), templateTag.id());
        jsonObject.put(TemplateTagFields.NAME.getName(), templateTag.getName());

        return jsonObject;
    }

    /**
     * <p>
     * Converts json attributes into {@link TemplateTag}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Represents the request data
     * @return The {@link TemplateTag}
     */
    private TemplateTag buildTemplateTag(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final TemplateTag templateTag = new TemplateTag();
        final String idKey = TemplateTagFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            templateTag.setId(jsonObject.getLong(idKey));
        }
        setJsonDataToModel(converterConfig, templateTag, jsonObject);

        return templateTag;
    }

    /**
     * <p>
     * Converts partial data into {@link TemplateTag}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param jsonObject      Represents the request data
     * @return The {@link TemplateTag}
     */
    private TemplateTag buildTemplateTagByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final TemplateTag templateTag = templateTagManager.get(converterConfig.orgId(), jsonObject.getLong(TemplateTagFields.ID.getName()));

        setJsonDataToModel(converterConfig, templateTag, jsonObject);

        return templateTag;
    }

    /**
     * <p>
     * Sets JSON data into {@link TemplateTag}
     * </p>
     *
     * @param converterConfig Converter configuration settings
     * @param templateTag     Represents the {@link TemplateTag} attributes
     * @param jsonObject      Holds the {@link TemplateTag} attributes
     */
    private void setJsonDataToModel(final ConverterConfig converterConfig, final TemplateTag templateTag, final JsonObject jsonObject) {
        final String nameKey = TemplateTagFields.NAME.getName();

        templateTag.setOrgId(converterConfig.orgId());
        templateTag.setOwnerId(converterConfig.ownerId());
        templateTag.setName(jsonObject.containsKey(nameKey) ? jsonObject.getString(nameKey).trim() : templateTag.getName().trim());
    }
}
