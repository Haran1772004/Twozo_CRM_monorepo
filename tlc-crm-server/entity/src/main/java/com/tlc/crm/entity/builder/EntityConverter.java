package com.tlc.crm.entity.builder;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Converts an entity from json object and vice versa
 *
 * @author Selvakumar
 * @version 1.0
 */
public abstract class EntityConverter<T> {

    protected final EntityBuilder entityBuilder;
    protected final FieldService fieldService;
    protected final FieldService secureFieldService;
    protected final I18nService i18nService;

    protected EntityConverter() {
        entityBuilder = EntityBuilder.getInstance();
        fieldService = FieldService.getInstance();
        secureFieldService = FieldService.getSecureInstance();
        i18nService = I18nServiceProvider.get();
    }

    public final Collection<T> convert(final ConverterConfig converterConfig, final JsonArray jsonArray) {
        final Collection<T> models = new ArrayList<>();

        for (int index = 0; index < jsonArray.size(); index++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(index);
            final T model = convert(converterConfig, jsonObject);

            models.add(model);
        }

        return models;
    }

    public abstract T convert(ConverterConfig converterConfig, JsonObject jsonObject);

    /**
     * Converts the model into the {@link JsonObject}
     *
     * @param model Represents the entity model
     * @return {@link JsonObject}
     */
    public JsonObject convert(final T model) {
        return Json.object();
    }

    /**
     * Converts the partial data into the {@link JsonObject}
     *
     * @param model Represents the entity model
     * @return {@link JsonObject}
     */
    public JsonObject convertPartialData(final T model) {
        return Json.object();
    }

    /**
     * Converts the model into the {@link JsonObject}
     *
     * @param converterConfig Represents the converter configuration
     * @param model           Represents the entity model
     * @return {@link JsonObject}
     */
    protected JsonObject convert(final ConverterConfig converterConfig, final T model) {
        return Json.object();
    }

    /**
     * <p>
     * Converts a model of type {@code T} into a {@link JsonObject}.
     * </p>
     *
     * @param model           The model of type {@code T} to convert.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return A {@link JsonObject} representation of the model, currently an empty {@link JsonObject}.
     */
    public JsonObject convertModelToJson(final T model, final ConverterConfig converterConfig) {
        return Json.object();
    }

    /**
     * Converts the given model object into its REST JSON representation
     * using the provided converter configuration.
     *
     * <p>This method transforms the internal model into a {@link JsonObject}
     * suitable for REST API responses. The conversion behavior can be
     * customized through the {@link ConverterConfig}.</p>
     *
     * @param model           The model of type {@code T} to convert.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return A {@link JsonObject} representation of the model, currently an empty {@link JsonObject}.
     */
    public JsonObject convertToRestJson(final T model, final ConverterConfig converterConfig) {
        return Json.object();
    }

    /**
     * <p>
     * Converts a partial model of type {@code T} into a generic {@link Object}.
     * </p>
     *
     * @param model           The partial model of type {@code T} to convert.
     * @return An {@link Object} representing the partial model, currently an empty {@link JsonObject}.
     */
    public JsonObject convertPartialModelToJson(final T model, final ConverterConfig converterConfig) {
        return Json.object();
    }
}
