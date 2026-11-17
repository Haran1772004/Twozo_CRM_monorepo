package com.tlc.crm.core.api.deal.model.converter.mobile;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.api.deal.model.converter.DealConverter;
import com.tlc.crm.core.field.deal.DealFields;
import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.PresetConfig;
import com.tlc.crm.entity.service.FieldService;

import java.util.Objects;
import java.util.Collection;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Converts {@link Deal} into Json and vice versa.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class DealConverterMobileClientImpl implements DealConverterMobileClient {

    private static class Instance {
        private static final DealConverterMobileClientImpl DEAL_CONVERTER_MOBILE_CLIENT = new DealConverterMobileClientImpl();
    }

    private DealConverterMobileClientImpl() {

    }

    /**
     * <p>
     * Gets the {@link DealConverterMobileClientImpl} instance.
     * </p>
     *
     * @return {@link DealConverterMobileClientImpl}.
     */
    public static DealConverterMobileClientImpl getInstance() {
        return Instance.DEAL_CONVERTER_MOBILE_CLIENT;
    }

    /**
     * <p>
     * Converts the {@link Deal} into {@link JsonObject} for summary response.
     * </p>
     *
     * @param deal            {@link Deal}.
     * @param converterConfig {@link ConverterConfig}.
     * @return {@link JsonObject}
     */
    public JsonObject convertSummary(final Deal deal, final ConverterConfig converterConfig) {
        final DealConverter dealConverter = DealConverter.getInstance();
        final JsonObject json = Json.object();

        json.put(DealFields.ID, deal.id());
        json.put(DealFields.SUMMARY, convertSummaryToJsonArray(dealConverter, deal, converterConfig));
        dealConverter.convertSubFieldToJson(deal, converterConfig, json);

        return json;
    }

    /**
     * <p>
     * Converts the {@link Deal} fields into {@link JsonArray}.
     * </p>
     *
     * @param deal            {@link Deal}.
     * @param converterConfig {@link ConverterConfig}.
     * @return {@link JsonObject}
     */
    private JsonArray convertSummaryToJsonArray(final DealConverter dealConverter, final Deal deal, final ConverterConfig converterConfig) {
        final JsonArray jsonArray = Json.array();
        final FieldService fieldService = FieldService.getSecureInstance();
        final Collection<FieldInfo> sourceFields = fieldService.getFieldsBySource(deal.orgId(), deal.getEntityType());
        final Map<Integer, List<FieldInfo>> fieldData = sourceFields.stream().filter(field -> {
                    final PresetConfig presetConfig = field.presetConfig();

                    return !presetConfig.showOnProfile() && !presetConfig.hidden()
                            && !Objects.equals(DealFields.RELATED_CONTACTS, field.aliasName());
                })
                .sorted(Comparator.comparing(field -> field.presetConfig().orderOnView()))
                .collect(Collectors.groupingBy(field -> field.presetConfig().mobileClientRowIndex()));
        final Collection<FieldInfo> defaultRowOnIndexFields = fieldData.remove(-1);
        final BiConsumer<Collection<FieldInfo>, JsonArray> fieldsAdder = (fields, fieldsArray) -> {

            if (Objects.nonNull(fields)) {
                for (final FieldInfo fieldInfo : fields) {
                    final JsonObject fieldJson = dealConverter.convertFieldInfoToJsonObject(fieldInfo, deal, converterConfig);

                    if (dealConverter.isDependableFieldAllowed(fieldInfo, fieldJson)) {
                        fieldsArray.put(fieldJson);
                    }
                }
            }
        };

        for (final Collection<FieldInfo> fields : fieldData.values()) {
            final JsonArray fieldsArray = Json.array();

            fieldsAdder.accept(fields, fieldsArray);
            jsonArray.put(fieldsArray);
        }
        fieldsAdder.accept(defaultRowOnIndexFields, jsonArray);

        return jsonArray;
    }
}
