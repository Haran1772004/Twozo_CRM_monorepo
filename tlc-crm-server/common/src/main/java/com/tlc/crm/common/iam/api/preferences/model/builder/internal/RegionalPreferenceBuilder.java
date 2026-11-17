package com.tlc.crm.common.iam.api.preferences.model.builder.internal;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.preferences.model.Preference;
import com.tlc.crm.common.iam.api.preferences.model.RegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.builder.PreferenceBuilder;
import com.tlc.crm.common.iam.api.preferences.model.type.PreferenceType;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;
import com.tlc.crm.common.models.Language;

import com.tlc.l10n.temporal.model.TimezoneInfo;

/**
 * <p>
 * Manages the building process of the {@link RegionalPreference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public abstract class RegionalPreferenceBuilder<T extends PreferenceType, P extends RegionalPreference<T>> implements PreferenceBuilder<P> {

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Holds the details of the preference.
     * @return {@link Preference}.
     */
    @Override
    public P build(final JsonObject jsonObject) {
        final P regionalPreference = getRegionalPreference();

        regionalPreference.setTimezoneInfo(TimezoneInfo.getTimezoneById(jsonObject.getInt(PreferenceFields.TIME_ZONE.getName())));
        regionalPreference.setLanguage(Language.getLanguageById(jsonObject.getInt(PreferenceFields.LANGUAGE.getName())));
        regionalPreference.setDateFormat(RegionalPreference.DateFormat.get(jsonObject.getShort(PreferenceFields.DATE_FORMAT.getName())));
        regionalPreference.setTimeFormat(RegionalPreference.TimeFormat.get(jsonObject.getShort(PreferenceFields.TIME_FORMAT.getName())));

        return regionalPreference;
    }

    /**
     * {@inheritDoc}
     *
     * @param preference Holds the details of the preference.
     * @return {@link JsonObject}.
     */
    @Override
    public JsonObject build(final P preference) {
        final JsonObject preferenceJson = Json.object();

        preferenceJson.put(PreferenceFields.TIME_ZONE.getName(), preference.getTimezoneInfo().getId());
        preferenceJson.put(PreferenceFields.LANGUAGE.getName(), preference.getLanguage().getId());
        preferenceJson.put(PreferenceFields.DATE_FORMAT.getName(), preference.getDateFormat().getId());
        preferenceJson.put(PreferenceFields.TIME_FORMAT.getName(), preference.getTimeFormat().getId());

        return preferenceJson;
    }

    /**
     * <p>
     * Fetches the {@link RegionalPreference} instance.
     * </p>
     *
     * @return {@link RegionalPreference} instance.
     */
    protected abstract P getRegionalPreference();
}
