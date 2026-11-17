package com.tlc.crm.core.formatter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.TypeChecker;

import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.core.field.contact.CompanyFields;
import com.tlc.crm.core.field.contact.ContactFields;

import com.tlc.crm.entity.fields.source.Source;
import com.tlc.crm.entity.models.FieldInfo;

import com.tlc.crm.fields.models.FieldSubType;
import com.tlc.crm.fields.models.StoreSubType;

import com.tlc.i18n.I18nResolver;
import com.tlc.l10n.temporal.TemporalServiceProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Formatter that converts an incoming REST request JSON for a Contact into the
 * internal request JSON structure used by the entity layer.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class ContactRestReqFormatter extends EntityReqJsonFormatter {

    private final CompanyRestReqFormatter companyRestReqFormatter;

    private ContactRestReqFormatter() {
        this.companyRestReqFormatter = CompanyRestReqFormatter.getInstance();
    }

    private static class Instance {
        private static final ContactRestReqFormatter INSTANCE = new ContactRestReqFormatter();
    }

    public static ContactRestReqFormatter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId        Represents the organization ID for which the formatting is being done, allowing for organization-specific logic if necessary.
     * @param requestJson  Represents the incoming JSON object from the request that needs to be formatted into the internal entity JSON structure.
     * @param i18nResolver Represents the internationalization resolver that can be used to fetch localized strings or handle locale-specific formatting during the conversion process.
     */
    @Override
    public JsonObject format(final long orgId, final JsonObject requestJson, final I18nResolver i18nResolver) {
        final JsonObject contactJson = Json.object();
        final Collection<FieldInfo> fields = fieldService.getFieldsBySource(orgId, Source.CONTACT.getName());

        for (final FieldInfo fieldInfo : fields) {
            final String internalName = i18nResolver.get(fieldInfo.getInternalName());

            if (fieldInfo.isPersistent()) {
                mapPersistentFieldToJson(orgId, fieldInfo, internalName, contactJson, requestJson, i18nResolver);
            } else {
                final String aliasName = fieldInfo.aliasName();

                switch (aliasName) {
                    case ContactFields.FIRST_NAME, ContactFields.LAST_NAME, ContactFields.FACEBOOK,
                         ContactFields.TWITTER, ContactFields.LINKEDIN, ContactFields.DESIGNATION,
                         ContactFields.DEPARTMENT, ContactFields.OTHER_UNSUBSCRIBE_REASON -> {
                        if (requestJson.containsKey(internalName)) {
                            contactJson.put(fieldInfo.idAsString(), requestJson.getString(internalName));
                        }
                    }
                    case ContactFields.SALES_OWNER, ContactFields.LIFECYCLE_STAGE,
                         ContactFields.LIFECYCLE_STATUS, ContactFields.TERRITORY -> {
                        if (requestJson.containsKey(internalName)) {
                            contactJson.put(fieldInfo.idAsString(), requestJson.getLong(internalName));
                        }
                    }
                    case ContactFields.CONTACT_LOST_REASON, ContactFields.TIMEZONE, ContactFields.SUBSCRIPTION_STATUS,
                         ContactFields.UNSUBSCRIBE_REASON, ContactFields.CONTACT_SOURCE -> {
                        if (requestJson.containsKey(internalName)) {
                            mapOptionField(orgId, requestJson.get(internalName), fieldInfo, contactJson, i18nResolver);
                        }
                    }
                    case ContactFields.COMPANY -> {
                        if (requestJson.containsKey(internalName)) {
                            final Object value = requestJson.get(internalName);

                            if (TypeChecker.isNonDecimalNumber(value)) {
                                contactJson.put(fieldInfo.idAsString(), value);
                            } else {
                                final JsonObject companyJson = companyRestReqFormatter.format(orgId, requestJson.getJsonObject(internalName), i18nResolver);

                                contactJson.put(fieldInfo.idAsString(), companyJson);
                            }
                        }
                    }
                    case ContactFields.ID -> {
                        if (requestJson.containsKey(internalName)) {
                            contactJson.put(ContactFields.ID, requestJson.getLong(internalName));
                        }
                    }
                    case ContactFields.EMAILS -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonArray jsonArray = requestJson.getJsonArray(internalName);
                            final JsonArray array = Json.array();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final String typeKey = fieldSubTypes.get(StoreSubType.EMAIL_TYPE).idAsString();
                            final String valueKey = fieldSubTypes.get(StoreSubType.EMAIL_VALUE).idAsString();

                            final int size = jsonArray.size();

                            for (int i = 0; i < size; i++) {
                                final JsonObject emailJson = jsonArray.optJsonObject(i);

                                if (Objects.nonNull(emailJson)) {
                                    final JsonObject subFieldJson = Json.object();

                                    subFieldJson.put(valueKey, emailJson.opt(StoreSubType.EMAIL_VALUE.getApiPropertyName()));
                                    mapEmailTypeField(emailJson.opt(StoreSubType.EMAIL_TYPE.getApiPropertyName()), subFieldJson, typeKey);
                                    array.put(subFieldJson);
                                }
                            }
                            contactJson.put(fieldInfo.idAsString(), array);
                        }
                    }
                    case ContactFields.PHONES -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonArray jsonArray = requestJson.getJsonArray(internalName);
                            final JsonArray array = Json.array();
                            final Map<StoreSubType, FieldSubType> fieldSubTypes = fieldTypeService.groupSubtypesByStoreSubType(fieldInfo.storeType());
                            final String typeKey = fieldSubTypes.get(StoreSubType.PHONE_TYPE).idAsString();
                            final String numberKey = fieldSubTypes.get(StoreSubType.PHONE_NUMBER).idAsString();

                            final int size = jsonArray.size();

                            for (int i = 0; i < size; i++) {
                                final JsonObject phoneJson = jsonArray.optJsonObject(i);

                                if (Objects.nonNull(phoneJson)) {
                                    final JsonObject subFieldJson = Json.object();

                                    mapPhoneTypeField(phoneJson.opt(StoreSubType.PHONE_TYPE.getApiPropertyName()), subFieldJson, typeKey);
                                    subFieldJson.put(numberKey, phoneJson.opt(StoreSubType.PHONE_NUMBER.getApiPropertyName()));
                                    array.put(subFieldJson);
                                }
                            }
                            contactJson.put(fieldInfo.idAsString(), array);
                        }
                    }
                    case ContactFields.TAGS -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonArray tags = Json.array();

                            requestJson.getJsonArray(internalName).forEach(value -> {
                                final JsonObject jsonObject = Json.object();

                                jsonObject.put(CompanyFields.ID, NumberUtils.parseNumber(value).longValue());
                                tags.put(jsonObject);
                            });

                            contactJson.put(fieldInfo.idAsString(), tags);
                        }
                    }
                    case ContactFields.DATE_OF_BIRTH -> {
                        if (requestJson.containsKey(internalName)) {
                            contactJson.put(fieldInfo.idAsString(), TemporalServiceProvider.get().getISOOffsetTimeStamp(requestJson.getString(internalName)));
                        }
                    }
                    case ContactFields.ADDRESS -> {
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

                            contactJson.put(fieldInfo.idAsString(), subFieldJson);
                        }
                    }
                    case ContactFields.SUBSCRIPTION_TYPES -> {
                        if (requestJson.containsKey(internalName)) {
                            final JsonArray array = Json.array();

                            requestJson.getJsonArray(internalName).forEach(value -> {
                                if (TypeChecker.isNumber(value)) {
                                    array.put(NumberUtils.parseNumber(value).longValue());
                                } else {
                                    getOption(orgId, value, fieldInfo, i18nResolver).ifPresent(option -> array.put(option.id()));
                                }
                            });
                            contactJson.put(fieldInfo.idAsString(), array);
                        }
                    }
                }
            }
        }
        return contactJson;
    }
}
