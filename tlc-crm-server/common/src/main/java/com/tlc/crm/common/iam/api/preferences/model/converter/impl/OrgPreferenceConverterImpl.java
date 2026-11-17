package com.tlc.crm.common.iam.api.preferences.model.converter.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.preferences.manager.OrgPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.OrgPreference;
import com.tlc.crm.common.iam.api.preferences.model.RegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.builder.internal.store.PreferenceBuilderStore;
import com.tlc.crm.common.iam.api.preferences.model.converter.OrgPreferenceConverter;
import com.tlc.crm.common.iam.api.preferences.model.holder.OrgPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.OrgRegionalPreference;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;
import com.tlc.crm.common.models.Language;

import com.tlc.l10n.temporal.model.TimezoneInfo;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link OrgPreference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class OrgPreferenceConverterImpl implements OrgPreferenceConverter {

    private final OrgPreferenceManager preferenceManager;
    private final PreferenceBuilderStore builderStore;

    private OrgPreferenceConverterImpl() {
        preferenceManager = OrgPreferenceManager.getInstance();
        builderStore = PreferenceBuilderStore.getInstance();
    }

    private static final class Instance {
        private static final OrgPreferenceConverterImpl PREFERENCE_CONVERTER = new OrgPreferenceConverterImpl();
    }

    public static OrgPreferenceConverterImpl getInstance() {
        return Instance.PREFERENCE_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id.
     * @param jsonObject Holds the preferences details as {@link JsonObject}.
     */
    @Override
    public OrgPreferenceHolder convert(final Long orgId, final JsonObject jsonObject) {
        final OrgPreferenceHolder preferenceHolder = preferenceManager.get(orgId);
        final OrgRegionalPreference orgRegionalPreference = preferenceHolder.get(OrgRegionalPreference.TYPE);
        final String languageKey = PreferenceFields.LANGUAGE.getName();
        final String timezoneKey = PreferenceFields.TIME_ZONE.getName();
        final String dateFormatKey = PreferenceFields.DATE_FORMAT.getName();
        final String timeFormatKey = PreferenceFields.TIME_FORMAT.getName();

        if (jsonObject.containsKey(languageKey)) {
            orgRegionalPreference.setLanguage(Language.getLanguageById(jsonObject.getInt(languageKey)));
        }

        if (jsonObject.containsKey(timezoneKey)) {
            orgRegionalPreference.setTimezoneInfo(TimezoneInfo.getTimezoneById(jsonObject.getInt(timezoneKey)));
        }

        if (jsonObject.containsKey(dateFormatKey)) {
            orgRegionalPreference.setDateFormat(RegionalPreference.DateFormat.get(jsonObject.getShort(dateFormatKey)));
        }

        if (jsonObject.containsKey(timeFormatKey)) {
            orgRegionalPreference.setTimeFormat(RegionalPreference.TimeFormat.get(jsonObject.getShort(timeFormatKey)));
        }

        return preferenceHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @param preference Represents the details of the preference.
     * @return The preferences as {@link JsonObject}.
     */
    @Override
    public JsonObject convertToJson(final OrgPreference preference) {
        return builderStore.get(preference.getType()).build(preference);
    }

    /**
     * {@inheritDoc}
     *
     * @param preferenceHolder Represents the details of the preferenceHolder.
     * @return The preferences as {@link JsonObject}.
     */
    @Override
    public JsonObject convertToJson(final OrgPreferenceHolder preferenceHolder) {
        return preferenceHolder.toJson(true);
    }
}
