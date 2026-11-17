package com.tlc.crm.common.iam.api.preferences.model.converter.impl;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.preferences.manager.UserPreferenceManager;
import com.tlc.crm.common.iam.api.preferences.model.RegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.builder.internal.store.PreferenceBuilderStore;
import com.tlc.crm.common.iam.api.preferences.model.converter.UserPreferenceConverter;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;
import com.tlc.crm.common.models.Language;

import com.tlc.l10n.temporal.model.TimezoneInfo;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link UserPreference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class UserPreferenceConverterImpl implements UserPreferenceConverter {

    private final UserPreferenceManager preferenceManager;
    private final PreferenceBuilderStore builderStore;

    private UserPreferenceConverterImpl() {
        preferenceManager = UserPreferenceManager.getInstance();
        builderStore = PreferenceBuilderStore.getInstance();
    }

    private static final class Instance {
        private static final UserPreferenceConverterImpl PREFERENCE_CONVERTER = new UserPreferenceConverterImpl();
    }

    public static UserPreferenceConverterImpl getInstance() {
        return Instance.PREFERENCE_CONVERTER;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId      Represents an organization id.
     * @param userId     Represents an user id.
     * @param jsonObject Holds the preferences details as {@link JsonObject}.
     */
    @Override
    public UserPreferenceHolder convert(final Long orgId, final Long userId, final JsonObject jsonObject) {
        final UserPreferenceHolder preferenceHolder = preferenceManager.get(orgId, userId);
        final UserRegionalPreference userRegionalPreference = preferenceHolder.get(UserRegionalPreference.TYPE);
        final String languageKey = PreferenceFields.LANGUAGE.getName();
        final String timezoneKey = PreferenceFields.TIME_ZONE.getName();
        final String dateFormatKey = PreferenceFields.DATE_FORMAT.getName();
        final String timeFormatKey = PreferenceFields.TIME_FORMAT.getName();

        if (jsonObject.containsKey(languageKey)) {
            userRegionalPreference.setLanguage(Language.getLanguageById(jsonObject.getInt(languageKey)));
        }

        if (jsonObject.containsKey(timezoneKey)) {
            userRegionalPreference.setTimezoneInfo(TimezoneInfo.getTimezoneById(jsonObject.getInt(timezoneKey)));
        }

        if (jsonObject.containsKey(dateFormatKey)) {
            userRegionalPreference.setDateFormat(RegionalPreference.DateFormat.get(jsonObject.getShort(dateFormatKey)));
        }

        if (jsonObject.containsKey(timeFormatKey)) {
            userRegionalPreference.setTimeFormat(RegionalPreference.TimeFormat.get(jsonObject.getShort(timeFormatKey)));
        }
        return preferenceHolder;
    }

    /**
     * {@inheritDoc}
     *
     * @param preference Represents the details of the preference.
     * @return The preference as {@link JsonObject}.
     */
    @Override
    public JsonObject convertToJson(final UserPreference preference) {
        return builderStore.get(preference.getType()).build(preference);
    }

    /**
     * {@inheritDoc}
     *
     * @param preferenceHolder Represents the details of the preferenceHolder.
     * @return The preferences as {@link JsonObject}.
     */
    @Override
    public JsonObject convertToJson(final UserPreferenceHolder preferenceHolder) {
        return preferenceHolder.toJson(true);
    }
}
