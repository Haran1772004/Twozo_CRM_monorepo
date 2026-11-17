package com.tlc.crm.core.api.iam.model;

import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;

/**
 * <p>
 * Model represents the {@link UserCompanyPreference} details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class UserCompanyPreference extends UserEntityPreference {

    public static final UserPreferenceType TYPE = UserPreferenceType.USER_COMPANY_PREFERENCE;

    @Override
    public UserPreferenceType getType() {
        return TYPE;
    }
}
