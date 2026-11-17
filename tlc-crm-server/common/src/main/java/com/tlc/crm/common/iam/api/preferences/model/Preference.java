package com.tlc.crm.common.iam.api.preferences.model;

import com.tlc.crm.common.iam.api.preferences.model.type.PreferenceType;

import java.io.Serializable;

/**
 * <p>
 * Provides the services according to the preference.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface Preference<T extends PreferenceType> extends Serializable {

    /**
     * <p>
     * Provides the organisation id.
     * </p>
     *
     * @return Organisation id.
     */
    Long orgId();

    /**
     * <p>
     * Gets the type of the preference.
     * </p>
     *
     * @return the {@link PreferenceType}
     */
    T getType();
}
