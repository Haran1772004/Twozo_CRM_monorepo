package com.tlc.crm.core.api.iam.model.builder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.preferences.model.builder.PreferenceBuilder;
import com.tlc.crm.common.iam.field.preferences.PreferenceFields;
import com.tlc.crm.core.api.iam.model.UserEntityPreference;

/**
 * <p>
 * Manages the building process of the {@link UserEntityPreference}
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public abstract class UserEntityPreferenceBuilder<T extends UserEntityPreference> implements PreferenceBuilder<T> {

    /**
     * {@inheritDoc}
     *
     * @param jsonObject Holds the details of the preference.
     * @return The {@link UserEntityPreference}
     */
    @Override
    public T build(final JsonObject jsonObject) {
        final T userEntityPreference = fetchUserEntityPreference();

        userEntityPreference.setDuplicateCheckEnabled(jsonObject.getBoolean(PreferenceFields.IS_DUPLICATE_CHECK_ENABLED.getName()));
        return userEntityPreference;
    }

    /**
     * {@inheritDoc}
     *
     * @param userEntityPreference Holds the details of the preference.
     * @return The preference {@link JsonObject}
     */
    @Override
    public JsonObject build(final UserEntityPreference userEntityPreference) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(PreferenceFields.IS_DUPLICATE_CHECK_ENABLED.getName(), userEntityPreference.isDuplicateCheckEnabled());
        return jsonObject;
    }

    /**
     * <p>
     * Fetches the {@link UserEntityPreference}
     * </p>
     *
     * @return The {@link UserEntityPreference}
     */
    abstract T fetchUserEntityPreference();
}
