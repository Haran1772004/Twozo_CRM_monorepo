package com.tlc.crm.notifications.api.call.model.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notifications.api.call.model.PhoneNumber;
import com.tlc.crm.notifications.api.call.model.converter.impl.PhoneNumberConverterImpl;

/**
 * Interface for converting {@link PhoneNumber} objects to and from {@link JsonObject}.
 * <p>
 * This converter facilitates seamless serialization and deserialization of phone number data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface PhoneNumberConverter {

    /**
     * Retrieves the singleton instance of {@link PhoneNumberConverter}.
     *
     * @return An instance of {@link PhoneNumberConverter}.
     */
    static PhoneNumberConverter getInstance() {
        return PhoneNumberConverterImpl.getInstance();
    }

    /**
     * Converts a {@link JsonObject} into a {@link PhoneNumber} object.
     *
     * @param jsonObject The JSON representation of the phone number.
     * @return The converted {@link PhoneNumber} object.
     */
    PhoneNumber convert(final Long orgId, final JsonObject jsonObject);

    /**
     * Converts a {@link PhoneNumber} object into a {@link JsonObject}.
     *
     * @param phoneNumber The {@link PhoneNumber} object to convert.
     * @return The JSON representation of the phone number.
     */
    JsonObject convert(final PhoneNumber phoneNumber);
}
