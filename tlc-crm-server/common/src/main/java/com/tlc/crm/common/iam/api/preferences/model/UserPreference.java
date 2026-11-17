package com.tlc.crm.common.iam.api.preferences.model;

import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;

import java.io.Serializable;

/**
 * <p>
 * Provides the services according to the user preference.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface UserPreference extends Preference<UserPreferenceType>, Serializable {

    /**
     * <p>
     * Provides the user id.
     * </p>
     *
     * @return User id.
     */
    Long userId();
}
