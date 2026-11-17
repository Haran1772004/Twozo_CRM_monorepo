package com.tlc.crm.common.iam.api.preferences.model.builder;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.iam.api.preferences.model.Preference;

import java.io.Serializable;

/**
 * <p>
 * Manages the building process of the {@link Preference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PreferenceBuilder<P extends Preference<?>> extends Serializable {

    /**
     * <p>
     * Builds the {@link Preference} from the given {@link JsonObject}.
     * </p>
     *
     * @param jsonObject Holds the details of the preference.
     * @return {@link Preference}.
     */
    P build(final JsonObject jsonObject);

    /**
     * <p>
     * Builds the {@link JsonObject} from the given {@link Preference}.
     * </p>
     *
     * @param preference Holds the details of the preference.
     * @return {@link JsonObject}.
     */
    JsonObject build(final P preference);
}
