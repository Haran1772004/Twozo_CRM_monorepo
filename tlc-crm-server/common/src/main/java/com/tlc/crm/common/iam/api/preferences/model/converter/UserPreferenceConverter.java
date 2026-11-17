package com.tlc.crm.common.iam.api.preferences.model.converter;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.preferences.model.UserPreference;
import com.tlc.crm.common.iam.api.preferences.model.converter.impl.UserPreferenceConverterImpl;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link UserPreferenceHolder}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface UserPreferenceConverter extends PreferenceConverter<UserPreference, UserPreferenceHolder> {

    static UserPreferenceConverter getInstance() {
        return UserPreferenceConverterImpl.getInstance();
    }

    /**
     * <p>
     * Converts the json data into {@link UserPreferenceHolder}.
     * </p>
     *
     * @param orgId      Represents an organization id.
     * @param userId     Represents an user id.
     * @param jsonObject Holds the preferences details as {@link JsonObject}.
     * @return {@link UserPreferenceHolder}.
     */
    UserPreferenceHolder convert(final Long orgId, final Long userId, final JsonObject jsonObject);
}
