package com.tlc.crm.common.iam.api.preferences.model.type;

import java.io.Serializable;

/**
 * <p>
 * Defines the type of the preference.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PreferenceType extends Serializable {

    /**
     * <p>
     * Gets the type for the preference type
     * </p>
     *
     * @return the type of {@link PreferenceType}
     */
    String type();
}
