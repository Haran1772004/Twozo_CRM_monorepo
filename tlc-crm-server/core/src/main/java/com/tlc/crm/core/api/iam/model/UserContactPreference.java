package com.tlc.crm.core.api.iam.model;

import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;

/**
 * <p>
 * Model represents the {@link UserContactPreference} details
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class UserContactPreference extends UserEntityPreference {

    public static final UserPreferenceType TYPE = UserPreferenceType.USER_CONTACT_PREFERENCE;

    /**
     * {@inheritDoc}
     *
     * @return the {@link UserPreferenceType}
     */
    @Override
    public UserPreferenceType getType() {
        return TYPE;
    }
}
