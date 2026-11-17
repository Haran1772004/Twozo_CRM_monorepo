package com.tlc.crm.zapier.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;

/**
 * Generic interface for converting a {@code JsonObject} into a specific type {@code T}.
 *
 * @param <T> The type of object the JSON is converted into.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ZapConverter<T> {

    /**
     * Converts a {@code JsonObject} into an instance of type {@code T} based on the provided configuration.
     *
     * @param converterConfig The configuration containing conversion settings.
     * @param jsonObject      The JSON object to be converted.
     * @return The converted object of type {@code T}.
     */
    T convert(final ConverterConfig converterConfig, final JsonObject jsonObject);

}
