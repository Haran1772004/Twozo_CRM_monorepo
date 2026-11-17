package com.tlc.crm.common.iam.api.preferences.model.type;

import java.io.Serializable;

/**
 * <p>
 * Defines the type of the user preference.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum UserPreferenceType implements PreferenceType, Serializable {

    USER_REGIONAL_PREFERENCE("userRegionalPreference"),
    USER_NOTIFICATION_PREFERENCE("userNotificationPreference"),
    USER_DEAL_PREFERENCE("userDealPreference"),
    USER_CONTACT_PREFERENCE("userContactPreference"),
    USER_COMPANY_PREFERENCE("userCompanyPreference"),
    ;

    private final String type;

    UserPreferenceType(final String type) {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String type() {
        return type;
    }
}
