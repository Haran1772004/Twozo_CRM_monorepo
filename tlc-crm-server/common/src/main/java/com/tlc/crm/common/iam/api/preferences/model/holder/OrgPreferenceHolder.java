package com.tlc.crm.common.iam.api.preferences.model.holder;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.preferences.model.OrgPreference;
import com.tlc.crm.common.iam.api.preferences.model.Preference;
import com.tlc.crm.common.iam.api.preferences.model.type.OrgPreferenceType;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import java.util.Objects;

/**
 * <p>
 * Holds the organisation {@link Preference} details.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class OrgPreferenceHolder extends PreferenceHolder<OrgPreferenceType, OrgPreference> {

    /**
     * {@inheritDoc}
     *
     * @param <C>   The target type that extends {@link PreferenceHolder}
     * @param clazz The {@code Class} object of the target type to which the migration is intended
     * @return An instance of the specified type {@code C} after migration
     */
    @Override
    public <C extends PreferenceHolder<?, ?>> C migrateTo(final Class<C> clazz) {

        if (Objects.equals(clazz, UserPreferenceHolder.class)) {
            final UserPreferenceHolder userPreferenceHolder = new UserPreferenceHolder();
            final JsonObject preferenceJson = toJson(false);
            final String orgRegionalPreferenceKey = OrgPreferenceType.ORG_REGIONAL_PREFERENCE.type();
            final JsonObject orgRegionalPreference = preferenceJson.getJsonObject(orgRegionalPreferenceKey);
            final String userRegionalPreferenceKey = UserPreferenceType.USER_REGIONAL_PREFERENCE.type();

            preferenceJson.put(userRegionalPreferenceKey, orgRegionalPreference);
            userPreferenceHolder.setId(id());
            userPreferenceHolder.setOrgId(orgId());

            preferenceJson.remove(orgRegionalPreferenceKey);
            userPreferenceHolder.addPreferencesAsJson(preferenceJson);

            return (C) userPreferenceHolder;
        } else {
            throw ErrorCode.get(CommonErrorCodes.UNSUPPORTED_PREFERENCE_MIGRATION, "Unsupported preference migration");
        }
    }
}