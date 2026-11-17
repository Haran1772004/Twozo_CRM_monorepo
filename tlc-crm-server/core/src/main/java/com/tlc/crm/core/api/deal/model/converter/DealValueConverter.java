package com.tlc.crm.core.api.deal.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.core.api.deal.model.DealValue;

import com.tlc.crm.entity.models.type.Monetary;
import com.tlc.crm.entity.models.type.converter.MonetaryConverter;

import java.util.Map;

/**
 * Provides different ways to convert the data from / into {@link DealValue}
 *
 * @author Sundar
 * @version 1.0
 */
public class DealValueConverter extends MonetaryConverter {

    private static class Instance {
        private static final DealValueConverter DEAL_VALUE_CONVERTER = new DealValueConverter();
    }

    private DealValueConverter() {
        super();
    }

    /**
     * Gets the {@link DealValueConverter} instance
     *
     * @return {@link DealValueConverter}
     */
    public static DealValueConverter getInstance() {
        return Instance.DEAL_VALUE_CONVERTER;
    }

    /**
     * Converts {@link DealValue} into {@link JsonObject}
     *
     * @param monetary A model that holds the attributes of {@link DealValue}
     * @return {@link JsonObject}.
     */
    @Override
    public JsonObject convert(final Monetary monetary) {
        final JsonObject jsonObject = Json.object();
        final Map<StoreSubType, FieldSubType> groupedSubtypes = fieldTypeService.groupSubtypesByStoreSubType(StoreType.DEAL_VALUE);

        loadMonetaryJson(monetary, jsonObject, groupedSubtypes);

        return jsonObject;
    }

    /**
     * Converts {@link DealValue} into {@link JsonObject}
     *
     * @param monetary        The {@link Monetary} model containing attributes of the deal value.
     * @param converterConfig The configuration used for conversion, providing locale or other settings.
     * @return {@link JsonObject}
     */
    @Override
    public JsonObject convertModelToJson(final Monetary monetary, final ConverterConfig converterConfig) {
        return convertModelToJson(monetary, fieldTypeService.groupSubtypesByStoreSubType(StoreType.DEAL_VALUE), converterConfig);
    }
}