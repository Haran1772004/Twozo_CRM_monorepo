package com.tlc.crm.notifications.api.call.model.converter;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.notifications.api.call.model.CallDetails;
import com.tlc.crm.notifications.api.call.model.converter.impl.CallDetailsConverterImpl;

/**
 * Provides conversion methods for transforming call history and call details between
 * JSON format and Java objects.
 *
 * @author Sathish
 * @version 1.0
 */
public interface CallDetailsConverter {

    /**
     * Gets the singleton instance of {@link CallDetailsConverter}.
     *
     * @return The singleton instance of {@link CallDetailsConverterImpl}.
     */
    static CallDetailsConverter getInstance() {
        return CallDetailsConverterImpl.getInstance();
    }

    /**
     * Converts a JSON object into a {@link CallDetails} instance.
     *
     * @param jsonObject The JSON object containing call details data.
     * @return A {@link CallDetails} object populated with the JSON data.
     */
    CallDetails convert(final JsonObject jsonObject);

    /**
     * Converts a {@link CallDetails} instance into a JSON object.
     *
     * @param callDetails The {@link CallDetails} object to be converted.
     * @return A {@link JsonObject} representing the call details.
     */
    JsonObject convert(final CallDetails callDetails);
}