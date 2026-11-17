package com.tlc.crm.messenger.campaign.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.language.Language;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.models.ViewType;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.core.campaign.fields.CampaignFields;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.messenger.campaign.model.WACampaignTemplateStatus;
import com.tlc.crm.messenger.campaign.model.WATemplateFilterPreference;
import com.tlc.social.common.whatsapp.fields.WAFields;
import com.tlc.social.common.whatsapp.model.template.WATemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * <p>
 * Converts {@link WATemplateFilterPreference} to and from API representations.
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplateFilterPreferenceConverter extends EntityConverter<WATemplateFilterPreference>  {

    private WATemplateFilterPreferenceConverter() {
    }

    private static final class InstanceHolder {
        private static final WATemplateFilterPreferenceConverter INSTANCE = new WATemplateFilterPreferenceConverter();
    }

    /**
     * <p>
     * Gets the instance of {@link WATemplateFilterPreferenceConverter}.
     * </p>
     *
     * @return {@link WATemplateFilterPreferenceConverter}.
     */
    public static WATemplateFilterPreferenceConverter getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}.
     * @param data            Represents the {@link JsonObject}.
     * @return the converted filter preference.
     */
    @Override
    public WATemplateFilterPreference convert(final ConverterConfig converterConfig, final JsonObject data) {
        final WATemplateFilterPreference preference = new WATemplateFilterPreference();
        final JsonObject criteriaJson = data.optJsonObject(CampaignFields.CRITERIA);

        preference.setOrgId(converterConfig.orgId());
        preference.setUserId(converterConfig.ownerId());
        preference.setBusinessPhoneNumberId(data.getLong(CampaignFields.BUSINESS_PHONE_NUMBER_ID));

        if (data.containsKey(CampaignFields.VIEW)) {
            preference.setViewType(ViewType.get(data.getShort(CampaignFields.VIEW)));
        }

        if (!Json.isEmpty(criteriaJson)) {
            if (criteriaJson.containsKey(CampaignFields.CATEGORY)) {
                final Collection<WATemplate.Category> categories = new HashSet<>();

                criteriaJson.getJsonArray(CampaignFields.CATEGORY).forEach(categoryId -> {
                    categories.add(WATemplate.Category.getById(NumberUtils.parseNumber(categoryId).shortValue()));
                });

                preference.setCategories(categories);
            }

            if (criteriaJson.containsKey(CampaignFields.STATUS)) {
                final Collection<WACampaignTemplateStatus> statuses = new HashSet<>();

                criteriaJson.getJsonArray(CampaignFields.STATUS).forEach(statusId -> {
                    statuses.add(WACampaignTemplateStatus.get(NumberUtils.parseNumber(statusId).shortValue()));
                });

                preference.setStatuses(statuses);
            }

            if (criteriaJson.containsKey(WAFields.LANGUAGE)) {
                final Collection<Language> languages = new HashSet<>();

                criteriaJson.getJsonArray(WAFields.LANGUAGE).forEach(languageId -> {
                    languages.add(Language.getById(NumberUtils.parseNumber(languageId).shortValue()));
                });

                preference.setLanguages(languages);
            }
        }

        return preference;
    }

    /**
     * {@inheritDoc}
     *
     * @param converterConfig    Represents the {@link ConverterConfig}.
     * @param existingPreference Represents the existing filter preference.
     * @return the converted filter preference.
     */
    @Override
    public JsonObject convert(final ConverterConfig converterConfig, final WATemplateFilterPreference existingPreference) {
        final JsonObject dataJson = Json.object();
        final JsonObject criteriaJson = Json.object();
        final WATemplateFilterPreference preference = Objects.isNull(existingPreference)
                ? WATemplateFilterPreference.getDefaultPreference(converterConfig.orgId(), converterConfig.ownerId())
                : existingPreference;
        final Collection<WATemplate.Category> categories = preference.getCategories();
        final Collection<WACampaignTemplateStatus> statuses = preference.getStatuses();
        final Collection<Language> languages = preference.getLanguages();
        final ViewType viewType = preference.getViewType();
        final Long businessPhoneNumberId = preference.getBusinessPhoneNumberId();

        if (Objects.nonNull(categories) && !categories.isEmpty()) {
            criteriaJson.put(CampaignFields.CATEGORY, categories.stream().map(WATemplate.Category::getId).toList());
        }

        if (Objects.nonNull(statuses) && !statuses.isEmpty()) {
            criteriaJson.put(CampaignFields.STATUS, statuses.stream().map(WACampaignTemplateStatus::getId).toList());
        }

        if (Objects.nonNull(languages) && !languages.isEmpty()) {
            criteriaJson.put(WAFields.LANGUAGE, languages.stream().map(Language::getId).toList());
        }

        if (!Json.isEmpty(criteriaJson)) {
            dataJson.put(CampaignFields.CRITERIA, criteriaJson);
        }

        if (Objects.nonNull(viewType)) {
            dataJson.put(CampaignFields.VIEW, viewType.getId());
        }

        if (Objects.nonNull(businessPhoneNumberId)) {
            dataJson.put(CampaignFields.BUSINESS_PHONE_NUMBER_ID, businessPhoneNumberId);
        }

        return dataJson;
    }
}