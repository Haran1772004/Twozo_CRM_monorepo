package com.tlc.crm.common.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

/**
 * <p>
 * The {@code OptionJsonConverter} interface provides methods to convert a model object of type {@code T} into a {@code JsonObject}.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
public interface OptionJsonConverter<T> {

    /**
     * Converts the specified model object into a {@code JsonObject}.
     *
     * @param model the model object to be converted
     * @return a {@code JsonObject} representing the converted model
     */
    default JsonObject convertModelToOptionJson(final T model) {
        return Json.object();
    }

    /**
     * Converts the specified model object into a {@code JsonObject} using the provided {@code ConverterConfig}.
     *
     * @param model           the model object to be converted
     * @param converterConfig the configuration to customize the conversion process
     * @return a {@code JsonObject} representing the converted model with applied configurations
     */
    default JsonObject convertModelToOptionJson(final T model, final ConverterConfig converterConfig) {
        return convertModelToOptionJson(model);
    }
}