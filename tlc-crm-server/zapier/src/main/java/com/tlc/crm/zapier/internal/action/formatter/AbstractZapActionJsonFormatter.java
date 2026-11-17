package com.tlc.crm.zapier.internal.action.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.models.FieldInfo;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.crm.entity.models.type.Phone;
import com.tlc.crm.entity.service.FieldService;
import com.tlc.crm.entity.currency.BaseCurrencyService;

import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;
import com.tlc.crm.fields.models.StoreType;
import com.tlc.crm.fields.service.FieldTypeService;

import com.tlc.crm.zapier.api.models.ZapAction;
import com.tlc.crm.zapier.internal.service.ZapierFieldService;

import com.tlc.l10n.temporal.TemporalServiceProvider;

import java.util.Map;

/**
 * Abstract base class for formatting data into JSON structures compatible with Zapier actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
abstract class AbstractZapActionJsonFormatter {

    protected final FieldService fieldService;
    protected final FieldTypeService fieldTypeService;
    protected final BaseCurrencyService baseCurrencyService;
    protected final ZapierFieldService zapierFieldService;

    protected AbstractZapActionJsonFormatter() {
        this.fieldService = FieldService.getInstance();
        this.fieldTypeService = FieldTypeService.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.zapierFieldService = ZapierFieldService.getInstance();
    }

    /**
     * Maps a persistent field from the internal entity JSON to the Zapier action JSON format.
     *
     * @param orgId      The ID of the organization, used for field-specific formatting or lookups.
     * @param fieldInfo  Metadata describing the field to be mapped.
     * @param entityJson The source JSON object containing internal entity data.
     * @param actionJson The target JSON object to be populated for the Zapier action.
     */
    protected final void mapPersistentFieldToJson(final long orgId, final FieldInfo fieldInfo,
                                                  final JsonObject entityJson, final JsonObject actionJson) {
        final StoreType storeType = fieldInfo.storeType();
        final String fieldName = fieldInfo.fieldName();
        final String idAsString = fieldInfo.idAsString();

        switch (storeType) {
            case SINGLE_STRING, TEXT -> {
                if (actionJson.containsKey(fieldName)) {
                    entityJson.put(idAsString, actionJson.getString(fieldName));
                }
            }
            case SINGLE_NUMBER, DROPDOWN, OWNER, COMPANY, CONTACT -> {
                if (actionJson.containsKey(fieldName)) {
                    entityJson.put(idAsString, actionJson.getLong(fieldName));
                }
            }
            case EMAIL -> {
                if (actionJson.containsKey(fieldName)) {
                    final JsonObject subFieldJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.EMAIL_TYPE).idAsString(), Email.Type.WORK.getId());
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.EMAIL_VALUE).idAsString(), actionJson.getString(fieldName));

                    entityJson.put(idAsString, subFieldJson);
                }
            }
            case PHONE -> {
                if (actionJson.containsKey(fieldName)) {
                    final JsonObject subFieldJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.PHONE_TYPE).idAsString(), Phone.Type.WORK.getId());
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.PHONE_NUMBER).idAsString(), actionJson.getString(fieldName));

                    entityJson.put(idAsString, subFieldJson);
                }
            }
            case ADDRESS -> {
                if (actionJson.containsKey(fieldName)) {
                    final JsonObject subFieldJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).idAsString(), actionJson.getString(fieldName));
                    entityJson.put(idAsString, subFieldJson);
                }
            }
            case MULTI_SELECT -> {
                if (actionJson.containsKey(fieldName)) {
                    final JsonArray array = Json.array();

                    actionJson.getJsonArray(fieldName).forEach(value -> array.put(NumberUtils.parseNumber(value).longValue()));
                    entityJson.put(fieldInfo.idAsString(), array);
                }
            }
            case MONETARY -> {
                if (actionJson.containsKey(fieldName)) {
                    final JsonObject subFieldJson = Json.object();
                    final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.VALUE).idAsString(), actionJson.getLong(fieldName));
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), baseCurrencyService.getDefaultCurrency(orgId).getId());

                    entityJson.put(idAsString, subFieldJson);
                }
            }
            case DATE, TIME -> {
                if (actionJson.containsKey(fieldName)) {
                    entityJson.put(idAsString, TemporalServiceProvider.get().getISOOffsetTimeStamp(actionJson.getString(fieldName)));
                }
            }
            case DATE_RANGE, TIME_RANGE, DATE_TIME_RANGE -> {
                final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(storeType);
                final String fromKey = String.join(" - ", fieldName, fieldSubTypes.get(StoreSubType.FROM).name());
                final String toKey = String.join(" - ", fieldName, fieldSubTypes.get(StoreSubType.TO).name());

                if (actionJson.containsKey(fromKey) || actionJson.containsKey(toKey)) {
                    final JsonObject subFieldJson = Json.object();

                    subFieldJson.put(fieldSubTypes.get(StoreSubType.FROM).idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(actionJson.getString(fromKey)));
                    subFieldJson.put(fieldSubTypes.get(StoreSubType.TO).idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(actionJson.getString(toKey)));

                    entityJson.put(idAsString, subFieldJson);
                }
            }
        }
    }

    /**
     * Formats the given Zapier action data into a JSON object.
     *
     * @param orgId      The ID of the organization, used to apply organization-specific logic or formatting.
     * @param zapAction  The Zapier action containing data to be formatted.
     * @param actionJson The existing JSON object that will be populated with the formatted action data.
     * @return The formatted JSON object ready for sending as a Zapier action payload.
     */
    protected abstract JsonObject format(final long orgId, final ZapAction zapAction, final JsonObject actionJson);
}
