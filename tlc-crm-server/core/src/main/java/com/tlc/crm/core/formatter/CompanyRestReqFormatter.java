package com.tlc.crm.core.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.field.contact.CompanyFields;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.i18n.I18nResolver;

import java.util.Collection;
import java.util.Map;

/**
 * Formatter that converts an incoming REST request JSON for a Company into the
 * internal request JSON structure used by the entity layer.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CompanyRestReqFormatter extends EntityReqJsonFormatter {

    private CompanyRestReqFormatter() {
    }

    private static class Instance {
        private static final CompanyRestReqFormatter INSTANCE = new CompanyRestReqFormatter();
    }

    /**
     * Returns the singleton instance of this formatter.
     *
     * @return the shared {@link CompanyRestReqFormatter} instance
     */
    public static CompanyRestReqFormatter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Organization id for which fields should be resolved.
     * @param requestJson  Incoming REST request JSON (client-facing keys).
     * @param i18nResolver Resolver used to translate a field's internal name
     *                     to the key used in {@code requestJson}.
     * @return A new {@link JsonObject} containing the internal company request
     * representation (field ids and subfields).
     */
    @Override
    public JsonObject format(final long orgId, final JsonObject requestJson, final I18nResolver i18nResolver) {
        final JsonObject companyJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.COMPANY.getName());

        for (final FieldInfo fieldInfo : fields) {
            final String internalName = i18nResolver.get(fieldInfo.getInternalName());

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(orgId, fieldInfo, internalName, companyJson, requestJson, i18nResolver);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case CompanyFields.NAME, CompanyFields.WEBSITE, CompanyFields.FACEBOOK, CompanyFields.TWITTER,
                         CompanyFields.LINKEDIN, CompanyFields.DESCRIPTION -> {
                        if (requestJson.containsKey(internalName)) {
                            companyJson.put(fieldInfo.idAsString(), requestJson.getString(internalName));
                        }
                    }
                    case CompanyFields.ADDRESS -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonObject addressJson = requestJson.getJsonObject(internalName);
                            final JsonObject subFieldJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final Object country = addressJson.opt(StoreSubType.COUNTRY.getApiPropertyName());

                            subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE1).idAsString(), addressJson.opt(StoreSubType.ADDRESS_LINE1.getApiPropertyName()));
                            subFieldJson.put(fieldSubTypes.get(StoreSubType.ADDRESS_LINE2).idAsString(), addressJson.opt(StoreSubType.ADDRESS_LINE2.getApiPropertyName()));
                            subFieldJson.put(fieldSubTypes.get(StoreSubType.CITY).idAsString(), addressJson.opt(StoreSubType.CITY.getApiPropertyName()));
                            subFieldJson.put(fieldSubTypes.get(StoreSubType.STATE).idAsString(), addressJson.opt(StoreSubType.STATE.getApiPropertyName()));
                            subFieldJson.put(fieldSubTypes.get(StoreSubType.ZIPCODE).idAsString(), addressJson.opt(StoreSubType.ZIPCODE.getApiPropertyName()));
                            mapCountryField(country, subFieldJson, fieldSubTypes.get(StoreSubType.COUNTRY).idAsString());

                            companyJson.put(fieldInfo.idAsString(), subFieldJson);
                        }
                    }
                    case CompanyFields.SALES_OWNER, CompanyFields.SIC_CODE, CompanyFields.TERRITORY,
                         CompanyFields.EMPLOYEE_COUNT, CompanyFields.PARENT_COMPANY -> {
                        if (requestJson.containsKey(internalName)) {
                            companyJson.put(fieldInfo.idAsString(), requestJson.getLong(internalName));
                        }
                    }
                    case CompanyFields.ORGANIZATION_STATUS, CompanyFields.INDUSTRY_TYPE,
                         CompanyFields.BUSINESS_TYPE -> {
                        if (requestJson.containsKey(internalName)) {
                            mapOptionField(orgId, requestJson.get(internalName), fieldInfo, companyJson, i18nResolver);
                        }
                    }
                    case CompanyFields.ID -> {
                        if (requestJson.containsKey(internalName)) {
                            companyJson.put(CompanyFields.ID, requestJson.getLong(internalName));
                        }
                    }
                    case CompanyFields.ANNUAL_REVENUE -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonObject jsonObject = requestJson.getJsonObject(internalName);
                            final JsonObject subFieldJson = Json.object();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());

                            subFieldJson.put(fieldSubTypes.get(StoreSubType.VALUE).idAsString(), jsonObject.getLong(StoreSubType.VALUE.getApiPropertyName()));
                            subFieldJson.put(fieldSubTypes.get(StoreSubType.CURRENCY).idAsString(), jsonObject.getLong(StoreSubType.CURRENCY.getApiPropertyName()));

                            companyJson.put(fieldInfo.idAsString(), subFieldJson);
                        }
                    }
                    case CompanyFields.TAGS -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonArray tags = Json.array();

                            requestJson.getJsonArray(internalName).forEach(value -> {
                                final JsonObject jsonObject = Json.object();

                                jsonObject.put(CompanyFields.ID, NumberUtils.parseNumber(value).longValue());
                                tags.put(jsonObject);
                            });

                            companyJson.put(fieldInfo.idAsString(), tags);
                        }
                    }
                }
            }
        }

        return companyJson;
    }
}
