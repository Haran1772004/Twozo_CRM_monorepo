package com.tlc.crm.common.iam.api.preferences.model.converter;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.api.preferences.model.Preference;
import com.tlc.crm.common.iam.api.preferences.model.holder.PreferenceHolder;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link Preference}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PreferenceConverter<P extends Preference<?>, H extends PreferenceHolder<?, ?>> {

    /**
     * <p>
     * Converts the {@link Preference} into {@link JsonObject}.
     * </p>
     *
     * @param preferenceHolder Represents the details of the preferenceHolder.
     * @return The preferences as {@link JsonObject}.
     */
    JsonObject convertToJson(final P preferenceHolder);

    /**
     * <p>
     * Converts the {@link PreferenceHolder} into {@link JsonObject}.
     * </p>
     *
     * @param preferenceHolder Represents the details of the preferenceHolder.
     * @return The preferences as {@link JsonObject}.
     */
    JsonObject convertToJson(final H preferenceHolder);
}
