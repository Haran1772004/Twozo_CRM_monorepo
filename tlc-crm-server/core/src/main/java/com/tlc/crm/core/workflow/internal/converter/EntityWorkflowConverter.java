package com.tlc.crm.core.workflow.internal.converter;

import com.tlc.validator.TlcModel;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.iam.service.UserService;

import com.tlc.crm.core.api.summaryInfo.model.converter.SummaryInfoConverter;

import com.tlc.crm.entity.builder.EntityBuilder;
import com.tlc.crm.entity.features.api.tag.model.converter.TagConverter;
import com.tlc.crm.entity.models.type.converter.AddressConverter;
import com.tlc.crm.entity.models.type.converter.MonetaryConverter;
import com.tlc.crm.entity.models.type.converter.OptionConverter;
import com.tlc.crm.entity.service.FieldService;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The converter that facilitates different ways to convert model from/to {@link TlcModel}.
 *
 * @param <T> Refers the type of the model.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
abstract class EntityWorkflowConverter<T extends TlcModel> {

    protected final EntityBuilder entityBuilder;
    protected final FieldService fieldService;
    protected final UserService userService;
    protected final TagConverter tagConverter;
    protected final OptionConverter optionConverter;
    protected final AddressConverter addressConverter;
    protected final MonetaryConverter monetaryConverter;
    protected final SummaryInfoConverter summaryInfoConverter;

    protected EntityWorkflowConverter() {
        this.entityBuilder = EntityBuilder.getInstance();
        this.fieldService = FieldService.getInstance();
        this.userService = UserService.getInstance();
        this.tagConverter = TagConverter.getInstance();
        this.optionConverter = OptionConverter.getInstance();
        this.addressConverter = AddressConverter.getInstance();
        this.monetaryConverter = MonetaryConverter.getInstance();
        this.summaryInfoConverter = SummaryInfoConverter.getInstance();
    }

    /**
     * Converts the models into {@link JsonArray}.
     *
     * @param models Refers the models.
     * @return The converted json.
     */
    public final JsonArray convert(final Collection<T> models) {
        final JsonArray jsonArray = Json.array();

        for (final T model : models) {
            jsonArray.put(convert(model));
        }

        return jsonArray;
    }

    /**
     * Converts the json array into collection of {@link TlcModel}.
     *
     * @param jsonArray Refers the json array.
     * @return The collection of {@link TlcModel}.
     */
    public final Collection<T> convert(final JsonArray jsonArray) {
        final Collection<T> models = new ArrayList<>();
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(index);

            models.add(convert(jsonObject));
        }

        return models;
    }

    /**
     * Converts the models into {@link JsonObject}.
     *
     * @param model Refers the model.
     * @return The converted json.
     */
    protected abstract JsonObject convert(final T model);

    /**
     * Converts the json object into {@link TlcModel}.
     *
     * @param jsonObject Refers the json object.
     * @return The {@link TlcModel}.
     */
    protected abstract T convert(final JsonObject jsonObject);

}
