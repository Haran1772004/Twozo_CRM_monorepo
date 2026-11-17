package com.tlc.crm.entity.features.api.tag.model.converter;

import com.tlc.commons.color.Color;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.converter.OptionJsonConverter;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.features.api.tag.manager.TagManager;
import com.tlc.crm.entity.features.api.tag.model.Tag;
import com.tlc.crm.entity.features.field.tag.TagFields;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Provides different ways to convert the data from / into {@link Tag}
 *
 * @author EswariNivethaVU
 * @version 1.0
 */
public class TagConverter extends EntityConverter<Tag> implements OptionJsonConverter<Tag> {

    private final TagManager tagManager;
    private final CrmUserConverter userConverter;
    private final UserService userService;
    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final TagConverter INSTANCE = new TagConverter();
    }

    private TagConverter() {
        tagManager = TagManager.getInstance();
        userConverter = CrmUserConverter.getInstance();
        userService = UserService.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * Gets instance of Tag Converter
     *
     * @return {@link Instance of Tag Converter}
     */
    public static TagConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Converts {@link JsonObject} into {@link Tag}
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the tag data
     * @return the {@link Tag}
     */
    @Override
    public Tag convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildTagByPartialData(converterConfig, jsonObject);
        } else {
            return buildTag(converterConfig, jsonObject);
        }
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link Tag}
     * </p>
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the tag data
     * @return the {@link Tag}
     */
    private Tag buildTag(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Tag tag = new Tag();
        final String valueKey = TagFields.VALUE.getName();
        final String idKey = TagFields.ID.getName();

        if (jsonObject.containsKey(valueKey)) {
            tag.setId(jsonObject.getLong(valueKey));
        } else if (jsonObject.containsKey(idKey)) {
            tag.setId(jsonObject.getLong(idKey));
        }
        setDataToTag(converterConfig, tag, jsonObject);

        return tag;
    }

    /**
     * <p>
     * Converts the partial data present in  {@link JsonObject} into {@link Tag}
     * </p>
     *
     * @param converterConfig converter configuration
     * @param jsonObject      which contains the tag data
     * @return the {@link Tag}
     */
    private Tag buildTagByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Tag tag = tagManager.get(converterConfig.orgId(), jsonObject.getLong(TagFields.ID.getName()));

        setDataToTag(converterConfig, tag, jsonObject);

        return tag;
    }

    /**
     * <p>
     * Sets the json data into {@link Tag}
     * </p>
     *
     * @param converterConfig converter configuration
     * @param tag             which contains the tag data
     * @param jsonObject      which contains the tag fields data
     */
    private void setDataToTag(final ConverterConfig converterConfig, final Tag tag, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final CrmUser crmUser = userService.getUser(orgId, converterConfig.ownerId());
        final String colorKey = TagFields.COLOR.getName();

        tag.setOrgId(orgId);
        tag.setName(jsonObject.optString(TagFields.TAG.getName(), null));
        tag.setCreatedBy(crmUser);

        if (jsonObject.containsKey(colorKey)) {
            tag.setColor(jsonObject.getString(colorKey));
        } else if (Objects.isNull(tag.id()) && Objects.isNull(tag.getColor())) {
            tag.setColor(Tag.getDefaultColor());
        }
    }

    /**
     * <p>
     * Converts {@link Tag} into {@link JsonObject}
     * </p>
     *
     * @param tag A model that holds the attributes of tag
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convert(final Tag tag) {
        final JsonObject jsonObject = Json.object();
        final CrmUser crmUser = tag.getCreatedBy();
        final Long lastUsedTime = dateTimeUtil.getTimeStamp(tag.getLastUsed());
        final CrmUser currentUser = LoggedInUserInfo.getUser();
        final UserRegionalPreference userRegionalPreference = currentUser.getPreferenceHolder().get(UserRegionalPreference.TYPE);
        final TimezoneInfo timezoneInfo = userRegionalPreference.getTimezoneInfo();
        final Color color = Objects.isNull(tag.getColor()) ? Tag.getDefaultColor() : tag.getColor();

        jsonObject.put(TagFields.ID.getName(), tag.id());
        jsonObject.put(TagFields.TAG.getName(), tag.getName());
        jsonObject.put(TagFields.ORG_ID.getName(), tag.orgId());
        jsonObject.put(TagFields.CREATED_TIME.getName(), dateTimeUtil.getTimeStamp(tag.getCreatedTime()));
        jsonObject.put(TagFields.UPDATED_TIME.getName(), dateTimeUtil.getTimeStamp(tag.getUpdatedTime()));
        jsonObject.put(TagFields.CREATED_BY.getName(), Objects.isNull(crmUser) ? null : userConverter.convert(crmUser, true, false));
        jsonObject.put(TagFields.LAST_USED_TIME.getName(), Objects.isNull(lastUsedTime) ? TagFields.NA.getName() :
                TemporalServiceProvider.get(lastUsedTime).convertTimeStampToRelativeTime(timezoneInfo));
        jsonObject.put(TagFields.COLOR.getName(), color.getName());
        return jsonObject;
    }

    /**
     * Converts the {@link Tag} into {@link JsonArray}
     *
     * @param tags Which holds the {@link Collection} of {@link Tag}
     * @return {@link JsonArray} of {@link Tag} details
     */
    public JsonArray convertTagsForSummary(final Collection<Tag> tags) {
        final JsonArray tagArray = Json.array();

        for (final Tag tag : tags) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(TagFields.ID.getName(), tag.id());
            jsonObject.put(TagFields.TAG.getName(), tag.getName());
            jsonObject.put(TagFields.COLOR.getName(), tag.getColor().name());

            tagArray.put(jsonObject);
        }

        return tagArray;
    }

    /**
     * Converts the {@link Tag} into {@link JsonArray}
     *
     * @param tags Which holds the {@link Collection} of {@link Tag}
     * @return {@link JsonArray} of {@link Tag} details
     */
    public JsonArray convertTags(final Collection<Tag> tags) {
        final JsonArray tagArray = Json.array();

        for (final Tag tag : tags) {
            final JsonObject jsonObject = Json.object();

            jsonObject.put(TagFields.ID.getName(), tag.id());

            tagArray.put(jsonObject);
        }

        return tagArray;
    }

    /**
     * <p>
     * Builds the {@link Tag} entity from the given json objects and map it to source
     * </p>
     *
     * @param fieldId         Represents the field id as String to obtain the tag values from the Json
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param jsonObject      Holds the field values of tag
     */
    public Collection<Tag> buildTags(final String fieldId, final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final JsonArray jsonArray = jsonObject.optJsonArray(fieldId);
        final Collection<Tag> tags = new ArrayList<>();

        if (Objects.nonNull(jsonArray)) {

            for (int index = 0; index < jsonArray.size(); index++) {
                tags.add(buildTag(converterConfig, jsonArray.getJsonObject(index)));
            }
        }

        return tags;
    }

    /**
     * <p>
     * Converts a collection of {@link Tag} objects into a {@link JsonArray}.
     * </p>
     *
     * @param tags            A {@link Collection} of {@link Tag} entities to be converted.
     * with each element representing a {@link Tag}.
     */
    public JsonArray convertModelToJson(final Collection<Tag> tags) {
        final JsonArray tagArray = Json.array();

        for (final Tag tag : tags) {
            tagArray.put(convertModelToOptionJson(tag));
        }

        return tagArray;
    }

    /**
     * Converts {@link Tag} into {@link JsonObject}
     *
     * @param tag The tag entity with the attributes of tag details.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToOptionJson(final Tag tag) {
        final JsonObject jsonObject = Json.object();
        final Color color = Objects.isNull(tag.getColor()) ? Tag.getDefaultColor() : tag.getColor();

        jsonObject.put(CrmUserFields.VALUE.getName(), tag.id());
        jsonObject.put(CrmUserFields.NAME.getName(), tag.getName());
        jsonObject.put(TagFields.COLOR.getName(), color.getName());

        return jsonObject;
    }
}
